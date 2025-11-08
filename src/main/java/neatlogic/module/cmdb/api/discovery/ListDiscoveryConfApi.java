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

package neatlogic.module.cmdb.api.discovery;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
