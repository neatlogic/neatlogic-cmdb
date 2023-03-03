package neatlogic.module.cmdb.resourcecenter.condition;

import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import neatlogic.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import neatlogic.framework.dto.condition.ConditionVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchSearchCondition extends ResourcecenterConditionBase {
    @Override
    public String getName() {
        return "batchSearchList";
    }

    @Override
    public String getDisplayName() {
        return "批量搜索";
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.IP.getValue());
    }

}
