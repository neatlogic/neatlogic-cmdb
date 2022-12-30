package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
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
public class EnvCondition extends ResourcecenterConditionBase {
    @Resource
    ResourceMapper resourceMapper;

    private String formHandlerType = FormHandlerType.SELECT.toString();
    @Override
    public String getName() {
        return "envIdList";
    }

    @Override
    public String getDisplayName() {
        return "环境";
    }

	@Override
	public String getHandler(FormConditionModel formConditionModel) {
        if (FormConditionModel.SIMPLE == formConditionModel) {
            formHandlerType = FormHandlerType.CHECKBOX.toString();
        } else {
            formHandlerType = FormHandlerType.SELECT.toString();
        }
        return formHandlerType;
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONObject config = new JSONObject();
        config.put("type", formHandlerType);
        config.put("search", true);
        config.put("multiple", true);
        config.put("transfer", true);
        config.put("className", "block-span");
        config.put("value", "");
        config.put("defaultValue", new ArrayList<String>());
        config.put("url", "/api/rest/resourcecenter/appenv/list/forselect");
        config.put("rootName", "tbodyList");
        config.put("textName","name");
        config.put("valueName","id");
        return config;
    }

    @Override
    public Integer getSort() {
        return 4;
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
            List<ResourceVo> appSystemVos = resourceMapper.getAppEnvListByIdList(valueList);
            if(CollectionUtils.isNotEmpty(appSystemVos)) {
                return appSystemVos.stream().map(ResourceVo::getName).collect(Collectors.joining("、"));
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.ENV_ID.getValue());
    }

}
