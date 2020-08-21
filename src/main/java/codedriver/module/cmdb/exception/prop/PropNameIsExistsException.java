package codedriver.module.cmdb.exception.prop;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class PropNameIsExistsException extends ApiRuntimeException {
	public PropNameIsExistsException(String msg) {
		super("属性定义：" + msg + " 已被引用");
	}

}
