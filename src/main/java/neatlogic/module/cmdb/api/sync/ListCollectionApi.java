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
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCollectionApi extends PrivateApiComponentBase {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取集合列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "needCount", type = ApiParamType.BOOLEAN, desc = "是否需要返回集合数据"),
            @Param(name = "collectionName", type = ApiParamType.STRING, desc = "集合名称"),
            @Param(name = "isNeedPhysicalType", type = ApiParamType.INTEGER, desc = "是否返回物理集合")})
    @Output({@Param(explode = CollectionVo[].class)})
    @Description(desc = "获取集合列表接口，需要依赖mongodb")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Query query = new Query();
        if (StringUtils.isNotBlank(paramObj.getString("collectionName"))) {
            query.addCriteria(Criteria.where("name").is(paramObj.getString("collectionName")));
        }
        List<CollectionVo> collectionList = mongoTemplate.find(query, CollectionVo.class, "_dictionary");
        if (StringUtils.isNotBlank(paramObj.getString("collectionName")) && CollectionUtils.isNotEmpty(collectionList) && paramObj.getIntValue("isNeedPhysicalType") == 1) {
            query = new Query();
            List<String> phyCollectionList = new ArrayList<>();
            for (CollectionVo collectionVo : collectionList) {
                phyCollectionList.add(collectionVo.getCollection().toLowerCase().substring(8));
            }
            query.addCriteria(Criteria.where("name").in(phyCollectionList));
            collectionList.addAll(mongoTemplate.find(query, CollectionVo.class, "_dictionary"));
        }
        collectionList = collectionList.stream().distinct().collect(Collectors.toList());
        if (paramObj.getBooleanValue("needCount")) {
            if (CollectionUtils.isNotEmpty(collectionList)) {
                for (CollectionVo collectionVo : collectionList) {
                    long dataCount = mongoTemplate.count(new Query(collectionVo.getFilterCriteria()), collectionVo.getCollection());
                    collectionVo.setDataCount(dataCount);
                }
            }
        }
        return collectionList;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/collection/list";
    }
}
