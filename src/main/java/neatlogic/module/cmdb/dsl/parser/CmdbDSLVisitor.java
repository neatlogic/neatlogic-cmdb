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