/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.group;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
