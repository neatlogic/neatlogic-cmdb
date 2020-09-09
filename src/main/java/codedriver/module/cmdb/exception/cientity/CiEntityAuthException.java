package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiEntityAuthException extends ApiRuntimeException {
    public CiEntityAuthException(String action) {
        super("您没有权限" + action + "配置项");
    }
}
