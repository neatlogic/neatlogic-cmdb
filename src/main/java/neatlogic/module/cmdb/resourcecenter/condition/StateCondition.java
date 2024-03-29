package neatlogic.module.cmdb.resourcecenter.condition;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import neatlogic.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import neatlogic.framework.common.constvalue.FormHandlerType;
import neatlogic.framework.dto.condition.ConditionVo;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StateCondition extends ResourcecenterConditionBase {
    @Resource
    ResourceMapper resourceMapper;
    private String formHandlerType = FormHandlerType.SELECT.toString();
    @Override
    public String getName() {
        return "stateIdList";
    }

    @Override
    public String getDisplayName() {
        return "资产状态";
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        if (value != null) {
            List<Long> valueList = new ArrayList<>();
            if (value instanceof String) {
                valueList.add(Long.valueOf(value.toString()));
            } else if (value instanceof List) {
                valueList = JSON.parseArray(JSON.toJSONString(value), Long.class);
            }
            if(CollectionUtils.isNotEmpty(valueList)) {
                List<ResourceVo> states = resourceMapper.searchStateListByIdList(valueList);
                if (CollectionUtils.isNotEmpty(states)) {
                    return states.stream().map(ResourceVo::getDescription).collect(Collectors.toList());
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.STATE_ID.getValue());
    }

}
