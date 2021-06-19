/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionFieldVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.service.customview.CustomViewDataService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCustomViewCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewDataService customViewDataService;

    @Resource
    private CustomViewMapper customViewMapper;

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
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        customViewConditionVo.setFieldList(customViewAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "attr")).collect(Collectors.toList()));
        List<CiEntityVo> ciEntityList = customViewDataService.searchCustomViewCiEntity(customViewConditionVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", customViewConditionVo.getPageSize());
        returnObj.put("ciEntityList", ciEntityList);
        return returnObj;
    }


}
