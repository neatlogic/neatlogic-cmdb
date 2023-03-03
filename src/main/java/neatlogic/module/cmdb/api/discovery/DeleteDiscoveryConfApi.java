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

package neatlogic.module.cmdb.api.discovery;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
import com.alibaba.fastjson.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDiscoveryConfApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private DiscoveryMapper discoveryMapper;

    @Override
    public String getToken() {
        return "/cmdb/discovery/conf/delete";
    }

    @Override
    public String getName() {
        return "删除自动发现配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "_id", type = ApiParamType.STRING, desc = "MongoDB文档id", isRequired = true),
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)})
    @Description(desc = "删除自动发现配置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        jsonObj.put("_id", new ObjectId(jsonObj.getString("_id")));
        mongoTemplate.remove(jsonObj, "_discovery_conf");
        discoveryMapper.deleteDiscoveryConfCombopByConfId(jsonObj.getLong("id"));
        return null;
    }
}
