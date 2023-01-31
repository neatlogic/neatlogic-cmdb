/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.discovery;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDiscoveryConfApi extends PrivateApiComponentBase {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "获取自动发现配置列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "配置列表")})
    @Description(desc = "获取自动发现配置列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return mongoTemplate.find(new Query(), JSONObject.class, "_discovery_conf");
    }

    @Override
    public String getToken() {
        return "/cmdb/discovery/conf/list";
    }
}
