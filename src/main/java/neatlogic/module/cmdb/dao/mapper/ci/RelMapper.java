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

    int updateRel(RelVo relVo);

    int updateRelGroup(RelGroupVo relGroupVo);

    int insertRel(RelVo relVo);

    int insertRelGroup(RelGroupVo relGroupVo);

    void insertRelativeRel(RelativeRelVo relativeRelVo);

    int deleteRelById(Long relId);

    void deleteRelativeRelByRelId(Long relId);

}
