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

package neatlogic.module.cmdb.api.mongodb;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCursor;
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
