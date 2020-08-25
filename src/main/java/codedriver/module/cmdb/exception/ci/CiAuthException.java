package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiAuthException extends ApiRuntimeException {
    public CiAuthException() {
        super("您没有权限修改配置项模型");
    }
}
