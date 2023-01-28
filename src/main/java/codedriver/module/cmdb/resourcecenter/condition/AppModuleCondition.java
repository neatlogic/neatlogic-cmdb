package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AppModuleCondition extends ResourcecenterConditionBase {

    @Resource
    ResourceMapper resourceMapper;

    @Override
    public String getName() {
        return "appModuleIdList";
    }

    @Override
    public String getDisplayName() {
        return "模块";
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
                List<ResourceVo> appModules = resourceMapper.getAppModuleListByIdListSimple(valueList, false);
                if (CollectionUtils.isNotEmpty(appModules)) {
                    return appModules.stream().map(o -> StringUtils.isNotBlank(o.getName())?String.format("%s(%s)", o.getAbbrName(), o.getName()):o.getAbbrName()).collect(Collectors.toList());
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.APP_MODULE_ID.getValue());
    }

}
