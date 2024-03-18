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
                syncAuditMapper.updateSyncAuditStatus(audit);
            }
        }
        return 0;
    }

}
