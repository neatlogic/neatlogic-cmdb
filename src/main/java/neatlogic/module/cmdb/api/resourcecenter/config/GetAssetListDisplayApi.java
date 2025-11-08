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
import neatlogic.framework.cmdb.dto.resourcecenter.AssetListDisplayVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class GetAssetListDisplayApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getName() {
        return "获取资产清单显示设置";
    }

    @Input({})
    @Output({})
    @Description(desc = "获取资产清单显示设置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AssetListDisplayVo assetListDisplayVo = resourceEntityMapper.getAssetListDisplay();
        return assetListDisplayVo;
    }

    @Override
    public String getToken() {
        return "resourcecenter/assetlist/display/get";
    }
}
