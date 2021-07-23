/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.ci;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.dto.ci.CiAuthVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.GroupType;
import codedriver.framework.common.constvalue.UserType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiAuthMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    public static boolean hasPrivilege(List<CiAuthVo> authList, CiAuthType... auths) {
        String userUuid = UserContext.get().getUserUuid(true);
        List<String> teamUuidList = null;
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        for (CiAuthVo ciAuthVo : authList) {
            for (CiAuthType auth : auths) {
                if (ciAuthVo.getAction().equals(auth.getValue())) {
                    switch (ciAuthVo.getAuthType()) {
                        case "common":
                            if (ciAuthVo.getAuthUuid().equals(UserType.ALL.getValue())) {
                                return true;
                            }
                            break;
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
        return false;
    }

    private static boolean hasCiPrivilege(Long ciId, CiAuthType... auths) {
        if (ciId != null) {
            List<CiAuthVo> authList = instance.ciAuthMapper.getCiAuthByCiId(ciId);
            return hasPrivilege(authList, auths);
        }
        return false;
    }

    public static Chain chain() {
        return new Chain();
    }

    public static class Chain {
        private boolean hasAuth = false;

        public Chain checkCiManagePrivilege() {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class);
            }
            return this;
        }

        public Chain checkCiManagePrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE);
                }
            }
            return this;
        }

        public Chain checkCiEntityUpdatePrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.TRANSACTIONMANAGE);
                }
            }
            return this;
        }

        public Chain checkCiEntityRecoverPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYRECOVER);
                }
            }
            return this;
        }

        public Chain checkCiEntityTransactionPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.TRANSACTIONMANAGE);
                }
            }
            return this;
        }

        public Chain checkCiEntityInsertPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYINSERT, CiAuthType.TRANSACTIONMANAGE);
                }
            }
            return this;
        }

        public Chain checkCiEntityDeletePrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE);
                }
            }
            return this;
        }

        public Chain checkCiEntityQueryPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYQUERY);
                }
            }
            return this;
        }

        public Chain checkViewPasswordPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYINSERT, CiAuthType.PASSWORDVIEW);
                }
            }
            return this;
        }


        public Chain checkIsInGroup(Long ciEntityId, GroupType... groupType) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.isInGroup(ciEntityId, groupType);
            }
            return this;
        }

        public boolean check() {
            return hasAuth;
        }

    }


    private static boolean isInGroup(Long ciEntityId, GroupType... groupType) {
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
