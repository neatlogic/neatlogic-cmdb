package codedriver.module.cmdb.service.ci;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.constvalue.CiAuthType;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiAuthMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import codedriver.module.cmdb.dto.ci.CiAuthVo;

@Service
public class CiAuthServiceImpl implements CiAuthService {
    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private CiAuthMapper ciAuthMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    /**
     * 判断模型管理权限
     */
    @Override
    public boolean hasCiManagePrivilege(Long ciId) {
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
    public boolean hasPasswordPrivilege(Long ciEntityId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasCiEntityQueryPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE, CiAuthType.CIENTITYQUERY,
            CiAuthType.CIENTITYQUERY);
    }

    @Override
    public boolean hasCiEntityQueryPrivilege(Long ciId, Long ciEntityId) {
        if (hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE, CiAuthType.CIENTITYQUERY,
            CiAuthType.CIENTITYQUERY)) {
            return true;
        } else {
            // FIXME 补充维护群组判断
        }
        return false;
    }

    @Override
    public boolean hasCiEntityInsertPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYINSERT);
    }

    @Override
    public boolean hasCiEntityInsertPrivilege(Long ciId, Long ciEntityId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasCiEntityUpdatePrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE);
    }

    @Override
    public boolean hasCiEntityDeletePrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE);
    }

    @Override
    public boolean hasTransactionPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.TRANSACTIONMANAGE);
    }

    @Override
    public boolean isInGroup(Long ciEntityId, GroupType... groupType) {

        return false;
    }

    @Override
    public List<Long> isInGroup(List<Long> ciEntityIdList, GroupType... groupType) {
        String userUuid = UserContext.get().getUserUuid(true);
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        List<Long> groupIdList = groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
        List<String> groupTypeList = new ArrayList<>();
        for (GroupType g : groupType) {
            groupTypeList.add(g.getValue());
        }
        return ciEntityMapper.getCiEntityIdByGroupIdList(groupIdList, ciEntityIdList, groupTypeList);
    }

}
