package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class RelEntityNotFoundException extends ApiRuntimeException {
    public RelEntityNotFoundException(String label) {
        super("关系：" + label + " 不能为空");
    }

}
