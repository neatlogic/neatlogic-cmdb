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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.graph.GraphRelVo;
import neatlogic.framework.cmdb.dto.graph.GraphVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.graphviz.Graphviz;
import neatlogic.framework.graphviz.Link;
import neatlogic.framework.graphviz.Node;
import neatlogic.framework.graphviz.enums.LayoutType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.graph.GraphMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetGraphTopoApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(GetGraphTopoApi.class);

    @Resource
    private GraphMapper graphMapper;

    @Override
    public String getToken() {
        return "/cmdb/graph/topo";
    }

    @Override
    public String getName() {
        return "获取视图关系拓扑";
    }

    @Override
    public String getConfig() {
        return null;
    }


    private void getAllRelGraphId(List<Long> graphIdList, Set<Long> checkGraphSet) {
        List<Long> fromGraphIdList = graphMapper.getFromGraphIdByToGraphIdList(graphIdList);
        List<Long> toGraphIdList = graphMapper.getToGraphIdByFromGraphIdList(graphIdList);
        fromGraphIdList.removeAll(checkGraphSet);
        toGraphIdList.removeAll(checkGraphSet);
        List<Long> allIdList = new ArrayList<>();
        allIdList.addAll(fromGraphIdList);
        allIdList.addAll(toGraphIdList);
        if (CollectionUtils.isNotEmpty(allIdList)) {
            checkGraphSet.addAll(allIdList);
            getAllRelGraphId(allIdList, checkGraphSet);
        }
    }

    @Input({@Param(name = "layout", type = ApiParamType.ENUM, member = LayoutType.class, isRequired = true), @Param(name = "graphId", type = ApiParamType.LONG, isRequired = true, desc = "视图id")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取视图关系拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String layout = jsonObj.getString("layout");
        Long graphId = jsonObj.getLong("graphId");
        Set<Long> graphIdSet = new HashSet<>();
        graphIdSet.add(graphId);
        getAllRelGraphId(new ArrayList<Long>() {{
            this.add(graphId);
        }}, graphIdSet);
        List<GraphVo> graphList = graphMapper.getGraphByIdList(new ArrayList<>(graphIdSet));
        Graphviz.Builder gb = new Graphviz.Builder(LayoutType.get(layout));
        for (GraphVo graphVo : graphList) {
            Node.Builder nb = new Node.Builder("Graph_" + graphVo.getId());
            nb.withTooltip(graphVo.getName());
            nb.withLabel(graphVo.getName());
            nb.withImage(graphVo.getIcon());
            nb.addClass("graphnode").addClass("normalnode");
            if (graphVo.getId().equals(graphId)) {
                nb.addClass("corenode");
                nb.withFontColor("red");
            }
            gb.addNode(nb.build());
        }
        List<GraphRelVo> graphRelList = graphMapper.getGraphRelByIdList(new ArrayList<>(graphIdSet));
        //为继承关系添加关联
        if (CollectionUtils.isNotEmpty(graphRelList)) {
            for (GraphRelVo relVo : graphRelList) {
                Link.Builder lb = new Link.Builder("Graph_" + relVo.getFromGraphId(), "Graph_" + relVo.getToGraphId());
                gb.addLink(lb.build());
            }
        }
        String dot = gb.build().toString();
        if (logger.isDebugEnabled()) {
            logger.debug(dot);
        }
        return dot;
    }

}
