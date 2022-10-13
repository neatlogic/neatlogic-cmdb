/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.discovery;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.SYNC_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDiscoveryRuleApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public String getToken() {
        return "/cmdb/discovery/rule/delete";
    }

    @Override
    public String getName() {
        return "删除自动发现规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "_id", type = ApiParamType.STRING,
            desc = "id", isRequired = true)})
    @Description(desc = "删除自动发现规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        jsonObj.put("_id", new ObjectId(jsonObj.getString("_id")));
        mongoTemplate.remove(jsonObj, "_discovery_rule");
        return null;
    }
}
