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

package neatlogic.module.cmdb.constvalue.matrix;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.matrix.constvalue.IMatrixAttributeType;
import neatlogic.framework.util.$;

import java.util.Collections;
import java.util.List;

public enum MatrixAttributeType implements IMatrixAttributeType {
    CMDBCI("cmdbci", "cmdb.ci",8, Collections.singletonList(Expression.INCLUDE), Expression.INCLUDE);

    private final String value;
    private final String text;
    private final int sort;
    private final List<Expression> expressionList;
    private final Expression expression;

    private MatrixAttributeType(String value, String text,Integer sort, List<Expression> expressionList, Expression expression) {
        this.value = value;
        this.text = text;
        this.sort = sort;
        this.expressionList = expressionList;
        this.expression = expression;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return $.t(text);
    }

    public int getSort() {
        return sort;
    }

    public List<Expression> getExpressionList() {
        return expressionList;
    }

    public Expression getExpression() {
        return expression;
    }

    public static List<Expression> getExpressionList(String _value) {
        for (MatrixAttributeType s : MatrixAttributeType.values()) {
            if (s.getValue().equals(_value)) {
                return s.getExpressionList();
            }
        }
        return null;
    }

    public static Expression getExpression(String _value) {
        for (MatrixAttributeType s : MatrixAttributeType.values()) {
            if (s.getValue().equals(_value)) {
                return s.getExpression();
            }
        }
        return null;
    }


    @Override
    public List getValueTextList() {
        JSONArray array = new JSONArray();
        for (MatrixAttributeType type : MatrixAttributeType.values()) {
            array.add(new JSONObject() {
                {
                    this.put("value", type.getValue());
                    this.put("text", type.getText());
                    this.put("sort", type.getSort());
                }
            });
        }
        return array;
    }
}
