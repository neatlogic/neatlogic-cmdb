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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.service.sync.SyncService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchSyncCiCollectionApi extends PrivateApiComponentBase {

    @Resource
    private SyncService syncService;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/search";
    }

    @Override
    public String getName() {
        return "nmcas.searchsynccicollectionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确刷新状态"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "isShowPhysicalType", type = ApiParamType.INTEGER, desc = "term.cmdb.isshowpsycollectioni"),
            @Param(name = "collectMode", type = ApiParamType.ENUM, rule = "initiative,passive", desc = "term.cmdb.collectmode"),
            @Param(name = "collectionType", type = ApiParamType.STRING, desc = "term.cmdb.collectiontype"),
            @Param(name = "collectionName", type = ApiParamType.STRING, desc = "term.cmdb.collectionname")})
    @Output({@Param(explode = BasePageVo.class)})
    @Description(desc = "nmcas.searchsynccicollectionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        SyncCiCollectionVo syncCiCollectionVo = JSONObject.toJavaObject(jsonObj, SyncCiCollectionVo.class);
        if (StringUtils.isNotBlank(jsonObj.getString("collectionType")) && StringUtils.isBlank(jsonObj.getString("collectionName"))) {
            Query query = new Query();
            query.addCriteria(Criteria.where("collection").is(jsonObj.getString("collectionType")));
            List<CollectionVo> collectionList = mongoTemplate.find(query, CollectionVo.class, "_dictionary");
            List<String> collectionNameList = new ArrayList<>();
            for (CollectionVo collectionVo : collectionList) {
                collectionNameList.add(collectionVo.getName());
            }
            syncCiCollectionVo.setCollectionNameList(collectionNameList);
        } else if (StringUtils.isNotBlank(jsonObj.getString("collectionName"))) {
            if (jsonObj.getIntValue("isShowPhysicalType") == 1) {
                syncCiCollectionVo.setCollectionName("");//清空集合名，改用collectionNameList属性进行检索
                List<String> collectionNameList = new ArrayList<>();
                collectionNameList.add(jsonObj.getString("collectionName"));
                Query query = new Query();
                query.addCriteria(Criteria.where("name").is(jsonObj.getString("collectionName")));
                CollectionVo collection = mongoTemplate.findOne(query, CollectionVo.class, "_dictionary");
                if (collection != null && StringUtils.isNotBlank(collection.getCollection())) {
                    collectionNameList.add(collection.getCollection().toLowerCase().substring(8));//去掉collect_前缀
                }
                syncCiCollectionVo.setCollectionNameList(collectionNameList);
            }
        }
        List<SyncCiCollectionVo> syncCiCollectionList = syncService.searchSyncCiCollection(syncCiCollectionVo);
        return TableResultUtil.getResult(syncCiCollectionList, syncCiCollectionVo);
    }


}
