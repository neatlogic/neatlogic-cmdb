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

package neatlogic.module.cmdb.api.resourcecenter.config;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/11/9 11:28
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceEntityApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/list";
    }

    @Override
    public String getName() {
        return "nmcarc.listresourceentityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
            @Param(name = "Return", explode = ResourceEntityVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmcarc.listresourceentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ResourceEntityVo> resultList = new ArrayList<>();
        List<String> viewNameList = ResourceEntityFactory.getViewNameList();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityListByNameList(viewNameList);
        Map<String, ResourceEntityVo> resourceEntityVoMap = resourceEntityList.stream().collect(Collectors.toMap(ResourceEntityVo::getName, e -> e));
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            ResourceEntityVo resourceEntityVo = resourceEntityVoMap.get(sceneEntityVo.getName());
            if (resourceEntityVo == null) {
                resourceEntityVo = new ResourceEntityVo();
                resourceEntityVo.setStatus(Status.PENDING.getValue());
            }
            resourceEntityVo.setName(sceneEntityVo.getName());
            resourceEntityVo.setLabel(sceneEntityVo.getLabel());
            resourceEntityVo.setDescription(sceneEntityVo.getDescription());
            resourceEntityVo.setIsMultiple(sceneEntityVo.getIsMultiple());
            try {
                resourceEntityMapper.getResourceEntityViewDataList(sceneEntityVo.getName(), 0, 1);
            } catch (Exception e) {
                resourceEntityVo.setStatus(Status.ERROR.getValue());
                String error = resourceEntityVo.getError();
                if (StringUtils.isNotBlank(error)) {
                    resourceEntityVo.setError(error + e.getMessage());
                } else {
                    resourceEntityVo.setError(e.getMessage());
                }
            }
            resultList.add(resourceEntityVo);
        }
        // 扩展视图
        List<ResourceEntityVo> allResourceEntityList = resourceEntityMapper.getResourceEntityList();
        for (ResourceEntityVo resourceEntityVo : allResourceEntityList) {
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
            if (sceneEntityVo == null) {
                String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
                if (StringUtils.isNotBlank(config)) {
                    ResourceEntityConfigVo resourceEntityConfigVo = JSONObject.parseObject(config, ResourceEntityConfigVo.class);
                    if (resourceEntityConfigVo != null) {
                        String sceneTemplateName = resourceEntityConfigVo.getSceneTemplateName();
                        if (StringUtils.isNotBlank(sceneTemplateName)) {
                            SceneEntityVo sceneTemplate = ResourceEntityFactory.getSceneEntityByViewName(sceneTemplateName);
                            if (sceneTemplate != null) {
                                resourceEntityVo.setIsMultiple(sceneTemplate.getIsMultiple());
                                try {
                                    resourceEntityMapper.getResourceEntityViewDataList(resourceEntityVo.getName(), 0, 1);
                                } catch (Exception e) {
                                    resourceEntityVo.setStatus(Status.ERROR.getValue());
                                    String error = resourceEntityVo.getError();
                                    if (StringUtils.isNotBlank(error)) {
                                        resourceEntityVo.setError(error + e.getMessage());
                                    } else {
                                        resourceEntityVo.setError(e.getMessage());
                                    }
                                }
                                resultList.add(resourceEntityVo);
                            }
                        }
                    }
                }
            }
        }
        resultList.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return resultList;
    }
}
