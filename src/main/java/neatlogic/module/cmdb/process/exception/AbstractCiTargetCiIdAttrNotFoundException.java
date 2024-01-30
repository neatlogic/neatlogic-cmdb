package neatlogic.module.cmdb.process.exception;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.exception.core.ApiRuntimeException;

public class AbstractCiTargetCiIdAttrNotFoundException extends ApiRuntimeException {

    public AbstractCiTargetCiIdAttrNotFoundException(CiVo ciVo) {
        super("nmcpe.abstractcitargetciidattrnotfoundexception.abstractcitargetciidattrnotfoundexception", ciVo.getLabel(), ciVo.getName());
    }

    public AbstractCiTargetCiIdAttrNotFoundException(CiVo ciVo, String configurationPath, String actualPath) {
        super("nmcpe.abstractcitargetciidattrnotfoundexception.abstractcitargetciidattrnotfoundexception_b", ciVo.getLabel(), ciVo.getName(), configurationPath, actualPath);
    }
}
