/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.customview.CustomViewAttrVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConstAttrVo;
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
            @Param(name = "isHidden", type = ApiParamType.INTEGER, desc = "common.ishidden，", help = "1是，0否"),
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
