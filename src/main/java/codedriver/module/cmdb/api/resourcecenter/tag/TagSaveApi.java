/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.tag;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterTagNameRepeatsException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterTagNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.RESOURCECENTER_TAG_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
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
