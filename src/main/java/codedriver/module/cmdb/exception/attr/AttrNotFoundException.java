package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrNotFoundException extends ApiRuntimeException {
    public AttrNotFoundException(Long attrId) {
        super("配置项模型属性：" + attrId + " 不存在");
    }
}
