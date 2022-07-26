/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.tag;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_TAG_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = RESOURCECENTER_TAG_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class TagSaveApi extends PrivateApiComponentBase {

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
        JSONObject resultObj = new JSONObject();
//        TagVo tagVo = JSON.toJavaObject(paramObj, TagVo.class);
//        Long id = paramObj.getLong("id");
//        if (resourceCenterMapper.checkTagNameIsRepeats(tagVo) > 0) {
//            throw new ResourceCenterTagNameRepeatsException(tagVo.getName());
//        }
//        if (id != null) {
//            if (resourceCenterMapper.checkTagIsExistsById(id) == 0) {
//                throw new ResourceCenterTagNotFoundException(id);
//            }
//            resourceCenterMapper.updateTag(tagVo);
//        } else {
//            resourceCenterMapper.insertTag(tagVo);
//        }
        return resultObj;
    }

    public IValid name() {
        return value -> {
//            TagVo vo = JSON.toJavaObject(value, TagVo.class);
//            if (resourceCenterMapper.checkTagNameIsRepeats(vo) > 0) {
//                return new FieldValidResultVo(new ResourceCenterTagNameRepeatsException(vo.getName()));
//            }
            return new FieldValidResultVo();
        };
    }

}
