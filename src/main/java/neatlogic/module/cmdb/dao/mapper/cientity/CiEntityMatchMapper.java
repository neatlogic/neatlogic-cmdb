/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the Sustainable Use License (SUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.dao.mapper.cientity;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.match.CiEntityMatchCandidateVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 仅接受经过严格元数据校验的模型及属性条件。 */
public interface CiEntityMatchMapper {
    /** 不计算总数，不装配属性关系，最多读取两个不同配置项。 */
    List<CiEntityMatchCandidateVo> match(@Param("ci") CiVo ci,
                                      @Param("conditions") List<AttrFilterVo> conditions);
}
