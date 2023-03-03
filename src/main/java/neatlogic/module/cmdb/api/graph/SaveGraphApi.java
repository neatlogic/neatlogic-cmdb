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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.GRAPH_MODIFY;
import neatlogic.framework.cmdb.dto.graph.GraphAuthVo;
import neatlogic.framework.cmdb.dto.graph.GraphVo;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.cmdb.enums.graph.GraphType;
import neatlogic.framework.cmdb.exception.graph.GraphNotFoundException;
import neatlogic.framework.cmdb.exception.graph.GraphPrivilegeException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.graph.GraphMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = GRAPH_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveGraphApi extends PrivateApiComponentBase {


    @Resource
    private GraphMapper graphMapper;


    @Override
    public String getToken() {
        return "/cmdb/graph/save";
    }

    @Override
    public String getName() {
        return "保存拓扑视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不存在代表新增"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称", isRequired = true, maxLength = 50, xss = true),
            @Param(name = "type", type = ApiParamType.ENUM, member = GraphType.class, isRequired = true, desc = "类型"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "说明", maxLength = 500, xss = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活", isRequired = true),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "图形配置", isRequired = true)})
    @Output({@Param(explode = GraphVo.class)})
    @Description(desc = "保存拓扑视图接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        String type = jsonObj.getString("type");
        if (type.equals(GraphType.PUBLIC.getValue())) {
            if (!AuthActionChecker.check(GRAPH_MODIFY.class)) {
                throw new GraphPrivilegeException(GraphPrivilegeException.Action.SAVE);
            }
        }
        GraphVo graphVo = JSONObject.toJavaObject(jsonObj, GraphVo.class);
        if (id != null) {
            GraphVo oldGraphVo = graphMapper.getGraphById(id);
            if (oldGraphVo == null) {
                throw new GraphNotFoundException(id);
            }
            if (oldGraphVo.getType().equals(CustomViewType.PRIVATE.getValue())) {
                if (!oldGraphVo.getFcu().equalsIgnoreCase(UserContext.get().getUserUuid(true))) {
                    throw new GraphPrivilegeException(GraphPrivilegeException.Action.SAVE);
                }
            }
            graphVo.setLcu(UserContext.get().getUserUuid(true));
            graphMapper.deleteGraphRelByFromGraphId(graphVo.getId());
            graphMapper.deleteGraphCiEntityByGraphId(graphVo.getId());
            graphMapper.deleteGraphAuthByGraphId(graphVo.getId());
            graphMapper.updateGraph(graphVo);
        } else {
            graphVo.setFcu(UserContext.get().getUserUuid(true));
            graphMapper.insertGraph(graphVo);
        }
        if (CollectionUtils.isNotEmpty(graphVo.getGraphAuthList())) {
            for (GraphAuthVo authVo : graphVo.getGraphAuthList()) {
                authVo.setGraphId(graphVo.getId());
                graphMapper.insertGraphAuth(authVo);
            }
        }
        if (MapUtils.isNotEmpty(graphVo.getConfig())) {
            JSONArray nodeObjList = graphVo.getConfig().getJSONObject("topo").getJSONArray("nodes");
            if (CollectionUtils.isNotEmpty(nodeObjList)) {
                for (int i = 0; i < nodeObjList.size(); i++) {
                    JSONObject nodeObj = nodeObjList.getJSONObject(i);
                    String objType = nodeObj.getString("type");
                    Long objId = nodeObj.getJSONObject("config").getLong("id");
                    if (objType.equalsIgnoreCase("graph")) {
                        graphMapper.insertGraphRel(graphVo.getId(), objId);
                    } else if (objType.equalsIgnoreCase("cientity")) {
                        graphMapper.insertGraphCiEntity(graphVo.getId(), objId);
                    }
                }
            }
        }
        return graphVo;
    }
}
