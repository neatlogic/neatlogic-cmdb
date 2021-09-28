/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.startup;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.startup.IStartup;
import org.springframework.stereotype.Service;

@Service
public class ClearExpiredScheduleStartupHandler implements IStartup {

    @Override
    public String getName() {
        return "启动定时作业清理失效关系";
    }

    @Override
    public int sort() {
        return 3;
    }

    @Override
    public void executeForCurrentTenant() {
        IJob handler = SchedulerManager.getHandler("codedriver.module.cmdb.schedule.handler.ClearExpiredRelEntityScheduleJob");
        String tenantUuid = TenantContext.get().getTenantUuid();
        handler.initJob(tenantUuid);
    }

    @Override
    public void executeForAllTenant() {

    }
}
