package codedriver.module.cmdb.service.ci;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
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
public class CiAuthChecker {
    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private CiAuthMapper ciAuthMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    private static CiAuthChecker instance;

    @Autowired
    public CiAuthChecker() {
        instance = this;
    }

    /**
     * 判断模型管理权限
     */
    public static boolean hasCiManagePrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE);
    }

    private static boolean hasCiPrivilege(Long ciId, CiAuthType... auths) {
        if (ciId != null) {
            String userUuid = UserContext.get().getUserUuid(true);
            List<String> teamUuidList = null;
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<CiAuthVo> authList = instance.ciAuthMapper.getCiAuthByCiId(ciId);
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
                                    teamUuidList = instance.teamMapper.getTeamUuidListByUserUuid(userUuid);
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean hasAuth = false;

        public Builder hasCiManagePrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.hasCiManagePrivilege(ciId);
            }
            return this;
        }

        public Builder hasCiEntityUpdatePrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.hasCiEntityUpdatePrivilege(ciId);
            }
            return this;
        }

        public Builder hasCiEntityQueryPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.hasCiEntityQueryPrivilege(ciId);
            }
            return this;
        }

        public Builder hasCiEntityQueryPrivilege(Long ciId, Long ciEntityId) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.hasCiEntityQueryPrivilege(ciId, ciEntityId);
            }
            return this;
        }

        public Builder isInGroup(Long ciEntityId, GroupType... groupType) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.isInGroup(ciEntityId, groupType);
            }
            return this;
        }

        public boolean check() {
            return hasAuth;
        }

    }

    public static boolean hasPasswordPrivilege(Long ciEntityId) {
        // FIXME 补充权限校验
        return false;
    }

    public static boolean hasCiEntityQueryPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE, CiAuthType.CIENTITYQUERY,
            CiAuthType.CIENTITYQUERY);
    }

    public static boolean hasCiEntityQueryPrivilege(Long ciId, Long ciEntityId) {
        if (hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE, CiAuthType.CIENTITYQUERY,
            CiAuthType.CIENTITYQUERY)) {
            return true;
        } else {
            // FIXME 补充维护群组判断
        }
        return false;
    }

    public static boolean hasCiEntityInsertPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYINSERT);
    }

    public static boolean hasCiEntityUpdatePrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE);
    }

    public static boolean hasCiEntityDeletePrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE);
    }

    public static boolean hasTransactionPrivilege(Long ciId) {
        return hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.TRANSACTIONMANAGE);
    }

    public static boolean isInGroup(Long ciEntityId, GroupType... groupType) {
        @SuppressWarnings("serial")
        List<Long> returnList = isInGroup(new ArrayList<Long>() {
            {
                this.add(ciEntityId);
            }
        }, groupType);
        return returnList.contains(ciEntityId);
    }

    public static List<Long> isInGroup(List<Long> ciEntityIdList, GroupType... groupType) {
        String userUuid = UserContext.get().getUserUuid(true);
        List<String> teamUuidList = instance.teamMapper.getTeamUuidListByUserUuid(userUuid);
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        // 获取当前用户属于哪个圈子
        List<Long> groupIdList = instance.groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
        List<String> groupTypeList = new ArrayList<>();
        for (GroupType g : groupType) {
            groupTypeList.add(g.getValue());
        }
        if (CollectionUtils.isNotEmpty(groupIdList) && CollectionUtils.isNotEmpty(ciEntityIdList)
            && CollectionUtils.isNotEmpty(groupTypeList)) {
            return instance.ciEntityMapper.getCiEntityIdByGroupIdList(groupIdList, ciEntityIdList, groupTypeList);
        } else {
            return new ArrayList<>();
        }
    }

}
