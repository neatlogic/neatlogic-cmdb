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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ValidateCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/validate";
    }

    @Override
    public String getName() {
        return "nmcac.validatecientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "nmcac.validatecientityapi.input.param.desc.id"),
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "nmcac.validatecientityapi.input.param.desc.uuid"),
            @Param(name = "attrEntityData", type = ApiParamType.JSONOBJECT, desc = "nmcac.validatecientityapi.input.param.desc.attr"),
            @Param(name = "relEntityData", type = ApiParamType.JSONOBJECT, desc = "nmcac.validatecientityapi.input.param.desc.rel")})
    @Output({@Param(name = "hasChange", type = ApiParamType.BOOLEAN, desc = "nmcac.validatecientityapi.output.param.desc.needchange")})
    @Description(desc = "nmcac.validatecientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long id = jsonObj.getLong("id");
        String uuid = jsonObj.getString("uuid");
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        ciEntityTransactionVo.setCiEntityId(id);
        ciEntityTransactionVo.setCiEntityUuid(uuid);
        ciEntityTransactionVo.setCiId(ciId);
        // 解析属性数据
        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
        ciEntityTransactionVo.setAttrEntityData(attrObj);
        //解析全局属性数据
        JSONObject globalAttrObj = jsonObj.getJSONObject("globalAttrEntityData");
        ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrObj);
        // 解析关系数据
        JSONObject relObj = jsonObj.getJSONObject("relEntityData");
        ciEntityTransactionVo.setRelEntityData(relObj);

        if (id == null) {
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        } else {
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        }
        boolean hasChange = ciEntityService.validateCiEntityTransaction(ciEntityTransactionVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("hasChange", hasChange);
        returnObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
        return returnObj;
    }

}
