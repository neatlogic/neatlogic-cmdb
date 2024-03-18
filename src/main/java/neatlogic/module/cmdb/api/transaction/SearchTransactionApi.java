/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.transaction;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.transaction.TransactionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchTransactionApi extends PrivateApiComponentBase {

    @Resource
    private TransactionService transactionService;

    @Override
    public String getToken() {
        return "/cmdb/transaction/search";
    }

    @Override
    public String getName() {
        return "nmcat.searchtransactionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "term.cmdb.cientityid"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "commited,uncommit,recover,expired", desc = "common.status"),
            @Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "term.cmdb.transactiongroupid"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needaction")})
    @Output({@Param(name = "tbodyList", explode = TransactionVo[].class), @Param(explode = BasePageVo.class)})
    @Description(desc = "nmcat.searchtransactionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        TransactionVo transactionVo = JSONObject.toJavaObject(jsonObj, TransactionVo.class);
        List<TransactionVo> transactionList = transactionService.searchTransaction(transactionVo);
        boolean needAction = jsonObj.getBooleanValue("needAction");
        if (needAction) {
            boolean canTransaction = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(transactionVo.getCiId()).check();
            boolean canRecover = CiAuthChecker.chain().checkCiEntityRecoverPrivilege(transactionVo.getCiId()).check();
            for (TransactionVo t : transactionList) {
                Map<String, Boolean> actionData = new HashMap<>();
                if (!canTransaction || !canRecover) {
                    boolean isInGroup = CiAuthChecker.chain().checkCiEntityIsInGroup(t.getCiEntityId(), GroupType.MAINTAIN).check();
                    if (isInGroup) {
                        canTransaction = true;
                        canRecover = true;
                    }
                }
                actionData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), canTransaction);
                actionData.put(CiAuthType.CIENTITYRECOVER.getValue(), canRecover);
                t.setAuthData(actionData);
            }
        }
        return TableResultUtil.getResult(transactionList, transactionVo);
    }

}
