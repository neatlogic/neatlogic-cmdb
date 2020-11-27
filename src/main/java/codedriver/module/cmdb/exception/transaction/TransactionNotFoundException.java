package codedriver.module.cmdb.exception.transaction;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class TransactionNotFoundException extends ApiRuntimeException {
    public TransactionNotFoundException(Long transactionId) {
        super("事务：" + transactionId + "不存在");
    }
}
