package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.condition.ConditionVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BgCondition extends ResourcecenterConditionBase {

    @Resource
    TeamMapper teamMapper;

    @Override
    public String getName() {
        return "bgList";
    }

    @Override
    public String getDisplayName() {
        return "所属部门";
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        if (value != null) {
            List<String> valueList = new ArrayList<>();
            if (value instanceof String) {
                valueList.add(value.toString());
            } else if (value instanceof List) {
                valueList = JSON.parseArray(JSON.toJSONString(value), String.class);
            }
            List<TeamVo> bgs = teamMapper.getTeamByUuidList(valueList);
            if (CollectionUtils.isNotEmpty(bgs)) {
                return bgs.stream().map(TeamVo::getName).collect(Collectors.joining("、"));
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.BG_ID.getValue());
    }
}
