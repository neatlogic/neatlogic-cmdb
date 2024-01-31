package neatlogic.module.cmdb.process.exception;

import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.util.$;

public class DataConversionAppendPathException extends ApiRuntimeException {

    public DataConversionAppendPathException(Exception e, String configurationPath, String actualPath) {
        super($.t("nmcpe.dataconversionappendpathexception.dataconversionappendpathexception", configurationPath, actualPath, e.getMessage()), e);
    }
}
