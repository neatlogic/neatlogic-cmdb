/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.SYNC_MODIFY;
import codedriver.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteSyncAuditApi extends PrivateApiComponentBase {

    @Resource
    private SyncAuditMapper syncAuditMapper;


    @Override
    public String getToken() {
        return "/cmdb/syncaudit/delete";
    }

    @Override
    public String getName() {
        return "删除自动采集执行日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")
    })
    @Description(desc = "删除自动采集执行日志接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        syncAuditMapper.deleteSyncAuditById(jsonObj.getLong("id"));
        return null;
    }

}
