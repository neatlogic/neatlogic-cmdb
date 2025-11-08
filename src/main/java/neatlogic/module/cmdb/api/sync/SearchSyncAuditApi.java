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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.sync.SyncAuditVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
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
            @Param(name = "status", type = ApiParamType.ENUM, rule = "doing,done,paused,pausing", desc = "状态"),
            @Param(name = "startTimeRange", type = ApiParamType.JSONARRAY, desc = "开始时间范围"),
            @Param(name = "endTimeRange", type = ApiParamType.JSONARRAY, desc = "结束时间范围")})
    @Output({@Param(name = "tbodyList", explode = SyncAuditVo[].class), @Param(explode = BasePageVo.class)})
    @Description(desc = "搜索自动采集执行日志接口，如果提供了idList参数，将会直接返回日志列表，没有tbodyList包裹")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncAuditVo syncAuditVo = JSON.toJavaObject(jsonObj, SyncAuditVo.class);
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
