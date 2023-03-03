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

package neatlogic.module.cmdb.dao.mapper.cientity;

import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityStatusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CiEntityAlertMapper {
    CiEntityAlertVo getCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    List<CiEntityStatusVo> listCiEntityStatus(@Param("ciEntityIdList") List<Long> ciEntityIdList);

    List<CiEntityAlertVo> searchCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    int searchCiEntityAlertCount(CiEntityAlertVo ciEntityAlertVo);

    void updateCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    void insertCiEntityAlert(CiEntityAlertVo ciEntityAlertVo);

    void deleteCiEntityAlertById(Long id);
}
