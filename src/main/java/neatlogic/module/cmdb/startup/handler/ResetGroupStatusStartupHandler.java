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

package neatlogic.module.cmdb.startup.handler;

import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.cmdb.enums.group.Status;
import neatlogic.framework.startup.IStartup;
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
