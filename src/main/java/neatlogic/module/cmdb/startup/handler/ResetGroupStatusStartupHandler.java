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

package neatlogic.module.cmdb.startup.handler;

import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.cmdb.enums.group.Status;
import neatlogic.framework.startup.StartupBase;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
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
    public int executeForCurrentTenant() {
        GroupVo gVo = new GroupVo();
        gVo.setStatus(Status.DOING.getValue());
        List<GroupVo> groupList = groupMapper.getDoingGroupByServerId(gVo);
        if (CollectionUtils.isNotEmpty(groupList)) {
            for (GroupVo groupVo : groupList) {
                groupVo.setStatus(Status.DONE.getValue());
                groupMapper.updateGroupStatus(groupVo);
            }
        }
        return 0;
    }

}
