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

    void deleteCiEntityAlertByCiEntityIdAndLevelList(@Param("ciEntityId") Long ciEntityId, @Param("levelList") List<Integer> levelList);
}
