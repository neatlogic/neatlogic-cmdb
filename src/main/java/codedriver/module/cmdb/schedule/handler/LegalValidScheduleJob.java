/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.schedule.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.legalvalid.LegalValidVo;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.cmdb.dao.mapper.legalvalid.LegalValidMapper;
import codedriver.module.cmdb.legalvalid.LegalValidManager;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 合规检查定时器
 */
@Component
@DisallowConcurrentExecution
public class LegalValidScheduleJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(LegalValidScheduleJob.class);

    @Resource
    private LegalValidMapper legalValidMapper;


    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        LegalValidVo jobVo = legalValidMapper.getLegalValidById(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null && jobVo.getIsActive().equals(1)) {
            return jobVo.getCron().equals(jobObject.getCron());
        }
        return false;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        LegalValidVo jobVo = legalValidMapper.getLegalValidById(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null) {
            JobObject newJobObject = new JobObject.Builder(jobVo.getId().toString(), this.getGroupName(), this.getClassName(), tenantUuid).withCron(jobVo.getCron()).addData("legalValidId", jobVo.getId()).build();
            schedulerManager.loadJob(newJobObject);
        } else {
            schedulerManager.unloadJob(jobObject);
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        LegalValidVo legalValidVo = new LegalValidVo();
        legalValidVo.setIsActive(1);
        List<LegalValidVo> jobList = legalValidMapper.searchLegalValid(legalValidVo);
        if (CollectionUtils.isNotEmpty(jobList)) {
            for (LegalValidVo vo : jobList) {
                JobObject newJobObject = new JobObject.Builder(vo.getId().toString(), this.getGroupName(), this.getClassName(), tenantUuid).withCron(vo.getCron()).addData("legalValidId", vo.getId()).build();
                schedulerManager.loadJob(newJobObject);
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        Long legalValidId = (Long) jobObject.getData("legalValidId");
        LegalValidVo legalValidVo = legalValidMapper.getLegalValidById(legalValidId);
        if (legalValidVo != null) {
            LegalValidManager.doValid(legalValidVo);
        } else {
            schedulerManager.unloadJob(jobObject);
        }
    }


    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-LEGAL-VALID";
    }

}
