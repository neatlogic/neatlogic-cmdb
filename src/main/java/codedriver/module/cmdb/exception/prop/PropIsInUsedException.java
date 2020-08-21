package codedriver.module.cmdb.exception.prop;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class PropIsInUsedException extends ApiRuntimeException {
    public PropIsInUsedException(String msg) {
        super("属性定义：" + msg + " 已存在");
    }

}
