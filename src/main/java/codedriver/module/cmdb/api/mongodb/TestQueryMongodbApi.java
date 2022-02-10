/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.mongodb;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
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
