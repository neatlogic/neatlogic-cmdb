package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiTypeNameIsBlankException extends ApiRuntimeException {
    public CiTypeNameIsBlankException() {
        super("模型类型名称不能为空");
    }
}
