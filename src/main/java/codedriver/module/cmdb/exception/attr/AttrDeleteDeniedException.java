package codedriver.module.cmdb.exception.attr;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrDeleteDeniedException extends ApiRuntimeException {
    public AttrDeleteDeniedException() {
        super("内部属性不允许删除");
    }
}
