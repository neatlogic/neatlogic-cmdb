/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.startup.handler;

import codedriver.framework.cmdb.dto.group.GroupVo;
import codedriver.framework.cmdb.enums.group.Status;
import codedriver.framework.startup.IStartup;
import codedriver.framework.startup.StartupBase;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ResetGroupStatusStartupHandler extends StartupBase {
    @Resource
    private GroupMapper groupMapper;

    @Override
    public String getName() {
        return "重置团体应用状态";
    }

    @Override
    public int sort() {
        return 1;
    }

    @Override
    public void executeForCurrentTenant() {
        GroupVo gVo = new GroupVo();
        gVo.setStatus(Status.DOING.getValue());
        List<GroupVo> groupList = groupMapper.getDoingGroupByServerId(gVo);
        if (CollectionUtils.isNotEmpty(groupList)) {
            for (GroupVo groupVo : groupList) {
                groupVo.setStatus(Status.DONE.getValue());
                groupMapper.updateGroupStatus(groupVo);
            }
        }
    }

    @Override
    public void executeForAllTenant() {

    }
}
