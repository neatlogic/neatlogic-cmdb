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

package neatlogic.module.cmdb.api.citype;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.exception.ci.CiTypeIsExistsException;
import neatlogic.framework.cmdb.exception.ci.CiTypeNameIsBlankException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAllCiTypeApi extends PrivateApiComponentBase {

    @Autowired
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/citype/saveall";
    }

    @Override
    public String getName() {
        return "nmcac.saveallcitypeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciTypeList", isRequired = true, explode = CiTypeVo[].class, type = ApiParamType.JSONARRAY,
            desc = "nmcac.saveallcitypeapi.input.param.desc.citypelist")})
    @Description(desc = "nmcac.saveallcitypeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciTypeList = jsonObj.getJSONArray("ciTypeList");
        int sort = 1;
        for (int i = 0; i < ciTypeList.size(); i++) {
            JSONObject ciTypeObj = ciTypeList.getJSONObject(i);
            CiTypeVo ciTypeVo = JSONObject.toJavaObject(ciTypeObj, CiTypeVo.class);
            if (ciTypeObj.getBooleanValue("isDeleted")) {
                ciTypeMapper.deleteCiTypeById(ciTypeVo.getId());
            } else {
                if (StringUtils.isBlank(ciTypeVo.getName())) {
                    throw new CiTypeNameIsBlankException();
                }
                if (ciTypeMapper.checkCiTypeNameIsExists(ciTypeVo) > 0) {
                    throw new CiTypeIsExistsException(ciTypeVo.getName());
                }
                ciTypeVo.setSort(sort);
                ciTypeMapper.updateCiType(ciTypeVo);
                sort += 1;
            }
        }
        return null;
    }
}
