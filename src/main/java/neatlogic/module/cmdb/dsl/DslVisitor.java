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

package neatlogic.module.cmdb.dsl;

import neatlogic.framework.cmdb.exception.dsl.DslSyntaxIrregularArrayException;
import neatlogic.framework.cmdb.exception.dsl.DslSyntaxIrregularOperatorException;
import neatlogic.module.cmdb.dsl.parser.CmdbDSLBaseVisitor;
import neatlogic.module.cmdb.dsl.parser.CmdbDSLParser;

public class DslVisitor extends CmdbDSLBaseVisitor<String> {
    private final DslSearchManager dslSearchManager;

    public DslVisitor(DslSearchManager _dslSearchManager) {
        super();
        dslSearchManager = _dslSearchManager;
    }

    @Override
    public String visitAttrs(CmdbDSLParser.AttrsContext ctx) {
        String attrs = ctx.getText();
        dslSearchManager.buildSearchItem(attrs);
        return visitChildren(ctx);
    }


    @Override
    public String visitExpression(CmdbDSLParser.ExpressionContext ctx) {
        validExpression(ctx);
        dslSearchManager.buildSearchExpression(ctx);
        return visitChildren(ctx);
    }

    private void validExpression(CmdbDSLParser.ExpressionContext ctx) {
        if (ctx.STRING_ARRAY() != null || ctx.NUMBER_ARRAY() != null) {
            if (ctx.comparisonOperator().INCLUDE() == null && ctx.comparisonOperator().EXCLUDE() == null) {
                throw new DslSyntaxIrregularArrayException();
            }
        } else if (ctx.calculateExpressions() != null) {
            if (ctx.comparisonOperator().EQ() == null && ctx.comparisonOperator().NOTEQ() == null && ctx.comparisonOperator().LE() == null && ctx.comparisonOperator().GE() == null && ctx.comparisonOperator().LT() == null && ctx.comparisonOperator().GT() == null) {
                throw new DslSyntaxIrregularOperatorException();
            }
        }
    }


    @Override
    public String visitCalculateExpressions(CmdbDSLParser.CalculateExpressionsContext ctx) {
        dslSearchManager.buildCalculateExpression(ctx);
        return visitChildren(ctx);
    }


}
