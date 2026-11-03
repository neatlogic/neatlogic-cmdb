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

package neatlogic.module.cmdb.dao.mapper.cientity;

import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityStatusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiEntityAlertMapper {
    /** 按源端 UUID 原子写入，重复消息只更新同一来源的记录。 */
    void upsertSourceCiEntityAlert(CiEntityAlertVo alert);

    /** 精确消除来源告警，不依赖当前配置项关联。 */
    void deleteSourceCiEntityAlert(@Param("source") String source, @Param("alertUuid") String alertUuid);

    CiEntityAlertVo getCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    List<CiEntityStatusVo> listCiEntityStatus(@Param("ciEntityIdList") List<Long> ciEntityIdList);

    List<CiEntityAlertVo> searchCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    int searchCiEntityAlertCount(CiEntityAlertVo ciEntityAlertVo);

    void updateCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    void insertCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    void deleteCiEntityAlertById(Long id);

    void deleteCiEntityAlertByCiEntityIdAndLevelList(@Param("ciEntityId") Long ciEntityId, @Param("levelList") List<Integer> levelList);
}
