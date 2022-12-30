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

import java.util.List;

@Component
public class PortCondition extends ResourcecenterConditionBase {
    @Override
    public String getName() {
        return "port";
    }

    @Override
    public String getDisplayName() {
        return "端口";
    }

	@Override
	public String getHandler(FormConditionModel formConditionModel) {
		return FormHandlerType.INPUT.toString();
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONObject config = new JSONObject();
        config.put("type", FormHandlerType.INPUT.toString());
        return config;
    }

    @Override
    public Integer getSort() {
        return 11;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.NUMBER;
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.PORT.getValue());
    }
}
