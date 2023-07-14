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
        CustomViewVo customViewVo = JSONObject.toJavaObject(paramObj, CustomViewVo.class);
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
