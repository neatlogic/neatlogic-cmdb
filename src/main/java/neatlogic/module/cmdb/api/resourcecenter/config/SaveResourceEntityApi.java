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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceEntityNameRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description")
    })
    @Description(desc = "nmcarc.saveresourceentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        String label = paramObj.getString("label");
        String configStr = paramObj.getString("config");
        String description = paramObj.getString("description");
        ResourceEntityVo resourceEntityVo = new ResourceEntityVo();
        resourceEntityVo.setName(name);
        resourceEntityVo.setLabel(label);
        resourceEntityVo.setConfigStr(configStr);
        resourceEntityVo.setDescription(description);
        SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());

        ResourceEntityConfigVo config = resourceEntityVo.getConfig();
        String mainCi = config.getMainCi();
        if (StringUtils.isNotBlank(mainCi)) {
            CiVo mainCiVo = ciMapper.getCiByName(mainCi);
            if (mainCiVo != null) {
                resourceEntityVo.setCiId(mainCiVo.getId());
                resourceEntityVo.setCi(mainCiVo);
            }
        }
        if (sceneEntityVo != null) {
            resourceEntityVo.setDescription(sceneEntityVo.getDescription());
            resourceEntityVo.setLabel(sceneEntityVo.getLabel());
        }
//        boolean configEquals = false;
        ResourceEntityVo oldResourceEntityVo = resourceEntityMapper.getResourceEntityByName(resourceEntityVo.getName());
        if (oldResourceEntityVo != null) {
//            configEquals = Objects.equals(resourceEntityVo.getConfigStr(), oldResourceEntityVo.getConfigStr());
//            if (configEquals) {
//                return null;
//            }
            resourceEntityMapper.updateResourceEntityLabelAndDescription(resourceEntityVo);
        } else {
            resourceEntityVo.setStatus(Status.PENDING.getValue());
            resourceEntityMapper.insertResourceEntity(resourceEntityVo);
        }
//        if (!configEquals) {
            resourceEntityVo.setError(null);
            String sql = resourceCenterResourceService.buildResourceView(resourceEntityVo);
            if (StringUtils.isNotBlank(resourceEntityVo.getError())) {
                resourceEntityVo.setStatus(Status.ERROR.getValue());
            } else {
                resourceEntityVo.setStatus(Status.READY.getValue());
            }
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
            return sql;
//        }
//        return null;
    }

    public IValid name() {
        return value -> {
            String name = value.getString("name");
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(name);
            if (sceneEntityVo != null) {
                return new FieldValidResultVo(new ResourceEntityNameRepeatException(name));
            }
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
            if (resourceEntityVo != null) {
                return new FieldValidResultVo(new ResourceEntityNameRepeatException(name));
            }
            return new FieldValidResultVo();
        };
    }
}
