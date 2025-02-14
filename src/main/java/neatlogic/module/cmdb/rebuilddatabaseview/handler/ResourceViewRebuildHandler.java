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

package neatlogic.module.cmdb.rebuilddatabaseview.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ResourceViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private SchemaMapper schemaMapper;

    @Override
    public String getDescription() {
        return "重建资源中心视图";
    }

    @Override
    public List<ViewStatusInfo> createViewIfNotExists() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), sceneEntityVo.getName());
            if (Objects.equals(tableType, "VIEW")) {
                continue;
            }
            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(sceneEntityVo);
            resultList.add(viewStatusInfo);
        }
        return resultList;
    }

    @Override
    public List<ViewStatusInfo> createOrReplaceView() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(sceneEntityVo);
            resultList.add(viewStatusInfo);
        }
        return resultList;
    }

    private ViewStatusInfo rebuildSceneEntity(SceneEntityVo sceneEntityVo) {
        ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
        resourceEntityVo.setName(sceneEntityVo.getName());
        resourceEntityVo.setLabel(sceneEntityVo.getLabel());
        String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
        if (StringUtils.isNotBlank(config)) {
            resourceEntityVo.setConfigStr(config);
            String error = resourceCenterResourceService.buildResourceView(resourceEntityVo.getName(), resourceEntityVo.getConfig());
            resourceEntityVo.setError(error);
            if (StringUtils.isNotBlank(error)) {
                resourceEntityVo.setStatus(Status.ERROR.getValue());
            } else {
                resourceEntityVo.setStatus(Status.READY.getValue());
            }
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
        } else {
            resourceEntityVo.setStatus(Status.PENDING.getValue());
        }
        ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
        viewStatusInfo.setName(resourceEntityVo.getName());
        viewStatusInfo.setLabel(resourceEntityVo.getLabel());
        viewStatusInfo.setError(resourceEntityVo.getError());
        if (Objects.equals(resourceEntityVo.getStatus(), Status.ERROR.getValue())) {
            viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
        } else {
            viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
        }
        return viewStatusInfo;
    }

    @Override
    public int getSort() {
        return 4;
    }
}
