package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiTypeIsExistsException extends ApiRuntimeException {
    public CiTypeIsExistsException(String name) {
        super("模型类型：" + name + "已存在");
    }
}
