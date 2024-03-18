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

import java.util.ArrayList;
import java.util.List;

/**
 * dsl计算单元
 */
public class CalculateExpression {
    public enum Type {
        NUMBER, ATTR, CALCULATE
    }

    private String calculateOperator;
    private final List<CalculateExpression> children = new ArrayList<>();
    private CalculateExpression parent;
    private final Type type;
    private String number;
    private String attrs;
    //括号中的表达式
    private CalculateExpression parenthesisExpression;
    //左表达式
    private CalculateExpression leftExpression;
    //右表达式
    private CalculateExpression rightExpression;

    public CalculateExpression(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getAttrs() {
        return attrs;
    }

    public void setAttrs(String attrs) {
        this.attrs = attrs;
    }

    public String getCalculateOperator() {
        return calculateOperator;
    }

    public void setCalculateOperator(String calculateOperator) {
        this.calculateOperator = calculateOperator;
    }

    public List<CalculateExpression> getChildren() {
        return children;
    }

    public CalculateExpression getParent() {
        return parent;
    }

    public void setParent(CalculateExpression parent) {
        this.parent = parent;
    }

    public CalculateExpression getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(CalculateExpression leftExpression) {
        this.leftExpression = leftExpression;
        leftExpression.parent = this;
    }

    public CalculateExpression getRightExpression() {
        return rightExpression;
    }

    public void setRightExpression(CalculateExpression rightExpression) {
        this.rightExpression = rightExpression;
        rightExpression.parent = this;
    }

    public CalculateExpression getParenthesisExpression() {
        return parenthesisExpression;
    }

    public void setParenthesisExpression(CalculateExpression parenthesisExpression) {
        this.parenthesisExpression = parenthesisExpression;
        parenthesisExpression.parent = this;
    }
}
