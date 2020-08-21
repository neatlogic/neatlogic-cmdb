package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiNameIsExistsException extends ApiRuntimeException {
    public CiNameIsExistsException(String name) {
        super("模型唯一标识：" + name + " 已存在");
    }
}
