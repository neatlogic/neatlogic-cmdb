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
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchGlobalAttrItemApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public String getName() {
        return "nmcag.searchglobalattritemapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "attrId", type = ApiParamType.LONG, desc = "属性id"),
            @Param(name = "attrName", type = ApiParamType.STRING, desc = "属性唯一标识"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的参数列表"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage")
    })
    @Output({@Param(explode = GlobalAttrItemVo[].class)})
    @Description(desc = "nmcag.searchglobalattritemapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long attrId = paramObj.getLong("attrId");
        String attrName = paramObj.getString("attrName");
        if (attrId == null && StringUtils.isBlank(attrName)) {
            throw new ParamNotExistsException("attrId", "attrName");
        }
        GlobalAttrItemVo globalAttrItemVo = JSON.toJavaObject(paramObj, GlobalAttrItemVo.class);
        if (attrId == null && StringUtils.isNotBlank(attrName)) {
            GlobalAttrVo globalAttrVo = globalAttrMapper.getGlobalAttrByName(attrName);
            if (globalAttrVo == null) {
                throw new GlobalAttrNotFoundException(attrName);
            }
            globalAttrItemVo.setAttrId(globalAttrVo.getId());
        }
        return globalAttrMapper.searchGlobalAttrItem(globalAttrItemVo);
    }

    @Override
    public String getToken() {
        return "/cmdb/globalattritem/search";
    }


}
