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
