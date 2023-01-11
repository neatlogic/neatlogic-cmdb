package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.InspectStatus;
import codedriver.framework.dto.condition.ConditionVo;
import com.alibaba.fastjson.JSON;
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
    public Object valueConversionText(Object value, JSONObject config) {
        if (value != null) {
            List<String> valueList = new ArrayList<>();
            List<String> textList = new ArrayList<>();
            if (value instanceof String) {
                valueList.add(value.toString());
            } else if (value instanceof List) {
                valueList = JSON.parseArray(JSON.toJSONString(value), String.class);
            }
            for (String valueTmp : valueList) {
                textList.add(InspectStatus.getText(valueTmp));
            }
            return textList;
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.INSPECT_STATUS.getValue());
    }
}
