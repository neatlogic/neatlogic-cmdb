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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
        resourceEntityMapper.insertAssetListDisplay(assetListDisplayVo);
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/assetlist/display/save";
    }
}
