/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.group;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.dto.group.CiGroupVo;
import codedriver.framework.cmdb.dto.group.GroupVo;
import codedriver.framework.cmdb.exception.group.GroupNotFoundException;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {
    // private final static Logger logger = LoggerFactory.getLogger(CiEntityServiceImpl.class);

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Resource
    private RoleMapper roleMapper;

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
    }

    @Transactional
    @Override
    public void updateGroup(GroupVo groupVo) {
        if (groupMapper.getGroupById(groupVo.getId()) == null) {
            throw new GroupNotFoundException(groupVo.getId());
        }
        groupMapper.deleteCiGroupByGroupId(groupVo.getId());
        groupMapper.updateGroup(groupVo);
        if (CollectionUtils.isNotEmpty(groupVo.getCiGroupList())) {
            for (CiGroupVo ciGroupVo : groupVo.getCiGroupList()) {
                ciGroupVo.setGroupId(groupVo.getId());
                groupMapper.insertCiGroup(ciGroupVo);
            }
        }
    }
}
