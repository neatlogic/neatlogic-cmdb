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
