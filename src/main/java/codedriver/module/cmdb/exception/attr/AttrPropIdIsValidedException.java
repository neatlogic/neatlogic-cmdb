package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrPropIdIsValidedException extends ApiRuntimeException {
    public AttrPropIdIsValidedException() {
        super("当前属性是属性定义类型，请选择有效的属性定义");
    }
}
