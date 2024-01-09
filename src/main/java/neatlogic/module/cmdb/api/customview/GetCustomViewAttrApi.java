/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.customview.CustomViewAttrVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConstAttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCustomViewAttrApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewMapper customViewMapper;

    @Override
    public String getName() {
        return "获取自定义视图属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "视图id"),
            @Param(name = "isHidden", type = ApiParamType.INTEGER, desc = "属性是否隐藏，1是，0否"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue")
    })
    @Output({
            @Param(name = "attrList", explode = CustomViewAttrVo[].class),
            @Param(name = "constAttrList", explode = CustomViewConstAttrVo[].class)
    })
    @Description(desc = "获取自定义视图属性列表接口")
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
        CustomViewConstAttrVo customViewConstAttrVo = new CustomViewConstAttrVo(id);
        CustomViewAttrVo customViewAttrVo = new CustomViewAttrVo(id);
        if (isHidden != null) {
            customViewConstAttrVo.setIsHidden(isHidden);
            customViewAttrVo.setIsHidden(isHidden);
        }
        List<CustomViewConstAttrVo> constAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(customViewConstAttrVo);
        List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("constAttrList", constAttrList);
        returnObj.put("attrList", attrList);
        return returnObj;
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/attr/get";
    }
}
