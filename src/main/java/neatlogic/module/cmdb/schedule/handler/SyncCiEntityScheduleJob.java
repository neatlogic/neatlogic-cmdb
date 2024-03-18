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

package neatlogic.module.cmdb.schedule.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncPolicyVo;
import neatlogic.framework.cmdb.dto.sync.SyncScheduleVo;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.sync.CiSyncManager;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置项自动同步定时器
 */
@Component
@DisallowConcurrentExecution
public class SyncCiEntityScheduleJob extends JobBase {

    @Resource
    private SyncMapper syncMapper;


    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        SyncScheduleVo jobVo = syncMapper.getSyncScheduleById(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null && jobVo.getIsActive().equals(1)) {
            return jobVo.getCron().equals(jobObject.getCron());
        }
        return false;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        SyncScheduleVo jobVo = syncMapper.getSyncScheduleById(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null) {
            JobObject newJobObject = new JobObject.Builder(jobVo.getId().toString(), this.getGroupName(), this.getClassName(), tenantUuid).withCron(jobVo.getCron()).addData("policyId", jobVo.getPolicyId()).build();
            schedulerManager.loadJob(newJobObject);
        } else {
            schedulerManager.unloadJob(jobObject);
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        List<SyncScheduleVo> jobList = syncMapper.getAllActivePolicySchedule();
        if (CollectionUtils.isNotEmpty(jobList)) {
            for (SyncScheduleVo vo : jobList) {
                JobObject newJobObject = new JobObject.Builder(vo.getId().toString(), this.getGroupName(), this.getClassName(), tenantUuid).withCron(vo.getCron()).addData("policyId", vo.getPolicyId()).build();
                schedulerManager.loadJob(newJobObject);
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        Long policyId = (Long) jobObject.getData("policyId");
        SyncPolicyVo syncPolicyVo = syncMapper.getSyncPolicyById(policyId);
        if (syncPolicyVo != null) {
            SyncCiCollectionVo syncCiCollectionVo = syncMapper.getSyncCiCollectionById(syncPolicyVo.getCiCollectionId());
            if (syncCiCollectionVo != null) {
                List<SyncCiCollectionVo> syncCiCollectionList = new ArrayList<>();
                syncCiCollectionList.add(syncCiCollectionVo);
                CiSyncManager.doSync(syncCiCollectionList);
            } else {
                schedulerManager.unloadJob(jobObject);
            }
        }
    }


    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-SYNC-CIENTITY";
    }

}
