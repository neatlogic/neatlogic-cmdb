/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
