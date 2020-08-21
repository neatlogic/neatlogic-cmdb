package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class RelIsExistsException extends ApiRuntimeException {
    public RelIsExistsException(String fromName, String toName) {
        super("关系：" + fromName + " -> " + toName + "已存在");
    }
}
