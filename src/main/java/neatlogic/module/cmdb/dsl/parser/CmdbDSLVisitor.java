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

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CmdbDSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface CmdbDSLVisitor<T> extends ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by {@link CmdbDSLParser#calculateExpressions}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitCalculateExpressions(CmdbDSLParser.CalculateExpressionsContext ctx);

    /**
     * Visit a parse tree produced by the {@code expressionJoin}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitExpressionJoin(CmdbDSLParser.ExpressionJoinContext ctx);

    /**
     * Visit a parse tree produced by the {@code expression}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitExpression(CmdbDSLParser.ExpressionContext ctx);

    /**
     * Visit a parse tree produced by the {@code expressionGroup}
     * labeled alternative in {@link CmdbDSLParser#expressions}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitExpressionGroup(CmdbDSLParser.ExpressionGroupContext ctx);

    /**
     * Visit a parse tree produced by {@link CmdbDSLParser#attrs}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAttrs(CmdbDSLParser.AttrsContext ctx);

    /**
     * Visit a parse tree produced by {@link CmdbDSLParser#logicalOperator}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitLogicalOperator(CmdbDSLParser.LogicalOperatorContext ctx);

    /**
     * Visit a parse tree produced by {@link CmdbDSLParser#comparisonOperator}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitComparisonOperator(CmdbDSLParser.ComparisonOperatorContext ctx);

    /**
     * Visit a parse tree produced by {@link CmdbDSLParser#calculateOperator}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitCalculateOperator(CmdbDSLParser.CalculateOperatorContext ctx);
}