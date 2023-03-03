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

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
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

    List<AppEnvironmentVo> getAppEnvListByAppSystemIdAndModuleIdList(@Param("appResourceId") Long appResourceId, @Param("moduleResourceIdList") List<Long> moduleIdList);

}
