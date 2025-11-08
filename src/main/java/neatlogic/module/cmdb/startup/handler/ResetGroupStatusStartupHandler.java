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
