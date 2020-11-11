package codedriver.module.cmdb.exception.cientity;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiEntityNotFoundException extends ApiRuntimeException {
    public CiEntityNotFoundException(Long ciEntityId) {
        super("配置项：" + ciEntityId + " 不存在");
    }

}
