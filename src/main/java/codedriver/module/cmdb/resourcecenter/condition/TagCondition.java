package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.CmdbResourcecenterResourceTagTable;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TagCondition extends ResourcecenterConditionBase {

    @Resource
    ResourceTagMapper resourceTagMapper;
    @Override
    public String getName() {
        return "tagIdList";
    }

    @Override
    public String getDisplayName() {
        return "标签";
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
                List<TagVo> tags = resourceTagMapper.getTagListByIdList(valueList);
                if (CollectionUtils.isNotEmpty(tags)) {
                    return tags.stream().map(TagVo::getName).collect(Collectors.joining("、"));
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new CmdbResourcecenterResourceTagTable().getShortName(), CmdbResourcecenterResourceTagTable.FieldEnum.TAG_ID.getValue());
    }
}
