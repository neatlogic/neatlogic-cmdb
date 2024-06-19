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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getName() {
        return "nmcac.searchcustomviewapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "tagId", type = ApiParamType.LONG, desc = "common.tagid"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid", help = "nmcac.searchcustomviewapi.input.param.help.ciid"),
            @Param(name = "startCiId", type = ApiParamType.LONG, desc = "term.cmdb.startciid"),
            @Param(name = "currentPage",
                    type = ApiParamType.INTEGER,
                    desc = "common.currentpage"),
            @Param(name = "pageSize",
                    type = ApiParamType.INTEGER,
                    desc = "common.pagesize"),
            @Param(name = "needPage",
                    type = ApiParamType.BOOLEAN,
                    desc = "common.isneedpage")
    })
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CustomViewVo[].class)})
    @Description(desc = "nmcac.searchcustomviewapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CustomViewVo customViewVo = JSON.toJavaObject(paramObj, CustomViewVo.class);
        String userUuid = UserContext.get().getUserUuid(true);
        customViewVo.setFcu(userUuid);
        if (AuthActionChecker.check(CUSTOMVIEW_MODIFY.class.getSimpleName())) {
            customViewVo.setAdmin(true);
        } else {
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            customViewVo.setUserUuid(authenticationInfoVo.getUserUuid());
            customViewVo.setTeamUuidList(authenticationInfoVo.getTeamUuidList());
            customViewVo.setRoleUuidList(authenticationInfoVo.getRoleUuidList());
        }

        List<CustomViewVo> viewList = customViewService.searchCustomView(customViewVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", customViewVo.getPageSize());
        returnObj.put("currentPage", customViewVo.getCurrentPage());
        returnObj.put("rowNum", customViewVo.getRowNum());
        returnObj.put("pageCount", customViewVo.getPageCount());
        returnObj.put("tbodyList", viewList);
        return returnObj;
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/search";
    }
}
