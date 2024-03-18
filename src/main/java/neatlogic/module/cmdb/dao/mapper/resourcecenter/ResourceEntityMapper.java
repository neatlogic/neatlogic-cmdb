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

import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ResourceEntityMapper extends IResourceEntityCrossoverMapper {
    ResourceEntityVo getResourceEntityByName(String name);

    List<ResourceEntityVo> getResourceEntityListByNameList(List<String> nameList);

    String getResourceEntityConfigByName(String name);

    List<Long> getAllResourceTypeCiIdList(ResourceSearchVo searchVo);

    int getResourceEntityViewDataCount(String name);

    List<Map<String, Object>> getResourceEntityViewDataList(@Param("name") String name, @Param("startNum") int startNum, @Param("pageSize") int pageSize);

    void insertResourceEntity(ResourceEntityVo resourceEntityVo);

    void insertResourceTypeCi(Long ciId);

    void updateResourceEntity(ResourceEntityVo resourceEntityVo);

    void updateResourceEntityStatusAndError(ResourceEntityVo resourceEntityVo);

    void updateResourceEntityLabelAndDescription(ResourceEntityVo resourceEntityVo);

    void deleteResourceEntityByName(String name);

    void deleteResourceTypeCi();
}
