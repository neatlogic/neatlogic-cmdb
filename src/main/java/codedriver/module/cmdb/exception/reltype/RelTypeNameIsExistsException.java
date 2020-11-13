package codedriver.module.cmdb.exception.reltype;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class RelTypeNameIsExistsException extends ApiRuntimeException {
    public RelTypeNameIsExistsException(String name) {
        super("关系类型：" + name + " 已存在");
    }
}
