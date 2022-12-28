package codedriver.module.cmdb.api.resourcecenter.condition;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.framework.cmdb.resourcecenter.condition.IResourcecenterCondition;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionFactory;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourcecenterConditionApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "resourcecenter/condition/get";
    }

    @Override
    public String getName() {
        return "资产中心获取条件接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "conditionModel", type = ApiParamType.STRING, desc = "条件模型 simple|custom,  simple:目前用于用于资产条件过滤简单模式, custom:目前用于用于资产条件过自定义模式;默认custom")
    })
    @Output({
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "组件uuid"),
            @Param(name = "handler", type = ApiParamType.STRING, desc = "处理器"),
            @Param(name = "handlerName", type = ApiParamType.STRING, desc = "处理器名"),
            @Param(name = "handlerType", type = ApiParamType.STRING, desc = "控件类型 select|input|radio|userselect|date|area|time"),
            @Param(name = "type", type = ApiParamType.STRING, desc = "类型  form|common"),
            @Param(name = "expressionList[0].expression", type = ApiParamType.STRING, desc = "表达式"),
            @Param(name = "expressionList[0].expressionName", type = ApiParamType.STRING, desc = "表达式名")
    })
    @Description(desc = "流程编辑获取条件接口，目前用于流程编辑，初始化条件使用")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray resultArray = new JSONArray();
        String conditionModel = jsonObj.getString("conditionModel");
        FormConditionModel formConditionModel = FormConditionModel.getFormConditionModel(conditionModel);
        formConditionModel = formConditionModel == null ? FormConditionModel.CUSTOM : formConditionModel;
        //固定字段条件
        for (IResourcecenterCondition condition : ResourcecenterConditionFactory.getConditionHandlerList()) {
            JSONObject config = condition.getConfig();
            JSONObject commonObj = new JSONObject();
            commonObj.put("handler", condition.getName());
            commonObj.put("handlerName", condition.getDisplayName());
            commonObj.put("conditionModel", condition.getHandler(formConditionModel));
            commonObj.put("type", condition.getType());
            commonObj.put("config", config);
            commonObj.put("sort", condition.getSort());
            ParamType paramType = condition.getParamType();
            if (paramType != null) {
                commonObj.put("defaultExpression", paramType.getDefaultExpression().getExpression());
                JSONArray expressionArray = new JSONArray();
                for (Expression expression : paramType.getExpressionList()) {
                    JSONObject expressionObj = new JSONObject();
                    expressionObj.put("expression", expression.getExpression());
                    expressionObj.put("expressionName", expression.getExpressionName());
                    expressionArray.add(expressionObj);
                    commonObj.put("expressionList", expressionArray);
                }
            }

            resultArray.add(commonObj);
        }
        resultArray.sort(Comparator.comparing(obj-> ((JSONObject) obj).getInteger("sort")));
        return resultArray;
    }

}
