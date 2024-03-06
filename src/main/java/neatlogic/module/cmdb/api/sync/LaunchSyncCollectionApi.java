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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.ObjectMapper;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.sync.CiSyncManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class LaunchSyncCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncMapper syncMapper;

    @Resource
    private ObjectMapper objectMapper;


    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/launch";
    }

    @Override
    public String getName() {
        return "nmcas.launchsynccollectionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "term.cmdb.syncid"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.syncidlist"),
            @Param(name = "collectionList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.collectionlist"),
            //@Param(name = "collectionObjList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.collectionobjlist"),
            @Param(name = "batchTag", type = ApiParamType.STRING, desc = "term.cmdb.batchtag"),
            @Param(name = "startTime", type = ApiParamType.LONG, desc = "common.starttime"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, desc = "term.cmdb.syncisall")})
    @Description(desc = "nmcas.launchsynccollectionapi.getname")
    @ResubmitInterval(value = 5)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Integer isAll = jsonObj.getInteger("isAll");
        Long id = jsonObj.getLong("id");
        JSONArray idList = jsonObj.getJSONArray("idList");
        JSONArray collectionList = jsonObj.getJSONArray("collectionList");
        JSONArray collectionObjList = jsonObj.getJSONArray("collectionObjList");
        Long startTime = jsonObj.getLong("startTime");
        String batchTag = jsonObj.getString("batchTag");
        if (id == null && CollectionUtils.isEmpty(idList) && isAll == null && CollectionUtils.isEmpty(collectionList) && CollectionUtils.isEmpty(collectionObjList)) {
            throw new ParamNotExistsException("id", "idList", "collectionList", "isAll", "collectionObjList");
        }
        List<Long> pIdList = new ArrayList<>();
        if (id != null) {
            pIdList.add(id);
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                pIdList.add(idList.getLong(i));
            }
        }
        if (CollectionUtils.isNotEmpty(collectionList)) {
            List<String> pCollectionList = collectionList.stream().map(Object::toString).collect(Collectors.toList());
            List<SyncCiCollectionVo> syncCiCollectionList = syncMapper.getInitiativeSyncCiCollectionByCollectNameList(pCollectionList);
            for (SyncCiCollectionVo syncCiCollection : syncCiCollectionList) {
                pIdList.add(syncCiCollection.getId());
            }
        }
        /*
        if (CollectionUtils.isNotEmpty(collectionObjList)) {
            for (int i = 0; i < collectionObjList.size(); i++) {
                JSONObject obj = collectionObjList.getJSONObject(i);
                ObjectVo objectVo = objectMapper.getObjectByCategoryAndType(obj.getString("category"), obj.getString("type"));
                if (objectVo != null && objectVo.getCiId() != null) {
                    syncMapper.getInitiativeSyncCiCollectionByCollectNameAndCiId()
                }
            }
        }*/
        List<SyncCiCollectionVo> syncCiCollectionList = new ArrayList<>();
        if (isAll == null || isAll.equals(0)) {
            syncCiCollectionList = syncMapper.getSyncCiCollectionByIdList(pIdList);
        } else {
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
            CiSyncManager.doSync(syncCiCollectionList, batchTag, startTime);
        }
        return null;
    }

}
