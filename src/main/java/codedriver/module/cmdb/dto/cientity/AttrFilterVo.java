package codedriver.module.cmdb.dto.cientity;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

public class AttrFilterVo {
    private Long attrId;
    private Long propId;
    private String expressionName; // 表达式名称
    private String expression;// 用户sql查询的表达式
    private List<String> valueList;
    @SuppressWarnings("unused")
    private List<String> valueHashList;

    public List<String> getValueHashList() {
        if (CollectionUtils.isNotEmpty(getValueList())) {
            return getValueList().stream().map(d -> DigestUtils.md5DigestAsHex(d.toLowerCase().getBytes()))
                .collect(Collectors.toList());
        }
        return null;
    }

    public Long getAttrId() {
        return attrId;
    }

    public void setAttrId(Long attrId) {
        this.attrId = attrId;
    }

    public Long getPropId() {
        return propId;
    }

    public void setPropId(Long propId) {
        this.propId = propId;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public List<String> getValueList() {
        if (CollectionUtils.isNotEmpty(valueList)) {
            return valueList.stream().filter(v -> StringUtils.isNotBlank(v)).collect(Collectors.toList());
        }
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public String getExpressionName() {
        return expressionName;
    }

    public void setExpressionName(String expressionName) {
        this.expressionName = expressionName;
    }

}
