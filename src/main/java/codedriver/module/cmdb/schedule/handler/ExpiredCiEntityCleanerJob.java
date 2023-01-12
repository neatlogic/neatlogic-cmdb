/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.schedule.handler;

import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.InputFrom;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 清理过期配置项作业
 */
@Component
@DisallowConcurrentExecution
public class ExpiredCiEntityCleanerJob extends JobBase {
    Logger logger = LoggerFactory.getLogger(ExpiredCiEntityCleanerJob.class);
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-EXPIRED-CIENTITY-CLEANER-GROUP";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        schedulerManager.loadJob(jobObject);
    }

    @Override
    public void initJob(String tenantUuid) {
        //每天凌晨1点运行
        JobObject jobObject = new JobObject.Builder("EXPIRED-CIENTITY-CLEANER-JOB", this.getGroupName(), this.getClassName(), tenantUuid)
                .withCron("0 0 1 * * ?")
                //.withCron("0 * * * * ?")//测试用
                .build();
        this.reloadJob(jobObject);
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCurrentPage(1);
        ciEntityVo.setPageSize(100);
        List<Long> ciEntityIdList = ciEntityMapper.searchExpiredCiEntityId(ciEntityVo);
        InputFromContext.init(InputFrom.CRON);
        while (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            for (Long ciEntityId : ciEntityIdList) {
                CiEntityVo delVo = new CiEntityVo();
                delVo.setId(ciEntityId);
                delVo.setDescription("过期配置项自动删除");
                try {
                    ciEntityService.deleteCiEntity(delVo, true);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
            ciEntityIdList = ciEntityMapper.searchExpiredCiEntityId(ciEntityVo);
        }
    }
}
