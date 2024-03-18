/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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
