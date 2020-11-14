package codedriver.module.cmdb.exception.transaction;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class TransactionAuthException extends ApiRuntimeException {
    public TransactionAuthException() {
        super("您没有权限处理当前事务");
    }
}
