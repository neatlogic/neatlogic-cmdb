package codedriver.module.cmdb.exception.rel;

import codedriver.framework.exception.core.ApiRuntimeException;

public class RelNotFoundException extends ApiRuntimeException {
    public RelNotFoundException(Long relId) {
        super("配置项模型关系：" + relId + " 不存在");
    }
}
