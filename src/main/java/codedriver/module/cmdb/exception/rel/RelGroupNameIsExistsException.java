package codedriver.module.cmdb.exception.rel;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class RelGroupNameIsExistsException extends ApiRuntimeException {
    public RelGroupNameIsExistsException(String name) {
        super("模型关系分组：" + name + " 已存在");
    }
}
