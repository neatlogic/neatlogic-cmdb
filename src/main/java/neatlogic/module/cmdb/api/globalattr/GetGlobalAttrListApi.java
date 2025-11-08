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

package neatlogic.module.cmdb.api.globalattr;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetGlobalAttrListApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public String getName() {
        return "获取全局属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表", isRequired = true),
            @Param(name = "needItem", type = ApiParamType.INTEGER, desc = "是否需要返回成员", rule = "0,1")})
    @Output({@Param(explode = GlobalAttrVo.class)})
    @Description(desc = "获取全局属性列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        GlobalAttrVo globalAttrVo = JSON.toJavaObject(paramObj, GlobalAttrVo.class);
        if (CollectionUtils.isNotEmpty(globalAttrVo.getIdList())) {
            return globalAttrMapper.getGlobalAttrByIdList(globalAttrVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/globalattr/getlist";
    }


}
