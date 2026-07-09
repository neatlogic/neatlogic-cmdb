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

import neatlogic.framework.cmdb.cientityevent.CiEntityEventManager;
import neatlogic.framework.cmdb.dao.mapper.cientity.CiEntityEventMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityEventVo;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.startup.StartupBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RecoverCiEntityEventStartupHandler extends StartupBase {
    @Resource
    private CiEntityEventMapper ciEntityEventMapper;

    @Override
    public String getName() {
        return "恢复配置项事件队列";
    }

    @Override
    public int sort() {
        return 4;
    }

    @Override
    public int executeForCurrentTenant() {
        // 重启后仅恢复当前serverId遗留的running事件，避免误抢其他节点正在执行的事件。
        ciEntityEventMapper.updateRunningCiEntityEventToPendingByServerId(Config.SCHEDULE_SERVER_ID);
        List<CiEntityEventVo> eventList = ciEntityEventMapper.getPendingCiEntityEventList();
        if (CollectionUtils.isNotEmpty(eventList)) {
            for (CiEntityEventVo eventVo : eventList) {
                CiEntityEventManager.submitEvent(eventVo.getId());
            }
        }
        return 0;
    }
}
