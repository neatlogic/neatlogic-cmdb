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

import neatlogic.framework.cmdb.dto.ci.RelTypeVo;

import java.util.List;

public interface RelTypeMapper {
    int checkRelTypeIsInUsed(Long relTypId);

    int checkRelTypeNameIsExists(RelTypeVo relTypeVo);

    RelTypeVo getRelTypeById(Long id);

    RelTypeVo getRelTypeByName(String name);

    List<RelTypeVo> getAllRelType();

    int insertRelType(RelTypeVo relTypeVo);

    int updateRelType(RelTypeVo relTypeVo);

    int updateRelTypeIsShow(RelTypeVo relTypeVo);

    int deleteRelTypeById(Long relTypId);
}
