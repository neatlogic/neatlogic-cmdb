/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.SyncPolicyVo;
import codedriver.framework.cmdb.dto.sync.SyncScheduleVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.module.cmdb.auth.label.SYNC_MODIFY;
import codedriver.module.cmdb.service.sync.SyncService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveSyncPolicyApi extends PrivateApiComponentBase {

    @Autowired
    private SyncService syncService;

    @Autowired
    private SchedulerManager schedulerManager;


    @Override
    public String getToken() {
        return "/cmdb/sync/policy/save";
    }

    @Override
    public String getName() {
        return "保存自动采集策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，存在代表修改，不存在代表新增"),
            @Param(name = "ciCollectionId", type = ApiParamType.LONG, isRequired = true, desc = "模型集合id"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活"),
            @Param(name = "cronList", type = ApiParamType.JSONARRAY, desc = "定时设置"),
            @Param(name = "conditionList", type = ApiParamType.JSONARRAY, desc = "筛选条件")})
    @Description(desc = "保存自动采集策略接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncPolicyVo syncPolicyVo = JSONObject.toJavaObject(jsonObj, SyncPolicyVo.class);
        syncService.saveSyncPolicy(syncPolicyVo);
        if (CollectionUtils.isNotEmpty(syncPolicyVo.getCronList())) {
            IJob handler = SchedulerManager.getHandler("codedriver.module.cmdb.schedule.handler.SyncCiEntityScheduleJob");
            String tenantUuid = TenantContext.get().getTenantUuid();
            for (SyncScheduleVo cron : syncPolicyVo.getCronList()) {
                JobObject newJobObject = new JobObject.Builder(cron.getId().toString(), handler.getGroupName(), handler.getClassName(), tenantUuid).withCron(cron.getCron()).addData("policyId", cron.getPolicyId()).build();
                schedulerManager.loadJob(newJobObject);
            }
        }
        return null;
    }
}
