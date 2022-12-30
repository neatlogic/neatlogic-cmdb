package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.resourcecenter.AppSystemVo;
import codedriver.framework.cmdb.enums.resourcecenter.condition.ConditionConfigType;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AppSystemCondition extends ResourcecenterConditionBase {

    @Resource
    ResourceMapper resourceMapper;

    @Override
    public String getName() {
        return "appSystemIdList";
    }

    @Override
    public String getDisplayName() {
        return "应用";
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
        config.put("value", "");
        config.put("defaultValue", new ArrayList<String>());
        config.put("dynamicUrl", "/api/rest/resourcecenter/appsystem/list/forselect");
        config.put("rootName", "tbodyList");
        config.put("dealDataByUrl","getAppForselect");
        return config;
    }

    @Override
    public Integer getSort() {
        return 2;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
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
            List<AppSystemVo> appSystemVos = resourceMapper.getAppSystemListByIdList(valueList);
            if(CollectionUtils.isNotEmpty(appSystemVos)) {
                return appSystemVos.stream().map(o->String.format("%s(%s)",o.getAbbrName(),o.getName())).collect(Collectors.joining("、"));
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.APP_SYSTEM_ID.getValue());
    }
}
