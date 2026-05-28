/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.cmdb.service.resourcecenter;

import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceEntityCrossoverService;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AssetListDisplayVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ResourceEntityServiceImpl implements ResourceEntityService, IResourceEntityCrossoverService {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public CiVo getAssetListRootCi() {
        AssetListDisplayVo assetListDisplay = resourceEntityMapper.getAssetListDisplay();
        if (assetListDisplay != null && StringUtils.isNotBlank(assetListDisplay.getRootCiName())) {
            ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
            return ciCrossoverMapper.getCiByName(assetListDisplay.getRootCiName());
        }
        return null;
    }

    @Override
    public CiVo getViewRootCi(String viewName) {
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(viewName);
        if (resourceEntityVo != null) {
            ResourceEntityConfigVo config = resourceEntityVo.getConfig();
            if (config != null) {
                if (StringUtils.isNotBlank(config.getMainCi())) {
                    ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
                    return ciCrossoverMapper.getCiByName(config.getMainCi());
                }
            }
        }
        return null;
    }

    @Override
    public ResourceEntityVo getResourceEntityByName(String viewName) {
        return resourceEntityMapper.getResourceEntityByName(viewName);
    }
}
