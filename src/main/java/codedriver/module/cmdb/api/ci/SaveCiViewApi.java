package codedriver.module.cmdb.api.ci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiViewApi extends ApiComponentBase {

    @Autowired
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ciview/save";
    }

    @Override
    public String getName() {
        return "保存模型显示配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "type", type = ApiParamType.ENUM, rule = "custom,property,expression", isRequired = true,
            desc = "属性类型，自定义｜属性值｜表达式"),
        @Param(name = "propId", type = ApiParamType.LONG, desc = "属性id"),
        @Param(name = "expression", type = ApiParamType.STRING, desc = "表达式"),
        @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, maxLength = 50, desc = "英文名称"),
        @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100,
            isRequired = true),
        @Param(name = "description", type = ApiParamType.STRING, desc = "备注", maxLength = 500, xss = true),
        @Param(name = "validator", type = ApiParamType.STRING, desc = "校验组件"),
        @Param(name = "validConfig", type = ApiParamType.JSONOBJECT, desc = "校验设置"),
        @Param(name = "isRequired", type = ApiParamType.INTEGER, desc = "是否必填", isRequired = true),
        @Param(name = "isSearch", type = ApiParamType.INTEGER, desc = "是否作为搜索条件", isRequired = true),
        @Param(name = "isUnique", type = ApiParamType.INTEGER, desc = "是否唯一", isRequired = true),
        @Param(name = "showType", type = ApiParamType.ENUM, rule = "none,all,list,detail",
            desc = "显示类型，不显示|全显示|仅列表｜仅明细", isRequired = true),
        @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", desc = "输入类型，人工录入|自动发现",
            isRequired = true),
        @Param(name = "groupName", type = ApiParamType.STRING, desc = "分组")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "属性id"),})
    @Description(desc = "保存模型显示配置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return null;
    }

}
