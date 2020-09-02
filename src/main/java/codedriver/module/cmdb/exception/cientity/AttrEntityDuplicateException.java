package codedriver.module.cmdb.exception.cientity;

import java.util.List;
import java.util.stream.Collectors;

import codedriver.framework.exception.core.ApiRuntimeException;

@SuppressWarnings("serial")
public class AttrEntityDuplicateException extends ApiRuntimeException {
    public AttrEntityDuplicateException(String label, List<String> valueList) {
        super("属性 " + label + " 值等于 " + valueList.stream().collect(Collectors.joining(",")) + " 的配置项已存在");
    }

}
