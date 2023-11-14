/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.service.ci;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.UserType;
import neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class CiAuthChecker {

    @Resource
    private CiAuthMapper ciAuthMapper;

    @Resource
    private GroupMapper groupMapper;

    private static CiAuthChecker instance;

    @Autowired
    public CiAuthChecker() {
        instance = this;
    }

    public static boolean hasPrivilege(List<CiAuthVo> authList, CiAuthType... auths) {
        String userUuid = UserContext.get().getUserUuid();
        List<String> teamUuidList = UserContext.get().getTeamUuidList();
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
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYINSERT, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYQUERY, CiAuthType.PASSWORDVIEW, CiAuthType.CIENTITYRECOVER);
                }
            }
            return this;
        }

        public Chain checkViewPasswordPrivilege(Long ciId) {
            if (!hasAuth) {
                hasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiPrivilege(ciId, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYINSERT, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.PASSWORDVIEW, CiAuthType.CIENTITYRECOVER);
                }
            }
            return this;
        }


        public Chain checkCiEntityIsInGroup(Long ciEntityId, GroupType... groupType) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.isCiEntityInGroup(ciEntityId, groupType);
            }
            return this;
        }

        public Chain checkCiIsInGroup(Long ciId, GroupType... groupType) {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.isCiInGroup(ciId, groupType);
            }
            return this;
        }

        public Chain checkAuth(Long ciId, CiAuthType auth) {
            if (auth == CiAuthType.CIMANAGE) {
                return checkCiManagePrivilege(ciId);
            } else if (auth == CiAuthType.CIENTITYINSERT) {
                return checkCiEntityInsertPrivilege(ciId);
            } else if (auth == CiAuthType.CIENTITYDELETE) {
                return checkCiEntityDeletePrivilege(ciId);
            } else if (auth == CiAuthType.TRANSACTIONMANAGE) {
                return checkCiEntityTransactionPrivilege(ciId);
            } else if (auth == CiAuthType.CIENTITYRECOVER) {
                return checkCiEntityRecoverPrivilege(ciId);
            } else if (auth == CiAuthType.PASSWORDVIEW) {
                return checkViewPasswordPrivilege(ciId);
            }
            return this;
        }

        public boolean check() {
            return hasAuth;
        }

    }

    public static boolean isCiInGroup(Long ciId, GroupType... groupType) {
        List<Long> returnList = isCiInGroup(new ArrayList<Long>() {
            {
                this.add(ciId);
            }
        }, groupType);
        return returnList.contains(ciId);
    }

    public static boolean isCiEntityInGroup(Long ciEntityId, GroupType... groupType) {
        List<Long> returnList = isCiEntityInGroup(new ArrayList<Long>() {
            {
                this.add(ciEntityId);
            }
        }, groupType);
        return returnList.contains(ciEntityId);
    }

    public static List<Long> isCiInGroup(List<Long> ciIdList, GroupType... groupType) {
        String userUuid = UserContext.get().getUserUuid();
        List<String> teamUuidList = UserContext.get().getTeamUuidList();
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        // 获取当前用户属于哪个圈子
        List<Long> groupIdList = instance.groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
        List<String> groupTypeList = new ArrayList<>();
        for (GroupType g : groupType) {
            groupTypeList.add(g.getValue());
        }
        if (CollectionUtils.isNotEmpty(groupIdList) && CollectionUtils.isNotEmpty(ciIdList)
                && CollectionUtils.isNotEmpty(groupTypeList)) {
            return instance.groupMapper.getCiIdByGroupIdList(groupIdList, ciIdList, groupTypeList);
        } else {
            return new ArrayList<>();
        }
    }

    public static List<Long> isCiEntityInGroup(List<Long> ciEntityIdList, GroupType... groupType) {
        String userUuid = UserContext.get().getUserUuid();
        List<String> teamUuidList = UserContext.get().getTeamUuidList();
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        // 获取当前用户属于哪个圈子
        List<Long> groupIdList = instance.groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
        List<String> groupTypeList = new ArrayList<>();
        for (GroupType g : groupType) {
            groupTypeList.add(g.getValue());
        }
        if (CollectionUtils.isNotEmpty(groupIdList) && CollectionUtils.isNotEmpty(ciEntityIdList)
                && CollectionUtils.isNotEmpty(groupTypeList)) {
            return instance.groupMapper.getCiEntityIdByGroupIdList(groupIdList, ciEntityIdList, groupTypeList);
        } else {
            return new ArrayList<>();
        }
    }


}
