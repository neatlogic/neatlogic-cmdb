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

package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.crossover.IRelCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.RelGroupVo;
import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.ci.RelativeRelVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RelMapper extends IRelCrossoverMapper {
    RelTypeVo getRelTypeByName(String name);

    List<RelVo> searchRel(RelVo relVo);

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
