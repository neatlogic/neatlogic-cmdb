/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
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
