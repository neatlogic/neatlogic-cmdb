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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.cmdb.enums.resourcecenter.ViewType;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import neatlogic.framework.cmdb.utils.ResourceViewGenerateSqlUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import neatlogic.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/11/9 11:26
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class SaveResourceEntityApi extends PrivateApiComponentBase {
    @Resource
    private CiMapper ciMapper;
    @Resource
    private ResourceEntityMapper resourceEntityMapper;
    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/save";
    }

    @Override
    public String getName() {
        return "nmcarc.saveresourceentityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name"),
            @Param(name = "label", type = ApiParamType.STRING, isRequired = true, desc = "common.cnname"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置"),
//            @Param(name = "xml", type = ApiParamType.STRING, desc = "common.config"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description")
    })
    @Description(desc = "nmcarc.saveresourceentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceEntityVo resourceEntityVo = paramObj.toJavaObject(ResourceEntityVo.class);
        SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
        if (sceneEntityVo == null) {
            throw new ResourceCenterResourceFoundException(resourceEntityVo.getName());
        }
        ResourceEntityConfigVo config = resourceEntityVo.getConfig();
        String mainCi = config.getMainCi();
        if (StringUtils.isNotBlank(mainCi)) {
            CiVo mainCiVo = ciMapper.getCiByName(mainCi);
            if (mainCiVo != null) {
                resourceEntityVo.setCiId(mainCiVo.getId());
            }
        }
        boolean configEquals = false;
        ResourceEntityVo oldResourceEntityVo = resourceEntityMapper.getResourceEntityByName(resourceEntityVo.getName());
        if (oldResourceEntityVo != null) {
            boolean labelEquals = Objects.equals(resourceEntityVo.getLabel(), oldResourceEntityVo.getLabel());
            boolean xmlEquals = Objects.equals(resourceEntityVo.getXml(), oldResourceEntityVo.getXml());
            boolean descriptionEquals = Objects.equals(resourceEntityVo.getDescription(), oldResourceEntityVo.getDescription());
            configEquals = Objects.equals(resourceEntityVo.getConfigStr(), oldResourceEntityVo.getConfigStr());
            if (labelEquals && xmlEquals && descriptionEquals && configEquals) {
                return null;
            }
            resourceEntityMapper.updateResourceEntityLabelAndDescription(resourceEntityVo);
        } else {
            resourceEntityVo.setType(ViewType.SCENE.getValue());
            resourceEntityMapper.insertResourceEntity(resourceEntityVo);
        }
        if (!configEquals) {
//            resourceEntityVo.setError("");
//            resourceEntityVo.setStatus(Status.PENDING.getValue());
//            resourceEntityMapper.updateResourceEntity(resourceEntityVo);
//            if (StringUtils.isNotBlank(resourceEntityVo.getXml())) {
//                ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(resourceEntityVo);
//                builder.buildView();
//            }
            String error = resourceCenterResourceService.buildResourceView(resourceEntityVo.getName(), resourceEntityVo.getConfig());
            resourceEntityVo.setError(error);
            if (StringUtils.isNotBlank(error)) {
                resourceEntityVo.setStatus(Status.ERROR.getValue());
            } else {
                resourceEntityVo.setStatus(Status.READY.getValue());
            }
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
        }
        return null;
    }
}
