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

package neatlogic.module.cmdb.api.transaction;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

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
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "common.isneedpage"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "commited,uncommit,recover,expired", desc = "common.status"),
            @Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "term.cmdb.transactiongroupid"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needaction")})
    @Output({@Param(name = "tbodyList", explode = TransactionVo[].class), @Param(explode = BasePageVo.class)})
    @Description(desc = "nmcat.searchtransactionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        TransactionVo transactionVo = JSONObject.toJavaObject(jsonObj, TransactionVo.class);
        List<TransactionVo> transactionList = transactionMapper.searchTransaction(transactionVo);
        JSONObject returnObj = new JSONObject();
        boolean needAction = jsonObj.getBooleanValue("needAction");
        if (transactionVo.getNeedPage() && CollectionUtils.isNotEmpty(transactionList)) {
            int rowNum = transactionMapper.searchTransactionCount(transactionVo);
            returnObj.put("rowNum", rowNum);
            returnObj.put("currentPage", transactionVo.getCurrentPage());
            returnObj.put("pageSize", transactionVo.getPageSize());
            returnObj.put("pageCount", PageUtil.getPageCount(rowNum, transactionVo.getPageSize()));
        }
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
        returnObj.put("tbodyList", transactionList);
        return returnObj;
    }

}
