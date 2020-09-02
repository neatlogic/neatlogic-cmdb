package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrEntityNotFoundException extends ApiRuntimeException {
    public AttrEntityNotFoundException(String msg) {
        super("属性：" + msg + " 值不能为空");
    }

}
