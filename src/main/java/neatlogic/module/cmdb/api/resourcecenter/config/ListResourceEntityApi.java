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

package neatlogic.module.cmdb.api.resourcecenter.config;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.cmdb.enums.resourcecenter.ViewType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
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
        Map<String, ResourceEntityVo> resourceEntityVoMap = resourceEntityList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            ResourceEntityVo resourceEntityVo = resourceEntityVoMap.get(sceneEntityVo.getName());
            if (resourceEntityVo == null) {
                resourceEntityVo = new ResourceEntityVo();
                resourceEntityVo.setName(sceneEntityVo.getName());
                resourceEntityVo.setLabel(sceneEntityVo.getLabel());
                resourceEntityVo.setStatus(Status.NO_CONFIGURE_VIEW_RULE.getValue());
                resourceEntityVo.setType(ViewType.SCENE.getValue());
            }
            resultList.add(resourceEntityVo);
        }
        return resultList;
    }

}
