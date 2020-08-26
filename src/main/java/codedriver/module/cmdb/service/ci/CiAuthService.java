package codedriver.module.cmdb.service.ci;

import codedriver.framework.cmdb.constvalue.CiAuthType;

public interface CiAuthService {
    public boolean hasCiPrivilege(Long ciId);

    public boolean hasCiEntityPrivilege(Long ciId, Long ciEntityId, CiAuthType auth);

    public boolean hasTransactionPrivilege(Long ciId, Long ciEntityId, Long transactionId);

    public boolean hasPasswordPrivilege(Long ciEntityId);
}
