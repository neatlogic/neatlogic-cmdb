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
    public void executeForCurrentTenant() {
        List<SyncAuditVo> auditList = syncAuditMapper.getDoingSyncByServerId(Config.SCHEDULE_SERVER_ID);
        if (CollectionUtils.isNotEmpty(auditList)) {
            for (SyncAuditVo audit : auditList) {
                audit.setStatus(SyncStatus.DONE.getValue());
                audit.setError("系统重启，作业终止");
                syncAuditMapper.updateSyncAuditStatus(audit);
            }
        }
    }

    @Override
    public void executeForAllTenant() {

    }
}
