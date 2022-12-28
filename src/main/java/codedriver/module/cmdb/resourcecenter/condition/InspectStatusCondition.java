package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.enums.resourcecenter.condition.ConditionConfigType;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.constvalue.ProcessFieldType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InspectStatusCondition extends ResourcecenterConditionBase {
    private String formHandlerType = FormHandlerType.SELECT.toString();
    @Override
    public String getName() {
        return "inspectStatusList";
    }

    @Override
    public String getDisplayName() {
        return "巡检状态";
    }

	@Override
	public String getHandler(FormConditionModel formConditionModel) {
        if (FormConditionModel.SIMPLE == formConditionModel) {
            formHandlerType = FormHandlerType.CHECKBOX.toString();
        } else {
            formHandlerType = FormHandlerType.SELECT.toString();
        }
        return formHandlerType;
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONObject config = new JSONObject();
        config.put("type", formHandlerType);
        config.put("search", true);
        config.put("multiple", true);
        config.put("transfer", true);
        config.put("className", "block-span");
        config.put("value", "");
        config.put("defaultValue", new ArrayList<String>());
        config.put("dynamicUrl", "/api/rest/universal/enum/get");
        config.put("params", new JSONObject(){{
            put("enumClass","codedriver.framework.common.constvalue.InspectStatus");
        }});
        return config;
    }

    @Override
    public Integer getSort() {
        return 1;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {

        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.INSPECT_STATUS.getValue());
    }
}
