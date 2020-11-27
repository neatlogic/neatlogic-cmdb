package codedriver.module.cmdb.exception.reltype;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class RelTypeIsInUsedException extends ApiRuntimeException {
    public RelTypeIsInUsedException() {
        super("当前关系类型正在使用中");
    }
}
