/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dsl;

import codedriver.framework.cmdb.dsl.parser.CmdbDSLBaseVisitor;
import codedriver.framework.cmdb.dsl.parser.CmdbDSLParser;
import codedriver.framework.cmdb.exception.dsl.DslSyntaxIrregularException;

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
                throw new DslSyntaxIrregularException("数组比较只能使用include或exclude运算符");
            }
        } else if (ctx.calculateExpressions() != null) {
            if (ctx.comparisonOperator().EQ() == null && ctx.comparisonOperator().NOTEQ() == null && ctx.comparisonOperator().LE() == null && ctx.comparisonOperator().GE() == null && ctx.comparisonOperator().LT() == null && ctx.comparisonOperator().GT() == null) {
                throw new DslSyntaxIrregularException("计算比较只能使用==、!=、>、<、>=或<=运算符");
            }
        }
    }


    @Override
    public String visitCalculateExpressions(CmdbDSLParser.CalculateExpressionsContext ctx) {
        dslSearchManager.buildCalculateExpression(ctx);
        return visitChildren(ctx);
    }


}
