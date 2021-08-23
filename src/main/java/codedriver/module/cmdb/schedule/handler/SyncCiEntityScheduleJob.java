/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.schedule.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.sync.SyncScheduleVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 报表发送计划定时器
 */
@Component
public class SyncCiEntityScheduleJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(SyncCiEntityScheduleJob.class);

    @Autowired
    private SyncMapper syncMapper;


    @Override
    public Boolean isHealthy(JobObject jobObject) {
        SyncScheduleVo jobVo = syncMapper.getSyncScheduleById(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null && jobVo.getIsActive().equals(1)) {
            return jobVo.getCron().equals(jobObject.getCron());
        }
        return false;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
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
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws JobExecutionException {
        Long policyId = (Long) jobObject.getData("policyId");

    }


    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-SYNC-CIENTITY";
    }

}
