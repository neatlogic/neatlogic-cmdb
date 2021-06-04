/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import codedriver.module.cmdb.service.customview.CustomViewService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CUSTOMVIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchPublicCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getName() {
        return "查询公共自定义视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true),
            @Param(name = "currentPage",
                    type = ApiParamType.INTEGER,
                    desc = "当前页"),
            @Param(name = "pageSize",
                    type = ApiParamType.INTEGER,
                    desc = "每页数据条目"),
            @Param(name = "needPage",
                    type = ApiParamType.BOOLEAN,
                    desc = "是否需要分页，默认true")
    })
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询公共自定义视图接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CustomViewVo customViewVo = JSONObject.toJavaObject(paramObj, CustomViewVo.class);
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
        return "/cmdb/customview/public/search";
    }
}
