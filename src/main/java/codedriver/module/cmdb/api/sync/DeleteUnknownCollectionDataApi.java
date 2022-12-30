/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.sync;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.SYNC_MODIFY;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteUnknownCollectionDataApi extends PrivateApiComponentBase {
    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public String getName() {
        return "删除未知设备数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "_id", type = ApiParamType.STRING, isRequired = true, desc = "数据id")})
    @Description(desc = "删除未知设备数据接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        paramObj.put("_id", new ObjectId(paramObj.getString("_id")));
        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is("unknown")), CollectionVo.class, "_dictionary");
        if (collectionVo != null) {
            mongoTemplate.remove(paramObj, collectionVo.getCollection());
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/collection/unknown/delete";
    }
}
