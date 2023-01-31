package neatlogic.module.cmdb.resourcecenter.condition;

import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import neatlogic.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.dto.condition.ConditionVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OwnerCondition extends ResourcecenterConditionBase {
    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "ownerList";
    }

    @Override
    public String getDisplayName() {
        return "所有者";
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
            if(CollectionUtils.isNotEmpty(valueList)) {
                List<UserVo> users = userMapper.getUserByUserUuidList(valueList);
                if (CollectionUtils.isNotEmpty(users)) {
                    return users.stream().map(UserVo::getName).collect(Collectors.toList());
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.USER_UUID.getValue());
    }
}
