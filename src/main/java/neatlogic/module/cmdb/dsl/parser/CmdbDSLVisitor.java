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