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

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppSystemMapper extends IAppSystemMapper {

    AppSystemVo getAppSystemByAbbrName(String abbrName);

    AppSystemVo getAppSystemById(Long id);

    List<AppSystemVo> getAppSystemListByIdList(List<Long> idList);

    AppModuleVo getAppModuleByAbbrName(String abbrName);

    AppModuleVo getAppModuleById(Long id);

    List<AppModuleVo> getAppModuleListByIdList(List<Long> idList);

    List<AppEnvironmentVo> getAppEnvListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList);

}
