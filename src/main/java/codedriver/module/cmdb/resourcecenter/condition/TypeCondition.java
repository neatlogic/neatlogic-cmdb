package codedriver.module.cmdb.resourcecenter.condition;

import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.resourcecenter.condition.ResourcecenterConditionBase;
import codedriver.framework.cmdb.resourcecenter.table.ScenceIpobjectDetailTable;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            if(CollectionUtils.isNotEmpty(valueList)) {
                List<CiVo> cis = ciMapper.getCiByIdList(valueList);
                if (CollectionUtils.isNotEmpty(cis)) {
                    return cis.stream().map(CiVo::getName).collect(Collectors.joining("、"));
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        //模型类型需穿透
        ConditionVo conditionVo = conditionList.get(index);
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
    }
}
