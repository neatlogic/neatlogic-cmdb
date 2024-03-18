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
