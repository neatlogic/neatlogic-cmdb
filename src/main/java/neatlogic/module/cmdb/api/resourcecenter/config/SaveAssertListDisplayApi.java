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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AssetListDisplayVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class SaveAssertListDisplayApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "保存资产清单显示设置";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "rootCiName", type = ApiParamType.STRING, isRequired = true, desc = "根模型名称"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")
    })
    @Output({})
    @Description(desc = "保存资产清单显示设置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        String rootCiName = paramObj.getString("rootCiName");
        JSONObject config = paramObj.getJSONObject("config");
        AssetListDisplayVo assetListDisplayVo = new AssetListDisplayVo();
        if (id != null) {
            assetListDisplayVo.setId(id);
        } else {
            AssetListDisplayVo assetListDisplay = resourceEntityMapper.getAssetListDisplay();
            if (assetListDisplay != null) {
                assetListDisplayVo.setId(assetListDisplay.getId());
            } else {
                assetListDisplayVo.setId(SnowflakeUtil.uniqueLong());
            }
        }
        assetListDisplayVo.setRootCiName(rootCiName);
        assetListDisplayVo.setConfig(config);
        CiVo ciVo = ciMapper.getCiByName(rootCiName);
        if (ciVo == null) {
            throw new CiNotFoundException(rootCiName);
        }
        List<Long> ciIdList = resourceEntityMapper.getAllResourceTypeCiIdList();
        if (!ciIdList.contains(ciVo.getId())) {
            if (CollectionUtils.isNotEmpty(ciIdList)) {
                resourceEntityMapper.deleteResourceTypeCi();
            }
            resourceEntityMapper.insertResourceTypeCi(ciVo.getId());
        }
        resourceEntityMapper.insertAssetListDisplay(assetListDisplayVo);
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/assetlist/display/save";
    }
}
