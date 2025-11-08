/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
                //可能是复制，因此只要是添加先清空id，避免用到老id
                ciGroupVo.setId(null);
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
