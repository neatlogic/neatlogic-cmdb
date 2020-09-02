package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiEntityIsLockedException extends ApiRuntimeException {
    public CiEntityIsLockedException(Long id) {
        super("配置项：" + id + " 已被锁定编辑");
    }

}
