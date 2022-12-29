package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.enums.resourcecenter.condition.ConditionConfigType;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.constvalue.ProcessFieldType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MaintenanceWindowCondition extends ResourcecenterConditionBase {
    @Override
    public String getName() {
        return "networkArea";
    }

    @Override
    public String getDisplayName() {
        return "维护期";
    }

    @Override
    public String getHandler(FormConditionModel formConditionModel) {
        return FormHandlerType.DATE.toString();
    }

    @Override
    public String getType() {
        return ProcessFieldType.COMMON.getValue();
    }

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONObject config = new JSONObject();
        config.put("type", FormHandlerType.DATE.toString());
        return config;
    }

    @Override
    public Integer getSort() {
        return 15;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.DATE;
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {

        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        ConditionVo condition = conditionList.get(index);
        Object value = StringUtils.EMPTY;
        if (condition.getValueList() instanceof String) {
            value = condition.getValueList();
        } else if (condition.getValueList() instanceof List) {
            List<String> values = JSON.parseArray(JSON.toJSONString(condition.getValueList()), String.class);
            value = String.join("','", values);
        }
        sqlSb.append(String.format("%s.%s = %s", new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.MAINTENANCE_WINDOW.getValue(), value.toString()));
    }
}
