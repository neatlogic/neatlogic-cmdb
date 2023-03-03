/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.sync;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.sync.SyncPolicyVo;
import neatlogic.framework.cmdb.dto.sync.SyncScheduleVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.module.cmdb.service.sync.SyncService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveSyncPolicyApi extends PrivateApiComponentBase {

    @Resource
    private SyncService syncService;
    @Resource
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
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 10, isRequired = true, xss = true, desc = "名称"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"),
            @Param(name = "cronList", type = ApiParamType.JSONARRAY, desc = "定时设置"),
            @Param(name = "conditionList", type = ApiParamType.JSONARRAY, desc = "筛选条件")})
    @Description(desc = "保存自动采集策略接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncPolicyVo syncPolicyVo = JSONObject.toJavaObject(jsonObj, SyncPolicyVo.class);
        syncService.saveSyncPolicy(syncPolicyVo);
        if (CollectionUtils.isNotEmpty(syncPolicyVo.getCronList())) {
            IJob handler = SchedulerManager.getHandler("neatlogic.module.cmdb.schedule.handler.SyncCiEntityScheduleJob");
            String tenantUuid = TenantContext.get().getTenantUuid();
            for (SyncScheduleVo cron : syncPolicyVo.getCronList()) {
                JobObject newJobObject = new JobObject.Builder(cron.getId().toString(), handler.getGroupName(), handler.getClassName(), tenantUuid).withCron(cron.getCron()).addData("policyId", cron.getPolicyId()).build();
                schedulerManager.loadJob(newJobObject);
            }
        }
        return null;
    }
}
