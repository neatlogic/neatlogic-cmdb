package neatlogic.module.cmdb.process.exception;

import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.util.$;

public class DataConversionAppendPathException extends ApiRuntimeException {

    public DataConversionAppendPathException(Exception e, String configurationPath, String actualPath) {
        super($.t("配置路径：{0}，实际路径：{1}，异常：{2}", configurationPath, actualPath, e.getMessage()), e);
    }
}
