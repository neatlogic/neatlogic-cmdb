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

// Generated from /Users/chenqiwei/idea_project/codedriver/neatlogic-cmdb-base/src/main/resources/CmdbDSL.g4 by ANTLR 4.12.0

package neatlogic.module.cmdb.dsl.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CmdbDSLParser}.
 */
public interface CmdbDSLListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link CmdbDSLParser#calculateExpressions}.
     *
     * @param ctx the parse tree
     */
    void enterCalculateExpressions(CmdbDSLParser.CalculateExpressionsContext ctx);

    /**
     * Exit a parse tree produced by {@link CmdbDSLParser#calculateExpressions}.
     *
     * @param ctx the parse tree
     */
    void exitCalculateExpressions(CmdbDSLParser.CalculateExpressionsContext ctx);

    /**
     * Enter a parse tree produced by the {@code expressionJoin}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     */
    void enterExpressionJoin(CmdbDSLParser.ExpressionJoinContext ctx);

    /**
     * Exit a parse tree produced by the {@code expressionJoin}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     */
    void exitExpressionJoin(CmdbDSLParser.ExpressionJoinContext ctx);

    /**
     * Enter a parse tree produced by the {@code expression}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     */
    void enterExpression(CmdbDSLParser.ExpressionContext ctx);

    /**
     * Exit a parse tree produced by the {@code expression}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     */
    void exitExpression(CmdbDSLParser.ExpressionContext ctx);

    /**
     * Enter a parse tree produced by the {@code expressionGroup}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     */
    void enterExpressionGroup(CmdbDSLParser.ExpressionGroupContext ctx);

    /**
     * Exit a parse tree produced by the {@code expressionGroup}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     */
    void exitExpressionGroup(CmdbDSLParser.ExpressionGroupContext ctx);

    /**
     * Enter a parse tree produced by {@link CmdbDSLParser#attrs}.
     *
     * @param ctx the parse tree
     */
    void enterAttrs(CmdbDSLParser.AttrsContext ctx);

    /**
     * Exit a parse tree produced by {@link CmdbDSLParser#attrs}.
     *
     * @param ctx the parse tree
     */
    void exitAttrs(CmdbDSLParser.AttrsContext ctx);

    /**
     * Enter a parse tree produced by {@link CmdbDSLParser#logicalOperator}.
     *
     * @param ctx the parse tree
     */
    void enterLogicalOperator(CmdbDSLParser.LogicalOperatorContext ctx);

    /**
     * Exit a parse tree produced by {@link CmdbDSLParser#logicalOperator}.
     *
     * @param ctx the parse tree
     */
    void exitLogicalOperator(CmdbDSLParser.LogicalOperatorContext ctx);

    /**
     * Enter a parse tree produced by {@link CmdbDSLParser#comparisonOperator}.
     *
     * @param ctx the parse tree
     */
    void enterComparisonOperator(CmdbDSLParser.ComparisonOperatorContext ctx);

    /**
     * Exit a parse tree produced by {@link CmdbDSLParser#comparisonOperator}.
     *
     * @param ctx the parse tree
     */
    void exitComparisonOperator(CmdbDSLParser.ComparisonOperatorContext ctx);

    /**
     * Enter a parse tree produced by {@link CmdbDSLParser#calculateOperator}.
     *
     * @param ctx the parse tree
     */
    void enterCalculateOperator(CmdbDSLParser.CalculateOperatorContext ctx);

    /**
     * Exit a parse tree produced by {@link CmdbDSLParser#calculateOperator}.
     *
     * @param ctx the parse tree
     */
    void exitCalculateOperator(CmdbDSLParser.CalculateOperatorContext ctx);
}