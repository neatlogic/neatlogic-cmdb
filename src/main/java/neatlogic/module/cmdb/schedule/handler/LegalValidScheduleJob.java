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
import neatlogic.framework.cmdb.dto.legalvalid.LegalValidVo;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.cmdb.dao.mapper.legalvalid.LegalValidMapper;
import neatlogic.module.cmdb.legalvalid.LegalValidManager;
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
