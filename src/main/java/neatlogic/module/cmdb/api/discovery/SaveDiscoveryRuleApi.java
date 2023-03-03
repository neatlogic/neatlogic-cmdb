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
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDiscoveryRuleApi extends PrivateApiComponentBase {

    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public String getToken() {
        return "/cmdb/discovery/rule/save";
    }

    @Override
    public String getName() {
        return "保存自动发现规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "_id", type = ApiParamType.STRING,
            desc = "id，存在代表修改，不存在代表新增"),
            @Param(name = "sysObjectId", type = ApiParamType.STRING, isRequired = true, desc = "目标oid"),
            @Param(name = "sysDescrPattern", type = ApiParamType.STRING, desc = "匹配规则"),
            @Param(name = "_OBJ_CATEGORY", type = ApiParamType.STRING, isRequired = true, desc = "对象大类"), @Param(name = "_OBJ_TYPE", type = ApiParamType.STRING, isRequired = true, desc = "对象分类"), @Param(name = "VENDOR", type = ApiParamType.STRING, desc = "厂商"), @Param(name = "MODEL", type = ApiParamType.STRING, desc = "型号")})
    @Description(desc = "保存自动发现规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String[] fieldList = new String[]{"_id", "sysObjectId", "sysDescrPattern", "_OBJ_CATEGORY", "_OBJ_TYPE", "VENDOR", "MODEL"};
        //由于前端可能会送一些没用的字段进来，而mongodb是直接保存json，所以需要先删除没用的字段
        jsonObj.keySet().removeIf(key -> Arrays.stream(fieldList).noneMatch(d -> d.equalsIgnoreCase(key)));
        if (StringUtils.isNotBlank(jsonObj.getString("_id"))) {
            jsonObj.put("_id", new ObjectId(jsonObj.getString("_id")));
        }
        mongoTemplate.save(jsonObj, "_discovery_rule");
        return null;
    }
}
