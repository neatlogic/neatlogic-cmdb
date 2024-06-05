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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.sync.ObjectVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncConditionVo;
import neatlogic.framework.cmdb.enums.sync.CollectMode;
import neatlogic.framework.cmdb.exception.collection.CollectionObjectNotFoundException;
import neatlogic.framework.cmdb.exception.sync.NoSyncToRunException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.ObjectMapper;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.sync.CiSyncManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
            @Param(name = "conditionList", type = ApiParamType.JSONARRAY, desc = "条件列表"),
            @Param(name = "collectionList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.collectionlist"),
            @Param(name = "collectionObjList", type = ApiParamType.JSONARRAY, help = "需要包含category和type属性", desc = "term.cmdb.collectionobjlist"),
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
        JSONArray conditionList = jsonObj.getJSONArray("conditionList");
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

        if (CollectionUtils.isNotEmpty(collectionObjList)) {
            for (int i = 0; i < collectionObjList.size(); i++) {
                JSONObject obj = collectionObjList.getJSONObject(i);
                String category = obj.getString("category");
                String type = obj.getString("type");
                if (StringUtils.isNotBlank(category) && StringUtils.isNotBlank(type)) {
                    ObjectVo objectVo = objectMapper.getObjectByCategoryAndType(obj.getString("category"), obj.getString("type"));
                    if (objectVo != null && objectVo.getCiId() != null) {
                        List<SyncCiCollectionVo> syncCiCollectionList = syncMapper.getInitiativeSyncCiCollectionByCollectNameAndCiId(obj.getString("type"), objectVo.getCiId());
                        for (SyncCiCollectionVo syncCiCollection : syncCiCollectionList) {
                            pIdList.add(syncCiCollection.getId());
                        }
                    } else {
                        throw new CollectionObjectNotFoundException(category, type);
                    }
                }
            }
        }
        List<SyncCiCollectionVo> syncCiCollectionList = new ArrayList<>();
        if ((isAll == null || isAll.equals(0)) && CollectionUtils.isNotEmpty(pIdList)) {
            syncCiCollectionList = syncMapper.getSyncCiCollectionByIdList(pIdList);
        } else if (Objects.equals(isAll, 1)) {
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
            if (syncCiCollectionList.size() == 1 && CollectionUtils.isNotEmpty(conditionList)) {
                List<SyncConditionVo> cList = new ArrayList<>();
                for (int i = 0; i < conditionList.size(); i++) {
                    cList.add(JSON.toJavaObject(conditionList.getJSONObject(i), SyncConditionVo.class));
                }
                syncCiCollectionList.get(0).setConditionList(cList);
            }
            CiSyncManager.doSync(syncCiCollectionList, batchTag, startTime);
        } else {
            throw new NoSyncToRunException();
        }
        return null;
    }
}
