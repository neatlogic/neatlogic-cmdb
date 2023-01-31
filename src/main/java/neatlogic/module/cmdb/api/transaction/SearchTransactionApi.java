/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
        return "查询事务";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"), @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "配置项id"), @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小"), @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"), @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页"), @Param(name = "status", type = ApiParamType.ENUM, rule = "commited,uncommit,recover,expired", desc = "状态"), @Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "事务组id"), @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要则根据用户权限返回操作列")})
    @Output({@Param(name = "tbodyList", explode = TransactionVo[].class), @Param(explode = BasePageVo.class)})
    @Description(desc = "查询事务接口")
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
