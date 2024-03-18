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

package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.crossover.IRelCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.RelGroupVo;
import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.ci.RelativeRelVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RelMapper extends IRelCrossoverMapper {
    RelVo getRelByCiIdAndRelName(@Param("ciId") Long ciId, @Param("relName") String relName);

    RelTypeVo getRelTypeByRelId(Long relId);

    List<RelativeRelVo> getRelativeRelByRelId(Long relId);

    List<RelVo> getAllRelList();

    List<RelGroupVo> getRelGroupByCiId(Long ciId);

    int checkRelGroupNameIsExists(RelGroupVo relGroupVo);

    int checkRelByFromToName(RelVo relVo);

    int checkRelByFromToLabel(RelVo relVo);

    RelVo getRelById(Long id);

    List<RelVo> getRelByIdList(@Param("relIdList") List<Long> relIdList);

    RelGroupVo getRelGroupById(Long relGroupId);

    List<RelVo> getRelByCiId(Long ciId);

    List<RelVo> getRelBaseInfoByCiId(Long ciId);

    int updateRel(RelVo relVo);

    int updateRelGroup(RelGroupVo relGroupVo);

    int insertRel(RelVo relVo);

    int insertRelGroup(RelGroupVo relGroupVo);

    void insertRelativeRel(RelativeRelVo relativeRelVo);

    int deleteRelById(Long relId);

    void deleteRelativeRelByRelId(Long relId);

    void deleteRelGroupByCiId(Long ciId);
}
