package codedriver.module.cmdb.exception.transaction;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiEntityTransactionNotFoundException extends ApiRuntimeException {
    public CiEntityTransactionNotFoundException(Long transactionId) {
        super("事务：" + transactionId + "没有修改任何配置项");
    }
}
