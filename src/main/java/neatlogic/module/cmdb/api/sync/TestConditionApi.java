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
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncConditionVo;
import neatlogic.framework.cmdb.dto.sync.SyncPolicyVo;
import neatlogic.framework.cmdb.exception.sync.CollectionNotFoundException;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestConditionApi extends PrivateApiComponentBase {
    @Resource
    private SyncMapper syncMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "测试采集策略搜索条件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "collectionId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.syncmappingid"),
            @Param(name = "conditionList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.ciid")})
    @Description(desc = "测试采集策略搜索条件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long collectionId = paramObj.getLong("collectionId");
        JSONArray conditionObjList = paramObj.getJSONArray("conditionList");
        List<SyncConditionVo> conditionList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(conditionObjList)) {
            for (int i = 0; i < conditionObjList.size(); i++) {
                SyncConditionVo syncConditionVo = JSONObject.toJavaObject(conditionObjList.getJSONObject(i), SyncConditionVo.class);
                conditionList.add(syncConditionVo);
            }
        }
        SyncCiCollectionVo syncCiCollectionVo = syncMapper.getSyncCiCollectionById(collectionId);
        if (syncCiCollectionVo == null) {
            throw new SyncCiCollectionNotFoundException(collectionId);
        }
        CollectionVo collectionVo = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("name").is(syncCiCollectionVo.getCollectionName())), CollectionVo.class, "_dictionary");
        if (collectionVo == null) {
            throw new CollectionNotFoundException(syncCiCollectionVo.getCollectionName());
        }
        SyncPolicyVo syncPolicyVo = new SyncPolicyVo();
        syncPolicyVo.setConditionList(conditionList);
        int pageSize = 100;
        Query query = new Query();
        Criteria finalCriteria = new Criteria();
        finalCriteria.andOperator(collectionVo.getFilterCriteria(), syncPolicyVo.getCriteria());
        query.addCriteria(finalCriteria);
        query.limit(pageSize);
        return mongoTemplate.find(query, JSONObject.class, collectionVo.getCollection()).size();
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/policy/condition/test";
    }
}
