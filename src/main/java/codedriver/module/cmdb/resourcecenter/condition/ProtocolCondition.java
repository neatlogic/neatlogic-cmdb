package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.CmdbResourcecenterAccountTable;
import codedriver.framework.dto.condition.ConditionVo;
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
    public Object valueConversionText(Object value, JSONObject config) {
        if (value != null) {
            List<Long> valueList = new ArrayList<>();
            if (value instanceof String) {
                valueList.add(Long.valueOf(value.toString()));
            } else if (value instanceof List) {
                valueList = JSON.parseArray(JSON.toJSONString(value), Long.class);
            }
            if(CollectionUtils.isNotEmpty(valueList)) {
                List<AccountProtocolVo> protocolVos = resourceAccountMapper.getAccountProtocolListByIdList(valueList);
                if (CollectionUtils.isNotEmpty(protocolVos)) {
                    return protocolVos.stream().map(AccountProtocolVo::getName).collect(Collectors.toList());
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new CmdbResourcecenterAccountTable().getShortName(), CmdbResourcecenterAccountTable.FieldEnum.PROTOCOL_ID.getValue());
    }

}
