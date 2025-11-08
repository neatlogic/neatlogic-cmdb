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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.graph.GraphMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetInvokeGraphApi extends PrivateApiComponentBase {


    @Resource
    private GraphMapper graphMapper;


    @Override
    public String getToken() {
        return "/cmdb/graph/listinvoke";
    }

    @Override
    public String getName() {
        return "获取引用拓扑视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "toGraphId", type = ApiParamType.LONG, isRequired = true, desc = "被引用视图id")})
    @Description(desc = "获取引用拓扑视图接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return graphMapper.getGraphListByToGraphId(jsonObj.getLong("toGraphId"));
    }
}
