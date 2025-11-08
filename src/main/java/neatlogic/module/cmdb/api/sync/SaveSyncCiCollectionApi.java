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
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.exception.sync.SyncMappingNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.sync.SyncService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveSyncCiCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncService syncService;


    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/save";
    }

    @Override
    public String getName() {
        return "保存配置项集合映射设置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，存在代表修改，不存在代表新增"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "collectionName", isRequired = true, maxLength = 200, xss = true, type = ApiParamType.STRING, desc = "集合名称"),
            @Param(name = "parentKey", type = ApiParamType.STRING, desc = "父属性"),
            @Param(name = "mappingList", type = ApiParamType.JSONARRAY, desc = "映射内容"),
            @Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "说明", maxLength = 500),
            @Param(name = "uniqueAttrIdList", type = ApiParamType.JSONARRAY, desc = "唯一规则"),
            @Param(name = "isAutoCommit", type = ApiParamType.INTEGER, desc = "是否自动提交"),
            @Param(name = "isAllowMultiple", type = ApiParamType.INTEGER, desc = "允许更新多条")})
    @Description(desc = "保存配置项集合映射设置")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncCiCollectionVo syncCiCollectionVo = JSON.toJavaObject(jsonObj, SyncCiCollectionVo.class);

        if (CollectionUtils.isEmpty(syncCiCollectionVo.getMappingList())) {
            throw new SyncMappingNotFoundException();
        }
        /*if (syncCiCollectionVo.getCollectMode().equals(CollectMode.INITIATIVE.getValue()) && syncMapper.checkInitiativeSyncCiCollectionIsExists(syncCiCollectionVo) > 0) {
            throw new InitiativeSyncCiCollectionIsExistsException(syncCiCollectionVo.getCollectionName());
        }*/
        syncService.saveSyncCiCollection(syncCiCollectionVo);
        return null;
    }
}
