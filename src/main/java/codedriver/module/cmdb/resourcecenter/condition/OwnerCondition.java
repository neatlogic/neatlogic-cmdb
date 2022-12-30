package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.enums.resourcecenter.condition.ConditionConfigType;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.constvalue.ProcessFieldType;
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
	public String getHandler(FormConditionModel formConditionModel) {
		return FormHandlerType.SELECT.toString();
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONObject config = new JSONObject();
        config.put("type", FormHandlerType.SELECT.toString());
        config.put("search", true);
        config.put("multiple", true);
        config.put("transfer", true);
        config.put("dynamicUrl", "/api/rest/user/search/forselect");
        config.put("params", new JSONObject(){{
            put("needPage",true);
        }});
        return config;
    }

    @Override
    public Integer getSort() {
        return 16;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
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
            List<UserVo> users = userMapper.getUserByUserUuidList(valueList);
            if (CollectionUtils.isNotEmpty(users)) {
                return users.stream().map(UserVo::getName).collect(Collectors.joining("、"));
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.USER_UUID.getValue());
    }
}
