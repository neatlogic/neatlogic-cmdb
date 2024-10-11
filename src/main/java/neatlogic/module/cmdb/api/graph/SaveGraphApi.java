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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSON;
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
import neatlogic.framework.cmdb.exception.graph.GraphPrivilegeSaveException;
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
    @Description(desc = "保存拓扑视图")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        String type = jsonObj.getString("type");
        if (type.equals(GraphType.PUBLIC.getValue())) {
            if (!AuthActionChecker.check(GRAPH_MODIFY.class)) {
                throw new GraphPrivilegeSaveException();
            }
        }
        GraphVo graphVo = JSON.toJavaObject(jsonObj, GraphVo.class);
        if (id != null) {
            GraphVo oldGraphVo = graphMapper.getGraphById(id);
            if (oldGraphVo == null) {
                throw new GraphNotFoundException(id);
            }
            if (oldGraphVo.getType().equals(CustomViewType.PRIVATE.getValue())) {
                if (!oldGraphVo.getFcu().equalsIgnoreCase(UserContext.get().getUserUuid(true))) {
                    throw new GraphPrivilegeSaveException();
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
            JSONArray nodeObjList = graphVo.getConfig().getJSONObject("topo").getJSONArray("cells");
            if (CollectionUtils.isNotEmpty(nodeObjList)) {
                for (int i = 0; i < nodeObjList.size(); i++) {
                    JSONObject nodeObj = nodeObjList.getJSONObject(i);
                    String objType = nodeObj.getString("shape");
                    Long objId = nodeObj.getLong("id");
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
