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

package neatlogic.module.cmdb.api.resourcecenter.config;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.common.util.ModuleUtil;
import neatlogic.framework.dto.module.ModuleVo;
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
        List<ResourceEntityVo> allResourceEntityList = resourceEntityMapper.getResourceEntityList();
        Map<String, ResourceEntityVo> resourceEntityVoMap = allResourceEntityList.stream().collect(Collectors.toMap(ResourceEntityVo::getName, e -> e));
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            ResourceEntityVo resourceEntityVo = resourceEntityVoMap.remove(sceneEntityVo.getName());
            if (resourceEntityVo == null) {
                resourceEntityVo = new ResourceEntityVo();
                resourceEntityVo.setName(sceneEntityVo.getName());
                resourceEntityVo.setLabel(sceneEntityVo.getLabel());
                resourceEntityVo.setDescription(sceneEntityVo.getDescription());
                resourceEntityVo.setStatus(Status.PENDING.getValue());
            }
            resourceEntityVo.setIsMultiple(sceneEntityVo.getIsMultiple());
            resourceEntityVo.setModuleId(sceneEntityVo.getModuleId());
            ModuleVo moduleVo = ModuleUtil.getModuleById(sceneEntityVo.getModuleId());
            if (moduleVo != null) {
                resourceEntityVo.setModuleName(moduleVo.getName());
            } else {
                resourceEntityVo.setModuleName(sceneEntityVo.getModuleId());
            }
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
        for (Map.Entry<String, ResourceEntityVo> entry : resourceEntityVoMap.entrySet()) {
            ResourceEntityVo resourceEntityVo = entry.getValue();
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
                                resourceEntityVo.setModuleId(sceneTemplate.getModuleId());
                                ModuleVo moduleVo = ModuleUtil.getModuleById(sceneTemplate.getModuleId());
                                if (moduleVo != null) {
                                    resourceEntityVo.setModuleName(moduleVo.getName());
                                } else {
                                    resourceEntityVo.setModuleName(sceneTemplate.getModuleId());
                                }
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
