/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.cmdb.api.resourcecenter.tag;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterTagNameRepeatsException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterTagNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_TAG_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_TAG_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class TagSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/tag/save";
    }

    @Override
    public String getName() {
        return "保存资源中心标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "标签ID"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 50, xss = true, isRequired = true, desc = "名称"),
            @Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "描述"),
    })
    @Output({
    })
    @Description(desc = "保存资源中心标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        TagVo tagVo = JSONObject.toJavaObject(paramObj, TagVo.class);
        Long id = paramObj.getLong("id");
        if (resourceTagMapper.checkTagNameIsRepeats(tagVo) > 0) {
            throw new ResourceCenterTagNameRepeatsException(tagVo.getName());
        }
        if (id != null) {
            if (resourceTagMapper.checkTagIsExistsById(id) == 0) {
                throw new ResourceCenterTagNotFoundException(id);
            }
            resourceTagMapper.updateTag(tagVo);
        } else {
            resourceTagMapper.insertTag(tagVo);
        }
        return tagVo;
    }

    public IValid name() {
        return value -> {
            TagVo vo = JSONObject.toJavaObject(value, TagVo.class);
            if (resourceTagMapper.checkTagNameIsRepeats(vo) > 0) {
                return new FieldValidResultVo(new ResourceCenterTagNameRepeatsException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
