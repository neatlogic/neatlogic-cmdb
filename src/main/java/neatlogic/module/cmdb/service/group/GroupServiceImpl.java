/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.service.group;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.group.CiGroupVo;
import neatlogic.framework.cmdb.dto.group.GroupAuthVo;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.cmdb.exception.group.GroupNotFoundException;
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


    @Override
    public List<Long> getCurrentUserGroupIdList() {
        String userUuid = UserContext.get().getUserUuid();
        List<String> teamUuidList = UserContext.get().getTeamUuidList();
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
