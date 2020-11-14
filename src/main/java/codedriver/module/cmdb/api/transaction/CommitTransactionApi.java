package codedriver.module.cmdb.api.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.transaction.TransactionVo;
import codedriver.module.cmdb.exception.transaction.TransactionAuthException;
import codedriver.module.cmdb.exception.transaction.TransactionNotFoundException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
@Transactional
public class CommitTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public String getToken() {
        return "/cmdb/transaction/commit";
    }

    @Override
    public String getName() {
        return "提交事务";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "transactionId", type = ApiParamType.LONG, isRequired = true, desc = "事务id")})
    @Output({@Param(type = ApiParamType.LONG, desc = "配置项id")})
    @Description(desc = "提交事务接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("transactionId");
        TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
        if (transactionVo == null) {
            throw new TransactionNotFoundException(transactionId);
        }
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY");
        if (!hasAuth) {
            // 拥有模型管理权限允许添加或修改配置项
            hasAuth = CiAuthChecker.hasTransactionPrivilege(transactionVo.getCiId());
        }

        if (!hasAuth) {
            throw new TransactionAuthException();
        }
        return ciEntityService.commitTransaction(transactionId);
    }

}
