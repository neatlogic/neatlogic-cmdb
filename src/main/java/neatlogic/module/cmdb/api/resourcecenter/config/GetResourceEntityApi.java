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
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityRelNodeVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author linbq
 * @since 2021/11/9 11:28
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourceEntityApi extends PrivateApiComponentBase {
    @Resource
    private ResourceEntityMapper resourceEntityMapper;
    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/get";
    }

    @Override
    public String getName() {
        return "nmcarc.getresourceentityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name")
    })
    @Output({
            @Param(name = "Return", explode = ResourceEntityVo.class, desc = "term.cmdb.resourceentityinfo")
    })
    @Description(desc = "nmcarc.getresourceentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
        if (resourceEntityVo == null) {
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(name);
            if (sceneEntityVo == null) {
                throw new ResourceCenterResourceFoundException(name);
            }
            resourceEntityVo = new ResourceEntityVo();
            resourceEntityVo.setName(sceneEntityVo.getName());
            resourceEntityVo.setLabel(sceneEntityVo.getLabel());
            resourceEntityVo.setDescription(sceneEntityVo.getDescription());
            resourceEntityVo.setIsMultiple(sceneEntityVo.getIsMultiple());
            List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName(name);
            resourceEntityVo.setFieldList(fieldList);
            ResourceEntityConfigVo config = new ResourceEntityConfigVo();
            config.setRelNode(new ResourceEntityRelNodeVo());
            resourceEntityVo.setConfig(config);
            return resourceEntityVo;
        }
        ResourceEntityConfigVo config = resourceEntityVo.getConfig();
        if (config != null) {
            if (StringUtils.isNotBlank(config.getMainCi())) {
                CiVo ciVo = ciMapper.getCiByName(config.getMainCi());
                if (ciVo != null) {
                    resourceEntityVo.setCi(ciVo);
                    ResourceEntityRelNodeVo relNode = config.getRelNode();
                    if (relNode == null) {
                        relNode = new ResourceEntityRelNodeVo();
                        relNode.setUuid(UuidUtil.randomUuid());
                        relNode.setCiName(ciVo.getName());
                        relNode.setCiLabel(ciVo.getLabel());
                        config.setRelNode(relNode);
                    }
                }
            }
            String sceneTemplateName = config.getSceneTemplateName();
            if (StringUtils.isNotBlank(sceneTemplateName)) {
                resourceEntityVo.setIsMultiple(true);
                SceneEntityVo sceneTemplate = ResourceEntityFactory.getSceneEntityByViewName(sceneTemplateName);
                if (sceneTemplate != null) {
                    resourceEntityVo.setIsMultiple(sceneTemplate.getIsMultiple());
                    List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName(sceneTemplateName);
                    resourceEntityVo.setFieldList(fieldList);
                }
            } else {
                resourceEntityVo.setIsMultiple(false);
                SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(name);
                if (sceneEntityVo != null) {
                    resourceEntityVo.setIsMultiple(sceneEntityVo.getIsMultiple());
                    List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName(name);
                    resourceEntityVo.setFieldList(fieldList);
                }
            }
        }
        return resourceEntityVo;
    }
}
