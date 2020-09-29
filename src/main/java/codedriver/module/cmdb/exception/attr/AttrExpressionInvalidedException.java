package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrExpressionInvalidedException extends ApiRuntimeException {
    public AttrExpressionInvalidedException() {
        super("当前属性是表达式类型，表达式不能为空");
    }
}
