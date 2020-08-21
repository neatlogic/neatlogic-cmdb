package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiLabelIsExistsException extends ApiRuntimeException {
    public CiLabelIsExistsException(String label) {
        super("模型名称：" + label + " 已存在");
    }
}
