package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrNameRepeatException extends ApiRuntimeException {
    public AttrNameRepeatException(String name) {
        super("配置项模型属性：" + name + "已存在");
    }
}
