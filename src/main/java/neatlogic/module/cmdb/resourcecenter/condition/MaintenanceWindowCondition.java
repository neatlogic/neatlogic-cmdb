package neatlogic.module.cmdb.resourcecenter.condition;

import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import neatlogic.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.dto.condition.ConditionVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MaintenanceWindowCondition extends ResourcecenterConditionBase {
    @Override
    public String getName() {
        return "maintenanceWindow";
    }

    @Override
    public String getDisplayName() {
        return "维护期";
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        ConditionVo condition = conditionList.get(index);
        Object value = StringUtils.EMPTY;
        if (condition.getValueList() instanceof String) {
            value = condition.getValueList();
        } else if (condition.getValueList() instanceof List) {
            List<String> values = JSON.parseArray(JSON.toJSONString(condition.getValueList()), String.class);
            value = String.join(",", values);
        }
        sqlSb.append(Expression.getExpressionSql(condition.getExpression(), new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.MAINTENANCE_WINDOW.getValue(), value.toString()));
    }
}
