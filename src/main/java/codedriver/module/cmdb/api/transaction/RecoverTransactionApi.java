/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.transaction;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.cmdb.enums.TransactionStatus;
import codedriver.framework.cmdb.exception.transaction.TransactionAuthException;
import codedriver.framework.cmdb.exception.transaction.TransactionStatusIrregularException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class RecoverTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Resource
    private CiEntityService ciEntityService;


    @Override
    public String getToken() {
        return "/cmdb/transaction/recover";
    }

    @Override
    public String getName() {
        return "恢复事务";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", isRequired = true, type = ApiParamType.LONG, desc = "事务id")})
    @Description(desc = "恢复事务接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("id");
        TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
        if (transactionVo.getStatus().equals(TransactionStatus.UNCOMMIT.getValue())) {
            throw new TransactionStatusIrregularException(TransactionStatus.UNCOMMIT);
        } else if (transactionVo.getStatus().equals(TransactionStatus.RECOVER.getValue())) {
            throw new TransactionStatusIrregularException(TransactionStatus.RECOVER);
        }
        if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(transactionVo.getCiId()).checkIsInGroup(transactionVo.getCiEntityId(), GroupType.MAINTAIN).check()) {
            throw new TransactionAuthException();
        }
        try {
            ciEntityService.recoverCiEntity(transactionVo);
        } catch (Exception ex) {
            if (ex instanceof ApiRuntimeException) {
                transactionVo.setError(((ApiRuntimeException) ex).getMessage(true));
            } else {
                transactionVo.setError(ex.getMessage());
            }
            transactionMapper.updateTransactionStatus(transactionVo);
            throw ex;
        }
        return null;
    }

}