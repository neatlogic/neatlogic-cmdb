package codedriver.module.cmdb.dto.cientity;

import java.util.List;

public class RelFilterVo {
    private Long relId;
    private String expressionName; // 表达式名称
    private String expression;// 用户sql查询的表达式
    private List<Long> valueList;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getExpressionName() {
        return expressionName;
    }

    public void setExpressionName(String expressionName) {
        this.expressionName = expressionName;
    }

    public Long getRelId() {
        return relId;
    }

    public void setRelId(Long relId) {
        this.relId = relId;
    }

    public List<Long> getValueList() {
        return valueList;
    }

    public void setValueList(List<Long> valueList) {
        this.valueList = valueList;
    }


}
