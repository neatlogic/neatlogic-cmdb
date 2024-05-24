/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
