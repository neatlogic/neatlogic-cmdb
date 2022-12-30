package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.enums.resourcecenter.condition.ConditionConfigType;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.CmdbResourcecenterAccountTable;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProtocolCondition extends ResourcecenterConditionBase {
    @Resource
    ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "protocolIdList";
    }

    @Override
    public String getDisplayName() {
        return "连接协议";
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
        config.put("dynamicUrl", "/api/rest/resourcecenter/account/protocol/search");
        config.put("rootName", "tbodyList");
        config.put("dealDataByUrl","getProtocolDataList");
        config.put("className","block-span");
        return config;
    }

    @Override
    public Integer getSort() {
        return 5;
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
            List<AccountProtocolVo> protocolVos = resourceAccountMapper.getAccountProtocolListByIdList(valueList);
            if (CollectionUtils.isNotEmpty(protocolVos)) {
                return protocolVos.stream().map(AccountProtocolVo::getName).collect(Collectors.joining("、"));
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new CmdbResourcecenterAccountTable().getShortName(), CmdbResourcecenterAccountTable.FieldEnum.PROTOCOL_ID.getValue());
    }

}
