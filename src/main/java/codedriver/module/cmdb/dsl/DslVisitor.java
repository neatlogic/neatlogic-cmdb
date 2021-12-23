/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dsl;

import codedriver.framework.cmdb.dsl.parser.CmdbDSLBaseVisitor;
import codedriver.framework.cmdb.dsl.parser.CmdbDSLParser;

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
        dslSearchManager.buildSearchExpression(ctx);
        return visitChildren(ctx);
    }
}
