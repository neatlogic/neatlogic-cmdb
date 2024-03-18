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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCollectionApi extends PrivateApiComponentBase {
    @Resource
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
