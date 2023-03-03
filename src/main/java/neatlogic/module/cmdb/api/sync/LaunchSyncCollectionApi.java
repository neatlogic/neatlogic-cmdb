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
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.sync.CiSyncManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class LaunchSyncCollectionApi extends PrivateApiComponentBase {

    @Autowired
    private SyncMapper syncMapper;


    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/launch";
    }

    @Override
    public String getName() {
        return "执行自动采集";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "采集id"), @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "采集id列表"), @Param(name = "isAll", type = ApiParamType.INTEGER, desc = "是否执行所有主动采集，不提供id或idList参数下此参数才有效")})
    @Description(desc = "执行自动采集接口，采集会在后台执行")
    @ResubmitInterval(value = 5)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer isAll = jsonObj.getInteger("isAll");
        JSONArray idList = jsonObj.getJSONArray("idList");
        if (id == null && CollectionUtils.isEmpty(idList) && isAll == null) {
            throw new ParamNotExistsException("id", "idList", "isAll");
        }
        List<SyncCiCollectionVo> syncCiCollectionList = new ArrayList<>();
        if (id != null) {
            SyncCiCollectionVo syncCiCollectionVo = syncMapper.getSyncCiCollectionById(id);
            if (syncCiCollectionVo == null) {
                throw new SyncCiCollectionNotFoundException(id);
            }
            syncCiCollectionList.add(syncCiCollectionVo);
        } else if (CollectionUtils.isNotEmpty(idList)) {
            List<Long> pIdList = idList.stream().map(d -> Long.parseLong(d.toString())).collect(Collectors.toList());
            syncCiCollectionList = syncMapper.getSyncCiCollectionByIdList(pIdList);
        } else if (isAll.equals(1)) {
            SyncCiCollectionVo p = new SyncCiCollectionVo();
            p.setCollectMode(CollectMode.INITIATIVE.getValue());
            p.setPageSize(100);
            p.setCurrentPage(1);
            List<SyncCiCollectionVo> sList = syncMapper.searchSyncCiCollection(p);
            while (CollectionUtils.isNotEmpty(sList)) {
                syncCiCollectionList.addAll(sList);
                p.setCurrentPage(p.getCurrentPage() + 1);
                sList = syncMapper.searchSyncCiCollection(p);
            }
        }
        if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
            CiSyncManager.doSync(syncCiCollectionList);
        }
        return null;
    }

}
