/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.resourcecenter.appmoduleresource.handler;

import neatlogic.framework.cmdb.dto.resourcecenter.AppEnvVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.resourcecenter.appmoduleresource.core.IAppModuleResource;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class AppInsClusterHandler implements IAppModuleResource {

    @Resource
    ResourceMapper resourceMapper;

    @Override
    public String getName() {
        return "APPInsCluster";
    }

    @Override
    public List<ResourceVo> getResourceList(ResourceSearchVo searchVo) {
        int rowNum = resourceMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceMapper.getResourceListByIdList(idList);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<AppEnvVo> getAppEnvList(ResourceSearchVo searchVo) {
        return resourceMapper.getIpObjectEnvListByAppSystemIdAndTypeId(searchVo);
    }
}
