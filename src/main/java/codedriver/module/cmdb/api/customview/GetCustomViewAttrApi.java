/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConstAttrVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import com.alibaba.fastjson.JSONObject;
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

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id", isRequired = true)})
    @Output({@Param(name = "attrList", explode = CustomViewAttrVo[].class), @Param(name = "constAttrList", explode = CustomViewConstAttrVo[].class)})
    @Description(desc = "获取自定义视图属性列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        List<CustomViewConstAttrVo> constAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(new CustomViewConstAttrVo(id));
        List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(id));
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
