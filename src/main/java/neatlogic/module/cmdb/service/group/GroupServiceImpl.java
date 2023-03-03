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

package neatlogic.module.cmdb.service.group;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.group.CiGroupVo;
import neatlogic.framework.cmdb.dto.group.GroupAuthVo;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.cmdb.exception.group.GroupNotFoundException;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {
    // private final static Logger logger = LoggerFactory.getLogger(CiEntityServiceImpl.class);

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private TeamMapper teamMapper;


    @Override
    public List<Long> getCurrentUserGroupIdList() {
        String userUuid = UserContext.get().getUserUuid(true);
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        return groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
    }

    @Transactional
    @Override
    public void insertGroup(GroupVo groupVo) {
        groupMapper.insertGroup(groupVo);
        if (CollectionUtils.isNotEmpty(groupVo.getCiGroupList())) {
            for (CiGroupVo ciGroupVo : groupVo.getCiGroupList()) {
                ciGroupVo.setGroupId(groupVo.getId());
                groupMapper.insertCiGroup(ciGroupVo);
            }
        }
        if (CollectionUtils.isNotEmpty(groupVo.getGroupAuthList())) {
            for (GroupAuthVo authVo : groupVo.getGroupAuthList()) {
                authVo.setGroupId(groupVo.getId());
                groupMapper.insertGroupAuth(authVo);
            }
        }
    }

    @Transactional
    @Override
    public void updateGroup(GroupVo groupVo) {
        if (groupMapper.getGroupById(groupVo.getId()) == null) {
            throw new GroupNotFoundException(groupVo.getId());
        }
        groupMapper.deleteCiGroupByGroupId(groupVo.getId());
        groupMapper.deleteGroupAuthByGroupId(groupVo.getId());
        groupMapper.updateGroup(groupVo);
        if (CollectionUtils.isNotEmpty(groupVo.getCiGroupList())) {
            for (CiGroupVo ciGroupVo : groupVo.getCiGroupList()) {
                ciGroupVo.setGroupId(groupVo.getId());
                groupMapper.insertCiGroup(ciGroupVo);
            }
        }
        if (CollectionUtils.isNotEmpty(groupVo.getGroupAuthList())) {
            for (GroupAuthVo authVo : groupVo.getGroupAuthList()) {
                authVo.setGroupId(groupVo.getId());
                groupMapper.insertGroupAuth(authVo);
            }
        }
    }
}
