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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.exception.ci.CiTypeIsExistsException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiTypeApi extends PrivateApiComponentBase {

    @Autowired
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/citype/save";
    }

    @Override
    public String getName() {
        return "nmcac.savecitypeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "nmcac.savecitypeapi.input.param.desc.id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "common.name"),
            @Param(name = "isMenu", type = ApiParamType.INTEGER, desc = "nmcac.savecitypeapi.input.param.desc.ismenu")})
    @Description(desc = "nmcac.savecitypeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTypeVo ciTypeVo = JSONObject.toJavaObject(jsonObj, CiTypeVo.class);
        Long id = jsonObj.getLong("id");
        if (ciTypeMapper.checkCiTypeNameIsExists(ciTypeVo) > 0) {
            throw new CiTypeIsExistsException(ciTypeVo.getName());
        }
        if (id == null) {
            Integer maxsort = ciTypeMapper.getMaxSort();
            if (maxsort == null) {
                maxsort = 1;
            } else {
                maxsort += 1;
            }
            ciTypeVo.setSort(maxsort);
            ciTypeMapper.insertCiType(ciTypeVo);
            return ciTypeVo.getId();
        } else {
            ciTypeMapper.updateCiType(ciTypeVo);
        }
        return null;
    }
}
