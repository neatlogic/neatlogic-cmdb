package codedriver.module.cmdb.exception.ci;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class CiNotFoundException extends ApiRuntimeException {
    public CiNotFoundException(Long ciId) {
        super("配置项模型：" + ciId + "不存在");
    }
}
