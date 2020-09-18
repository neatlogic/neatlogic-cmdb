package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiHasRelException extends ApiRuntimeException {
    public CiHasRelException(String msg) {
        super("当前模型已经被模型：" + msg + " 引用，请先删除引用关系");
    }
}
