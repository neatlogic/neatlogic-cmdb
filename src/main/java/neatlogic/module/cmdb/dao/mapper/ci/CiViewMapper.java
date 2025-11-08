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

import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.view.ViewConstVo;

import java.util.List;

public interface CiViewMapper {
    List<CiViewVo> getCiViewBaseInfoByCiId(Long ciId);

    List<ViewConstVo> getCiViewConstByCiId(Long ciId);

    List<ViewConstVo> getAllCiViewConstList();

    List<CiViewVo> getCiViewByCiId(CiViewVo ciViewVo);

    int insertCiView(CiViewVo ciViewVo);

    int deleteCiViewByCiId(Long ciId);
}
