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

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.customview.CustomViewAttrVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConstAttrVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewGlobalAttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCustomViewAttrApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewMapper customViewMapper;

    @Override
    public String getName() {
        return "nmcac.getcustomviewattrapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "isHidden", type = ApiParamType.INTEGER, rule = "1,0", desc = "common.ishidden", help = "1是，0否"),
            @Param(name = "isHasTargetCiId", type = ApiParamType.INTEGER, rule = "1,0", desc = "是否有目标模型", help = "1是，0否"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue")
    })
    @Output({
            @Param(name = "attrList", explode = CustomViewAttrVo[].class),
            @Param(name = "constAttrList", explode = CustomViewConstAttrVo[].class)
    })
    @Description(desc = "nmcac.getcustomviewattrapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<String> uuidList = defaultValue.toJavaList(String.class);
            List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByUuidList(uuidList);
            JSONObject returnObj = new JSONObject();
            returnObj.put("attrList", attrList);
            return returnObj;
        }
        Long id = paramObj.getLong("id");
        if (id == null) {
            throw new ParamNotExistsException("id");
        }
        Integer isHidden = paramObj.getInteger("isHidden");
        Integer isHasTargetCiId = paramObj.getInteger("isHasTargetCiId");
        CustomViewConstAttrVo customViewConstAttrVo = new CustomViewConstAttrVo(id);
        CustomViewGlobalAttrVo customViewGlobalAttrVo = new CustomViewGlobalAttrVo(id);
        CustomViewAttrVo customViewAttrVo = new CustomViewAttrVo(id);
        if (isHidden != null) {
            customViewGlobalAttrVo.setIsHidden(isHidden);
            customViewConstAttrVo.setIsHidden(isHidden);
            customViewAttrVo.setIsHidden(isHidden);
        }
        List<CustomViewGlobalAttrVo> globalAttrList = customViewMapper.getCustomViewGlobalAttrByCustomViewId(customViewGlobalAttrVo);
        List<CustomViewConstAttrVo> constAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(customViewConstAttrVo);
        List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("globalAttrList", globalAttrList);
        returnObj.put("constAttrList", constAttrList);
        if (isHasTargetCiId != null) {
            if (isHasTargetCiId.equals(1)) {
                attrList = attrList.stream().filter(attr -> attr.getAttrVo().getTargetCiId() != null).collect(Collectors.toList());
            } else if (isHasTargetCiId.equals(0)) {
                // 普通属性列表仅返回能够从动态表生成视图字段的属性。
                attrList = attrList.stream()
                        .filter(attr -> attr.getAttrVo().getTargetCiId() == null && !attr.getAttrVo().isInvokeAttr())
                        .collect(Collectors.toList());
            }
        }
        returnObj.put("attrList", attrList);
        return returnObj;
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/attr/get";
    }
}
