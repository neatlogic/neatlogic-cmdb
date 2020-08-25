package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiAuthInvalidException extends ApiRuntimeException {
    public CiAuthInvalidException() {
        super("授权信息不完整");
    }
}
