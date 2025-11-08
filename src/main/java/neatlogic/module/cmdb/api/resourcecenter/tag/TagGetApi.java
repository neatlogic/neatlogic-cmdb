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
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TagGetApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/tag/get";
    }

    @Override
    public String getName() {
        return "查询资源中心标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "标签ID"),
            @Param(name = "name", type = ApiParamType.STRING, minLength = 1, desc = "标签名称")
    })
    @Output({
            @Param(explode = TagVo.class, desc = "标签")
    })
    @Description(desc = "查询资源中心标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        if (id != null) {
            return resourceTagMapper.getTagById(id);
        }
        String name = paramObj.getString("name");
        if (StringUtils.isNotBlank(name)) {
            return resourceTagMapper.getTagByName(name);
        }
        return null;
    }

}
