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
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.ApplicationListDisplayVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class SaveApplicationListDisplayApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getName() {
        return "保存应用清单显示设置";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")
    })
    @Output({})
    @Description(desc = "保存应用清单显示设置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        JSONObject config = paramObj.getJSONObject("config");
        ApplicationListDisplayVo applicationListDisplayVo = new ApplicationListDisplayVo();
        if (id != null) {
            applicationListDisplayVo.setId(id);
        } else {
            ApplicationListDisplayVo applicationListDisplay = resourceEntityMapper.getApplicationListDisplay();
            if (applicationListDisplay != null) {
                applicationListDisplayVo.setId(applicationListDisplay.getId());
            } else {
                applicationListDisplayVo.setId(SnowflakeUtil.uniqueLong());
            }
        }
        applicationListDisplayVo.setConfig(config);
        resourceEntityMapper.insertApplicationListDisplay(applicationListDisplayVo);
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/applicationlist/display/save";
    }
}
