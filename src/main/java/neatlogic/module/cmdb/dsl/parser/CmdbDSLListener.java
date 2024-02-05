/*
 * Copyright(c) 2024 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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