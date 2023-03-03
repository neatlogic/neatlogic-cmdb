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

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.sync.SyncAuditVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchSyncAuditApi extends PrivateApiComponentBase {

    @Resource
    private SyncAuditMapper syncAuditMapper;

    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public String getToken() {
        return "/cmdb/syncaudit/search";
    }

    @Override
    public String getName() {
        return "搜索自动采集执行日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciCollectionId", type = ApiParamType.LONG, desc = "采集映射配置id"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确查找"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "doing,done", desc = "状态"),
            @Param(name = "startTimeRange", type = ApiParamType.JSONARRAY, desc = "开始时间范围"),
            @Param(name = "endTimeRange", type = ApiParamType.JSONARRAY, desc = "结束时间范围")})
    @Output({@Param(name = "tbodyList", explode = SyncAuditVo[].class), @Param(explode = BasePageVo.class)})
    @Description(desc = "搜索自动采集执行日志接口，如果提供了idList参数，将会直接返回日志列表，没有tbodyList包裹")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncAuditVo syncAuditVo = JSONObject.toJavaObject(jsonObj, SyncAuditVo.class);
        List<SyncAuditVo> syncAuditList = syncAuditMapper.searchSyncAudit(syncAuditVo);
        for (SyncAuditVo syncAudit : syncAuditList) {
            if (syncAudit.getTransactionGroupId() != null) {
                syncAudit.setTransactionCount(transactionMapper.getTransactionCountByGroupId(syncAudit.getTransactionGroupId()));
            }
        }
        if (CollectionUtils.isEmpty(syncAuditVo.getIdList())) {
            if (CollectionUtils.isNotEmpty(syncAuditList)) {
                int rowNum = syncAuditMapper.searchSyncAuditCount(syncAuditVo);
                syncAuditVo.setRowNum(rowNum);
            }
            return TableResultUtil.getResult(syncAuditList, syncAuditVo);
        } else {
            return syncAuditList;
        }
    }

}
