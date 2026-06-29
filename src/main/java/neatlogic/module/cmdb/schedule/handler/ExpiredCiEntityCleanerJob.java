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

import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.enums.JobLoadTriggerType;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
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
    @Override
    public String getName() {
        return "过期配置项定时清理";
    }

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
    public void reloadJob(JobObject jobObject, JobLoadTriggerType triggerType) {
        schedulerManager.loadJob(jobObject, triggerType);
    }

    @Override
    public void initJob(String tenantUuid) {
        //每天凌晨1点运行
        JobObject jobObject = new JobObject.Builder("EXPIRED-CIENTITY-CLEANER-JOB", this.getGroupName(), this.getClassName(), tenantUuid)
                .withCron("0 0 1 * * ?")
                //.withCron("0 * * * * ?")//测试用
                .build();
        this.reloadJob(jobObject, JobLoadTriggerType.SERVER_RESTART);
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
