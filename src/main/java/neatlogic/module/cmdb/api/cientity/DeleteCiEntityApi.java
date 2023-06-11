/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteCiEntityApi extends PrivateApiComponentBase {

    @Autowired
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
        TransactionVo t = new TransactionVo();
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(id);
        ciEntityVo.setDescription(description);
        ciEntityService.deleteCiEntity(ciEntityVo, needCommit);
        return null;
    }

}
