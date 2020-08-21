package codedriver.module.cmdb.exception.prop;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class PropNotFoundException extends ApiRuntimeException {
    public PropNotFoundException(Long propId) {
        super("属性定义：" + propId + " 不存在");
    }

}
