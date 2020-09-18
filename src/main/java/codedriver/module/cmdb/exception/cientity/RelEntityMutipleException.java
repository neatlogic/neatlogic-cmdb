package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class RelEntityMutipleException extends ApiRuntimeException {
    public RelEntityMutipleException(String label) {
        super("关系：" + label + " 不能存在多个");
    }

}
