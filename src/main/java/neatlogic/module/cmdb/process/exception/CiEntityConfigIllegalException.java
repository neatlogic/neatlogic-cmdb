package neatlogic.module.cmdb.process.exception;

import neatlogic.framework.exception.core.ApiRuntimeException;

public class CiEntityConfigIllegalException extends ApiRuntimeException {

    public CiEntityConfigIllegalException(String msg) {
        super("nmcpe.cientityconfigillegalexception.cientityconfigillegalexception", msg);
    }
}
