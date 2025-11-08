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

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ApplicationListDisplayVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AssetListDisplayVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ResourceEntityMapper extends IResourceEntityCrossoverMapper {
    ResourceEntityVo getResourceEntityByName(String name);

    List<ResourceEntityVo> getResourceEntityListByNameList(List<String> nameList);

    List<ResourceEntityVo> getResourceEntityList();

    String getResourceEntityConfigByName(String name);

    List<Long> getAllResourceTypeCiIdList(ResourceSearchVo searchVo);

    int getResourceEntityViewDataCount(String name);

    List<Map<String, Object>> getResourceEntityViewDataList(@Param("name") String name, @Param("startNum") int startNum, @Param("pageSize") int pageSize);

    ApplicationListDisplayVo getApplicationListDisplay();

    AssetListDisplayVo getAssetListDisplay();

    void insertResourceEntity(ResourceEntityVo resourceEntityVo);

    void insertResourceTypeCi(Long ciId);

    int insertApplicationListDisplay(ApplicationListDisplayVo applicationListDisplayVo);

    int insertAssetListDisplay(AssetListDisplayVo assetListDisplayVo);

    void updateResourceEntity(ResourceEntityVo resourceEntityVo);

    void updateResourceEntityStatusAndError(ResourceEntityVo resourceEntityVo);

    void updateResourceEntityLabelAndDescription(ResourceEntityVo resourceEntityVo);

    void deleteResourceEntityByName(String name);

    void deleteResourceTypeCi();
}
