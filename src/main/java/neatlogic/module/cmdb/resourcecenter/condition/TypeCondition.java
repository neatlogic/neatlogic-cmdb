package neatlogic.module.cmdb.resourcecenter.condition;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import neatlogic.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import neatlogic.framework.dto.condition.ConditionVo;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TypeCondition extends ResourcecenterConditionBase {

    @Resource
    CiMapper ciMapper;

    @Override
    public String getName() {
        return "typeIdList";
    }

    @Override
    public String getDisplayName() {
        return "模型类型";
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
            if (CollectionUtils.isNotEmpty(valueList)) {
                List<CiVo> cis = ciMapper.getCiByIdList(valueList);
                if (CollectionUtils.isNotEmpty(cis)) {
                    return cis.stream().map(CiVo::getLabel).collect(Collectors.toList());
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb, String searchMode) {
        //模型类型需穿透
        ConditionVo conditionVo = conditionList.get(index);
        if (Objects.equals(searchMode, "value")) {
            List<Long> typeIdList = (List<Long>) conditionVo.getValueList();
            if (CollectionUtils.isNotEmpty(typeIdList)) {
                Set<Long> ciIdSet = new HashSet<>();
                for (Long ciId : typeIdList) {
                    CiVo ciVo = ciMapper.getCiById(ciId);
                    if (ciVo == null) {
                        throw new CiNotFoundException(ciId);
                    }
                    List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                    ciIdSet.addAll(ciIdList);
                }
                conditionVo.setValueList(new ArrayList<>(ciIdSet));
            }
            getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.TYPE_ID.getValue());
        } else {
            getSimpleSqlConditionWhere(conditionList.get(index), sqlSb, new ScenceIpobjectDetailTable().getShortName(), ScenceIpobjectDetailTable.FieldEnum.TYPE_LABEL.getValue());
        }
    }
}
