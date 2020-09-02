package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class NoChangeException extends ApiRuntimeException {
    public NoChangeException() {
        super("配置项没有任何修改");
    }

}
