/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.enums.customview.SearchMode;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.customview.CustomViewDataService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCustomViewDataApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewDataService customViewDataService;

    @Override
    public String getName() {
        return "获取自定义视图数据";
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/data/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id", isRequired = true),
            @Param(name = "searchMode", type = ApiParamType.ENUM, rule = "normal,group", isRequired = true, desc = "搜索模式：normal或group"),
            @Param(name = "groupBy", type = ApiParamType.STRING, desc = "分组属性的uuid")
    })
    @Output({@Param(name = "dataList", type = ApiParamType.JSONARRAY, desc = "结果集"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小")})
    @Description(desc = "获取自定义视图数据接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CustomViewConditionVo customViewConditionVo = JSONObject.toJavaObject(paramObj, CustomViewConditionVo.class);
        customViewConditionVo.setCustomViewId(paramObj.getLong("id"));
        JSONObject returnObj = new JSONObject();
        if (customViewConditionVo.getSearchMode().equals(SearchMode.NORMAL.getValue())) {
            returnObj.put("dataList", customViewDataService.searchCustomViewData(customViewConditionVo));
        } else {
            returnObj.put("dataList", customViewDataService.searchCustomViewDataGroup(customViewConditionVo));
        }
        returnObj.put("pageSize", customViewConditionVo.getPageSize());
        returnObj.put("currentPage", customViewConditionVo.getCurrentPage());
        return returnObj;
    }


}
