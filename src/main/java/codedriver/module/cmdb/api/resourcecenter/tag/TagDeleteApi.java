/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.tag;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterTagHasBeenReferredException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterTagNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.RESOURCECENTER_TAG_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_TAG_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class TagDeleteApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/tag/delete";
    }

    @Override
    public String getName() {
        return "删除资源中心标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "标签ID"),
    })
    @Output({
    })
    @Description(desc = "删除资源中心标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        TagVo tag = resourceTagMapper.getTagById(id);
        if (tag == null) {
            throw new ResourceCenterTagNotFoundException(id);
        }
        if (resourceTagMapper.checkTagHasBeenReferredById(id) > 0) {
            throw new ResourceCenterTagHasBeenReferredException(tag.getName());
        }
        resourceTagMapper.deleteTagById(id);
        return null;
    }

}
