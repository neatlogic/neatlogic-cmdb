package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

public class AttrIsInvokedByExpressionException extends ApiRuntimeException {
    public AttrIsInvokedByExpressionException() {
        super("当前属性已被其他表达式类型属性引用，请先删除");
    }
}
