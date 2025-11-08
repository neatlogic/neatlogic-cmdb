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

package neatlogic.module.cmdb.api.mongodb;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCursor;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestQueryMongodbApi extends PrivateApiComponentBase {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getToken() {
        return "/cmdb/mongodb/query";
    }

    @Override
    public String getName() {
        return "mongo测试";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "key", type = ApiParamType.STRING), @Param(name = "value", type = ApiParamType.STRING)})
    @Output({@Param(explode = AttrVo.class)})
    @Description(desc = "mongo测试接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Query query = new Query();
        Criteria criteria = Criteria.where(jsonObj.getString("key")).in(jsonObj.getString("value"));
        query.addCriteria(criteria);
       /* List<JSONObject> result = mongoTemplate.find(query, JSONObject.class, "COLLECT_INS");
        return result.size();*/

        MongoCursor<Document> cursor = mongoTemplate.getCollection("COLLECT_VIRTUALIZED").find(query.getQueryObject()).noCursorTimeout(true).batchSize(1000).cursor();
        int count = 0;
        while (cursor.hasNext()) {
            System.out.println("====================");
            System.out.println(cursor.next().toJson());
            count++;
        }
        cursor.close();
        return count;

    }
}
