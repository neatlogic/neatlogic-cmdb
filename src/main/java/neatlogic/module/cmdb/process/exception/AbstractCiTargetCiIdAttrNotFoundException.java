package neatlogic.module.cmdb.process.exception;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.exception.core.ApiRuntimeException;

public class AbstractCiTargetCiIdAttrNotFoundException extends ApiRuntimeException {

    public AbstractCiTargetCiIdAttrNotFoundException(CiVo ciVo) {
        super("nmcpe.abstractcitargetciidattrnotfoundexception.abstractcitargetciidattrnotfoundexception", ciVo.getLabel(), ciVo.getName());
    }

    public AbstractCiTargetCiIdAttrNotFoundException(CiVo ciVo, String configurationPath, String actualPath) {
        super("抽象模型“{0}({1})”的“targetCiId(写入模型)”字段的值为空，配置路径：{2}，实际路径：{3}", ciVo.getLabel(), ciVo.getName(), configurationPath, actualPath);
    }
}
