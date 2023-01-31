/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.sync;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.sync.SyncPolicyVo;
import neatlogic.framework.cmdb.exception.sync.SyncPolicyNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteSyncPolicyApi extends PrivateApiComponentBase {

    @Autowired
    private SyncMapper syncMapper;


    @Override
    public String getToken() {
        return "/cmdb/sync/policy/delete";
    }

    @Override
    public String getName() {
        return "删除自动采集策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "策略id")})
    @Description(desc = "删除自动采集策略接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        SyncPolicyVo syncPolicyVo = syncMapper.getSyncPolicyById(id);
        if (syncPolicyVo == null) {
            throw new SyncPolicyNotFoundException(id);
        }
        syncMapper.deleteSyncPolicyById(id);
        return null;
    }

}
