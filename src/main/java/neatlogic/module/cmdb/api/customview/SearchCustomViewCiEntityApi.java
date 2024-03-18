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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCustomViewCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewDataService customViewDataService;


    @Override
    public String getName() {
        return "查询自定义视图的配置项数据";
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/data/search/cientity";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "customViewId", type = ApiParamType.LONG, desc = "视图id", isRequired = true)
    })
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", desc = "结果数据", type = ApiParamType.JSONARRAY)})
    @Description(desc = "查询自定义视图的配置项数据接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CustomViewConditionVo customViewConditionVo = JSONObject.toJavaObject(paramObj, CustomViewConditionVo.class);
        List<CiEntityVo> ciEntityList = customViewDataService.searchCustomViewCiEntity(customViewConditionVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", customViewConditionVo.getPageSize());
        returnObj.put("ciEntityList", ciEntityList);
        return returnObj;
    }


}
