package codedriver.module.cmdb.service.ci;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.constvalue.CiAuthType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiAuthMapper;
import codedriver.module.cmdb.dto.ci.CiAuthVo;

@Service
public class CiAuthServiceImpl implements CiAuthService {
    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private CiAuthMapper ciAuthMapper;

    /**
     * 判断模型管理权限
     */
    @Override
    public boolean hasCiPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE);
    }

    private boolean hasCiPrivilege(Long ciId, CiAuthType... auths) {
        if (ciId != null) {
            String userUuid = UserContext.get().getUserUuid(true);
            List<String> teamUuidList = null;
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<CiAuthVo> authList = ciAuthMapper.getCiAuthByCiId(ciId);
            for (CiAuthVo ciAuthVo : authList) {
                for (CiAuthType auth : auths) {
                    if (ciAuthVo.getAction().equals(auth.getValue())) {
                        switch (ciAuthVo.getAuthType()) {
                            case "user":
                                if (userUuid.equals(ciAuthVo.getAuthUuid())) {
                                    return true;
                                }
                                break;
                            case "team":
                                if (teamUuidList == null) {
                                    teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
                                }
                                if (teamUuidList.contains(ciAuthVo.getAuthUuid())) {
                                    return true;
                                }
                                break;
                            case "role":
                                if (roleUuidList.contains(ciAuthVo.getAuthUuid())) {
                                    return true;
                                }
                                break;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    /**
     * 拥有模型权限的用户也拥有配置项的所有权限
     */
    public boolean hasCiEntityPrivilege(Long ciId, Long ciEntityId, CiAuthType auth) {
        boolean hasAuth = false;
        hasAuth = hasCiPrivilege(ciId, CiAuthType.CIMANAGE, auth);
        if (!hasAuth) {
            // 判断维护圈和消费圈权限
            if (auth.equals(CiAuthType.CIENTITYQUERY)) {

            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean hasTransactionPrivilege(Long ciId, Long ciEntityId, Long transactionId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasPasswordPrivilege(Long ciEntityId) {
        // TODO Auto-generated method stub
        return false;
    }

}
