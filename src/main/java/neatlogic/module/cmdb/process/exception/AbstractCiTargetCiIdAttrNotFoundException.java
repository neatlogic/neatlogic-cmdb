package neatlogic.module.cmdb.process.exception;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.exception.core.ApiRuntimeException;

public class AbstractCiTargetCiIdAttrNotFoundException extends ApiRuntimeException {

    public AbstractCiTargetCiIdAttrNotFoundException(CiVo ciVo) {
        super("nmcpe.abstractcitargetciidattrnotfoundexception.abstractcitargetciidattrnotfoundexception", ciVo.getLabel(), ciVo.getName());
    }
}
