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
