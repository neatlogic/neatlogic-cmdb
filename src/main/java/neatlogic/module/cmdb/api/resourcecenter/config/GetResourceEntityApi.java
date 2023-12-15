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
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
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
        SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(name);
        if (sceneEntityVo == null) {
            throw new ResourceCenterResourceFoundException(name);
        }
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
        if (resourceEntityVo == null) {
            resourceEntityVo = new ResourceEntityVo();
            resourceEntityVo.setName(sceneEntityVo.getName());
        } else if (resourceEntityVo.getCiId() != null) {
            CiVo ciVo = ciMapper.getCiById(resourceEntityVo.getCiId());
            if (ciVo != null) {
                resourceEntityVo.setCi(ciVo);
            }
        }
        resourceEntityVo.setLabel(sceneEntityVo.getLabel());
        resourceEntityVo.setDescription(sceneEntityVo.getDescription());
        List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName(name);
        resourceEntityVo.setFieldList(fieldList);
        return resourceEntityVo;
    }
}
