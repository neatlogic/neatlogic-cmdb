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
import neatlogic.framework.cmdb.dto.cientity.CiEntityStatusVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.graph.GraphMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAllCiEntityIdByGraphId extends PrivateApiComponentBase {


    @Resource
    private GraphMapper graphMapper;

    @Override
    public String getToken() {
        return "/cmdb/graph/getallcientityid";
    }

    @Override
    public String getName() {
        return "递归获取视图所有配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "graphIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "视图id列表"),})
    @Output({@Param(explode = CiEntityStatusVo[].class)})
    @Description(desc = "递归获取视图所有配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Map<Long, Set<Long>> checkGraphMap = new HashMap<>();
        Map<String, Set<Long>> graphCiEntityMap = new HashMap<>();
        List<Long> graphIdList = new ArrayList<>();
        for (int i = 0; i < jsonObj.getJSONArray("graphIdList").size(); i++) {
            Long graphId = jsonObj.getJSONArray("graphIdList").getLong(i);
            if (!checkGraphMap.containsKey(graphId)) {
                graphIdList.add(graphId);
                checkGraphMap.put(graphId, new HashSet<Long>() {{
                    //先将自己加进去，避免循环
                    this.add(graphId);
                }});
                graphCiEntityMap.put("graph_" + graphId, new HashSet<>());
            }
        }
        for (Long graphId : graphIdList) {
            List<Long> toGraphIdList = graphMapper.getToGraphIdByFromGraphId(graphId);
            graphCiEntityMap.get("graph_" + graphId).addAll(graphMapper.getCiEntityIdByGraphId(graphId));
            while (CollectionUtils.isNotEmpty(toGraphIdList)) {
                //清除已经存在的视图id
                toGraphIdList.removeAll(checkGraphMap.get(graphId));
                if (CollectionUtils.isNotEmpty(toGraphIdList)) {
                    graphCiEntityMap.get("graph_" + graphId).addAll(graphMapper.getCiEntityIdByGraphIdList(toGraphIdList));
                    toGraphIdList = graphMapper.getToGraphIdByFromGraphIdList(toGraphIdList);
                }
            }
        }

        return graphCiEntityMap;
    }


}
