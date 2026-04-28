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

package neatlogic.module.cmdb.rebuilddatabaseview.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.ResourceBuildSqlService;
import neatlogic.framework.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class ResourceViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private ResourceBuildSqlService resourceBuildSqlService;

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
        List<String> viewNameList = new ArrayList<>();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityList();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), resourceEntityVo.getName());
            if (Objects.equals(tableType, "VIEW")) {
                viewNameList.add(resourceEntityVo.getName());
                continue;
            }
            String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
            if (sceneEntityVo == null) {
                if (StringUtils.isBlank(config)) {
                    continue;
                }
                ResourceEntityConfigVo resourceEntityConfigVo = JSONObject.parseObject(config, ResourceEntityConfigVo.class);
                if (resourceEntityConfigVo == null) {
                    continue;
                }
                String sceneTemplateName = resourceEntityConfigVo.getSceneTemplateName();
                if (StringUtils.isBlank(sceneTemplateName)) {
                    continue;
                }
                SceneEntityVo sceneTemplate = ResourceEntityFactory.getSceneEntityByViewName(sceneTemplateName);
                if (sceneTemplate == null) {
                    continue;
                }
            }
            resourceEntityVo.setConfigStr(config);
            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(resourceEntityVo);
            resultList.add(viewStatusInfo);
            viewNameList.add(resourceEntityVo.getName());
        }
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            if (viewNameList.contains(sceneEntityVo.getName())) {
                continue;
            }
            ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
            resourceEntityVo.setName(sceneEntityVo.getName());
            resourceEntityVo.setLabel(sceneEntityVo.getLabel());
            resourceEntityVo.setConfigStr("{}");
            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(resourceEntityVo);
            resultList.add(viewStatusInfo);
        }
        return resultList;
    }

    @Override
    public List<ViewStatusInfo> createOrReplaceView() {
        List<ViewStatusInfo> resultList = Collections.synchronizedList(new ArrayList<>());
        List<String> viewNameList = new ArrayList<>();
        List<ResourceEntityVo> allResourceEntityList = new ArrayList<>();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityList();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
            if (sceneEntityVo == null) {
                if (StringUtils.isBlank(config)) {
                    continue;
                }
                ResourceEntityConfigVo resourceEntityConfigVo = JSONObject.parseObject(config, ResourceEntityConfigVo.class);
                if (resourceEntityConfigVo == null) {
                    continue;
                }
                String sceneTemplateName = resourceEntityConfigVo.getSceneTemplateName();
                if (StringUtils.isBlank(sceneTemplateName)) {
                    continue;
                }
                SceneEntityVo sceneTemplate = ResourceEntityFactory.getSceneEntityByViewName(sceneTemplateName);
                if (sceneTemplate == null) {
                    continue;
                }
            }
            resourceEntityVo.setConfigStr(config);
//            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(resourceEntityVo);
//            resultList.add(viewStatusInfo);
            viewNameList.add(resourceEntityVo.getName());
            allResourceEntityList.add(resourceEntityVo);
        }
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            if (viewNameList.contains(sceneEntityVo.getName())) {
                continue;
            }
            ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
            resourceEntityVo.setName(sceneEntityVo.getName());
            resourceEntityVo.setLabel(sceneEntityVo.getLabel());
            resourceEntityVo.setConfigStr("{}");
//            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(resourceEntityVo);
//            resultList.add(viewStatusInfo);
            allResourceEntityList.add(resourceEntityVo);
        }
//        System.out.println("allResourceEntityList.size() = " + allResourceEntityList.size());
        BatchRunner<ResourceEntityVo> runner = new BatchRunner<>();
        runner.execute(allResourceEntityList, 5, (threadIndex, dataIndex, resourceEntityVo) -> {
//            System.out.println("start " + "resourceEntityVo.getName() = " + resourceEntityVo.getName());
            long startTime = System.currentTimeMillis();
            ViewStatusInfo viewStatusInfo = rebuildSceneEntity(resourceEntityVo);
            viewStatusInfo.setTimeCost(System.currentTimeMillis() - startTime);
            resultList.add(viewStatusInfo);
//            System.out.println("end " + "resourceEntityVo.getName() = " + resourceEntityVo.getName() + (System.currentTimeMillis() - start) + ", " + JSON.toJSON(viewStatusInfo));
        }, "REBUILD-DATABASE-VIEW-FOR-RESOURCEVIEW");
        return resultList;
    }

    private ViewStatusInfo rebuildSceneEntity(ResourceEntityVo resourceEntityVo) {
        if (resourceEntityVo.getConfig() != null) {
            resourceEntityVo.setError(null);
            String sql = resourceBuildSqlService.buildResourceView(resourceEntityVo);
            if (StringUtils.isNotBlank(resourceEntityVo.getError())) {
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
