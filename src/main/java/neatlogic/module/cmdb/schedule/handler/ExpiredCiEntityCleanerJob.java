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

import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
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
