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
