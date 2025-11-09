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

package neatlogic.module.cmdb.api.resourcecenter.tag;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_TAG_MODIFY;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterTagHasBeenReferredException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterTagNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
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
