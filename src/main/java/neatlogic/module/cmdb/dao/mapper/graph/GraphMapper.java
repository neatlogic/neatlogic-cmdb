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

package neatlogic.module.cmdb.dao.mapper.graph;

import neatlogic.framework.cmdb.dto.graph.GraphAuthVo;
import neatlogic.framework.cmdb.dto.graph.GraphRelVo;
import neatlogic.framework.cmdb.dto.graph.GraphVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GraphMapper {
    GraphVo getGraphById(Long id);

    List<GraphRelVo> getGraphRelByIdList(@Param("graphIdList") List<Long> graphIdList);

    List<GraphVo> getGraphByIdList(@Param("graphIdList") List<Long> graphIdList);

    List<GraphVo> searchGraph(GraphVo graphVo);

    List<GraphVo> getGraphListByToGraphId(Long toGraphId);

    List<Long> getCiEntityIdByGraphId(Long graphId);

    List<Long> getCiEntityIdByGraphIdList(@Param("graphIdList") List<Long> graphIdList);

    List<Long> getToGraphIdByFromGraphId(Long fromGraphId);

    List<Long> getFromGraphIdByToGraphId(Long toGraphId);

    List<Long> getToGraphIdByFromGraphIdList(@Param("graphIdList") List<Long> graphIdList);

    List<Long> getFromGraphIdByToGraphIdList(@Param("graphIdList") List<Long> graphIdList);

    int searchGraphCount(GraphVo graphVo);

    int checkGraphIsInvoked(Long graphId);

    void updateGraph(GraphVo graphVo);

    void insertGraph(GraphVo graphVo);

    void insertGraphAuth(GraphAuthVo graphAuthVo);

    void insertGraphRel(@Param("fromGraphId") Long fromGraphId, @Param("toGraphId") Long toGraphId);

    void insertGraphCiEntity(@Param("graphId") Long graphId, @Param("ciEntityId") Long ciEntityId);

    void deleteGraphAuthByGraphId(Long graphId);

    void deleteGraphCiEntityByGraphId(Long graphId);

    void deleteGraphRelByFromGraphId(Long graphId);

    void deleteGraphById(Long graphId);
}
