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

package neatlogic.module.cmdb.api.sync;

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
