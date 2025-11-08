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

import neatlogic.framework.cmdb.dto.sync.SyncAuditVo;
import neatlogic.framework.cmdb.enums.sync.SyncStatus;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.startup.StartupBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ResetCiSyncStatusStartupHandler extends StartupBase {
    @Resource
    private SyncAuditMapper syncAuditMapper;

    @Override
    public String getName() {
        return "重置自动采集作业状态";
    }

    @Override
    public int sort() {
        return 3;
    }

    @Override
    public int executeForCurrentTenant() {
        List<SyncAuditVo> auditList = syncAuditMapper.getDoingSyncByServerId(Config.SCHEDULE_SERVER_ID);
        if (CollectionUtils.isNotEmpty(auditList)) {
            for (SyncAuditVo audit : auditList) {
                audit.setStatus(SyncStatus.DONE.getValue());
                audit.setError("系统重启，作业终止");
                syncAuditMapper.updateSyncAuditToEnd(audit);
            }
        }
        return 0;
    }

}
