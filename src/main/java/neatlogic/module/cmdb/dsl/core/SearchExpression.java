/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.dsl.core;

import neatlogic.framework.cmdb.exception.dsl.DslSyntaxIrregularDecimalException;
import neatlogic.framework.cmdb.exception.dsl.DslSyntaxIrregularIntegerException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * dsl查询单元：a == 1或 a=1 && b=1或带括号的表达式都算一个查询单元，查询单元将组成树形关系，方便生成最终的表达式
 */
public class SearchExpression {
    public enum Type {
        JOIN, EXPRESSION
    }

    public enum ValueType {
        NUMBER, STRING, NUMBER_ARRAY, STRING_ARRAY, CALCULATE
    }

    private Type type;
    private String attr;
    private String value;
    private ValueType valueType;
    private String logicalOperator;
    private String comparisonOperator;
    private final List<SearchExpression> children = new ArrayList<>();
    private SearchExpression parent;
    //如果是join类型才会有leftExpression和rightExpression
    private SearchExpression leftExpression;
    private SearchExpression rightExpression;
    private CalculateExpression calculateExpression;

    public SearchExpression(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getAttr() {
        return attr;
    }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public CalculateExpression getCalculateExpression() {
        return calculateExpression;
    }

    public void setCalculateExpression(CalculateExpression calculateExpression) {
        this.calculateExpression = calculateExpression;
    }

    public Expression getComparisonExpression() {
        switch (this.comparisonOperator) {
            case "==":
                return new EqualsTo();
            case ">":
                return new GreaterThan();
            case ">=":
                return new GreaterThanEquals();
            case "<":
                return new MinorThan();
            case "<=":
                return new MinorThanEquals();
            case "!=":
                return new NotEqualsTo();
            case "like":
                return new LikeExpression();
            case "not like":
                return new LikeExpression().withNot(true);
            case "include":
                return new InExpression();
            case "exclude":
                InExpression e = new InExpression();
                e.setNot(true);
                return e;
        }
        return null;
    }

    public Object getExpressionValue(Map<String, SearchItem> searchItemMap) {
        if (valueType == ValueType.NUMBER) {
            if (StringUtils.isNotBlank(value)) {
                if (value.contains(".")) {
                    try {
                        return new DoubleValue(value);
                    } catch (Exception ex) {
                        return new StringValue(value);
                    }
                } else {
                    try {
                        return new LongValue(value);
                    } catch (Exception ex) {
                        return new StringValue(value);
                    }
                }
            }
        } else if (valueType == ValueType.STRING) {
            return new StringValue(value);
        } else if (valueType == ValueType.NUMBER_ARRAY) {
            if (StringUtils.isNotBlank(value) && value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
                String[] vs = value.split(",");
                if (vs.length > 0) {
                    List<Expression> expressionList = new ArrayList<>();
                    for (String v : vs) {
                        if (v.contains(".")) {
                            try {
                                expressionList.add(new DoubleValue(v));
                            } catch (Exception ex) {
                                throw new DslSyntaxIrregularDecimalException(v);
                            }
                        } else {
                            try {
                                expressionList.add(new LongValue(v));
                            } catch (Exception ex) {
                                throw new DslSyntaxIrregularIntegerException(v);
                            }
                        }
                    }
                    return new ExpressionList(expressionList);
                }
            }
            return new ExpressionList();
        } else if (valueType == ValueType.STRING_ARRAY) {
            if (StringUtils.isNotBlank(value) && value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
                String[] vs = value.split(",");
                if (vs.length > 0) {
                    List<Expression> expressionList = new ArrayList<>();
                    for (String v : vs) {
                        if (v.startsWith("\"")) {
                            v = v.substring(1);
                        }
                        if (v.endsWith("\"")) {
                            v = v.substring(0, v.length() - 1);
                        }
                        expressionList.add(new StringValue(v));
                    }
                    return new ExpressionList(expressionList);
                }
            }
            return new ExpressionList();
        } else if (valueType == ValueType.CALCULATE) {
            return getCalculateExpression(calculateExpression, searchItemMap);
        }
        return null;
    }

    private Expression getCalculateExpression(CalculateExpression calculateExpression, Map<String, SearchItem> searchItemMap) {
        if (calculateExpression != null) {
            if (calculateExpression.getType() == CalculateExpression.Type.CALCULATE) {
                if (calculateExpression.getLeftExpression() != null && calculateExpression.getRightExpression() != null) {
                    switch (calculateExpression.getCalculateOperator()) {
                        case "+":
                            return new Addition()
                                    .withLeftExpression(getCalculateExpression(calculateExpression.getLeftExpression(), searchItemMap))
                                    .withRightExpression(getCalculateExpression(calculateExpression.getRightExpression(), searchItemMap));
                        case "-":
                            return new Subtraction()
                                    .withLeftExpression(getCalculateExpression(calculateExpression.getLeftExpression(), searchItemMap))
                                    .withRightExpression(getCalculateExpression(calculateExpression.getRightExpression(), searchItemMap));
                        case "*":
                            return new Multiplication()
                                    .withLeftExpression(getCalculateExpression(calculateExpression.getLeftExpression(), searchItemMap))
                                    .withRightExpression(getCalculateExpression(calculateExpression.getRightExpression(), searchItemMap));
                        case "/":
                            return new Division()
                                    .withLeftExpression(getCalculateExpression(calculateExpression.getLeftExpression(), searchItemMap))
                                    .withRightExpression(getCalculateExpression(calculateExpression.getRightExpression(), searchItemMap));
                    }
                } else if (calculateExpression.getParenthesisExpression() != null) {
                    return new Parenthesis().withExpression(getCalculateExpression(calculateExpression.getParenthesisExpression(), searchItemMap));
                }
            } else if (calculateExpression.getType() == CalculateExpression.Type.NUMBER) {
                if (StringUtils.isNotBlank(calculateExpression.getNumber())) {
                    if (calculateExpression.getNumber().contains(".")) {
                        return new DoubleValue(calculateExpression.getNumber());
                    } else {
                        return new LongValue(calculateExpression.getNumber());
                    }
                }
            } else if (calculateExpression.getType() == CalculateExpression.Type.ATTR) {
                return new Column().withColumnName(searchItemMap.get(calculateExpression.getAttrs()).getAlias());
            }
        }
        return null;
    }

    public void setValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            //去掉前后括号
            if (value.startsWith("\"")) {
                value = value.substring(1);
            }
            if (value.endsWith("\"")) {
                value = value.substring(0, value.length() - 1);
            }
            this.value = value;
        }

    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(String logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public List<SearchExpression> getChildren() {
        return children;
    }

    public void addChildren(SearchExpression children) {
        this.children.add(children);
        children.parent = this;
    }

    public SearchExpression getParent() {
        return parent;
    }

    public void setParent(SearchExpression parent) {
        this.parent = parent;
        parent.children.add(this);
    }

    public SearchExpression getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(SearchExpression leftExpression) {
        this.leftExpression = leftExpression;
    }

    public SearchExpression getRightExpression() {
        return rightExpression;
    }

    public void setRightExpression(SearchExpression rightExpression) {
        this.rightExpression = rightExpression;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }
}
