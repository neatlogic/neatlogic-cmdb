/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.resourcecenter.config;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.framework.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteResourceEntityApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private SchemaMapper schemaMapper;

    @Override
    public String getName() {
        return "删除资源视图配置信息";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name")
    })
    @Description(desc = "删除资源视图配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        ResourceEntityVo oldResourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
        if (oldResourceEntityVo != null) {
            ResourceEntityConfigVo config = oldResourceEntityVo.getConfig();
            if (config != null) {
                if (StringUtils.isNotBlank(config.getSceneTemplateName())) {
                    SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(config.getSceneTemplateName());
                    if (sceneEntityVo != null && Objects.equals(sceneEntityVo.getIsMultiple(), true)) {
                        resourceEntityMapper.deleteResourceEntityByName(name);
                        String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), name);
                        if (!Objects.equals(tableType, "BASE TABLE")) {
                            schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + name);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/delete";
    }
}
