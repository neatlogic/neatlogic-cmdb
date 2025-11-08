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
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/delete";
    }

    @Override
    public String getName() {
        return "nmcac.deletecientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "nmcac.batchdeletecientityapi.input.param.desc.needcommit"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.memo", xss = true)})
    @Output({@Param(name = "transactionId", type = ApiParamType.LONG, desc = "term.cmdb.transactionid")})
    @Description(desc = "nmcac.deletecientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        String description = jsonObj.getString("description");
        CiEntityVo oldCiEntityVo = ciEntityService.getCiEntityBaseInfoById(id);
        if (oldCiEntityVo == null) {
            throw new CiEntityNotFoundException(id);
        }
        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        if (!CiAuthChecker.chain().checkCiEntityDeletePrivilege(oldCiEntityVo.getCiId()).checkCiEntityIsInGroup(id, GroupType.MAINTAIN).check()) {
            throw new CiEntityAuthException(TransactionActionType.DELETE.getText());
        }
        if (needCommit) {
            needCommit = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(oldCiEntityVo.getCiId()).checkCiEntityIsInGroup(id, GroupType.MAINTAIN).check();
        }
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(id);
        ciEntityVo.setDescription(description);
        return ciEntityService.deleteCiEntity(ciEntityVo, needCommit);
    }

}
