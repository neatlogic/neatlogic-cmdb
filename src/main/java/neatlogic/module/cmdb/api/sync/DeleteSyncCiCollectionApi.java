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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.SyncPolicyVo;
import neatlogic.framework.cmdb.exception.sync.CiCollectionIsInUsedException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteSyncCiCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncMapper syncMapper;

    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/delete";
    }

    @Override
    public String getName() {
        return "nmcas.deletesynccicollectionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid")})
    @Description(desc = "nmcas.deletesynccicollectionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        List<SyncPolicyVo> policyList = syncMapper.getSyncPolicyByCiCollectionId(id);
        if (CollectionUtils.isNotEmpty(policyList)) {
            throw new CiCollectionIsInUsedException(policyList);
        }
        syncMapper.deleteSyncCiCollectionById(id);
        return null;
    }

}
