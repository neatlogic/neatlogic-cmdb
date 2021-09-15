/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.sync.SyncAuditVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.sync.SyncAuditMapper;
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

    @Input({@Param(name = "ciCollectionId", type = ApiParamType.LONG, isRequired = true, desc = "采集映射配置id")})
    @Description(desc = "搜索自动采集执行日志接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncAuditVo syncAuditVo = JSONObject.toJavaObject(jsonObj, SyncAuditVo.class);
        List<SyncAuditVo> syncAuditList = syncAuditMapper.searchSyncAudit(syncAuditVo);
        if (CollectionUtils.isNotEmpty(syncAuditList)) {
            int rowNum = syncAuditMapper.searchSyncAuditCount(syncAuditVo);
            syncAuditVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(syncAuditList, syncAuditVo);
    }

}
