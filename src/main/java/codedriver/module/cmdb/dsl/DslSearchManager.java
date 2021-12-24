/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dsl;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dsl.core.CalculateExpression;
import codedriver.framework.cmdb.dsl.core.SearchExpression;
import codedriver.framework.cmdb.dsl.core.SearchItem;
import codedriver.framework.cmdb.dsl.core.SelectFragment;
import codedriver.framework.cmdb.dsl.parser.CmdbDSLLexer;
import codedriver.framework.cmdb.dsl.parser.CmdbDSLParser;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.dsl.DslSyntaxIrregularException;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.service.ci.CiService;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DslSearchManager {
    private static CiService ciService;
    private static AttrMapper attrMapper;
    private static RelMapper relMapper;
    private String dsl;
    private Long ciId;
    private final Map<String, SearchItem> searchItemMap = new HashMap<>();
    private final Map<Integer, SearchExpression> searchExpressionMap = new HashMap<>();
    private final Map<Integer, CalculateExpression> calculateExpressionMap = new HashMap<>();

    @Autowired
    public DslSearchManager(CiService _ciService, AttrMapper _attrMapper, RelMapper _relMapper) {
        ciService = _ciService;
        attrMapper = _attrMapper;
        relMapper = _relMapper;
    }

    private DslSearchManager(Long ciId, String dsl) {
        this.dsl = dsl;
        this.ciId = ciId;
    }

    /**
     * 生成SQL的逻辑
     * 1、不带.的属性代表当前模型的属性，直接在当前模型的sql中查找即可。
     * 2、带.的属性代表其他模型的属性，需要分拆成多段去匹配，有可能是属性，有可能是关系，直到找到倒数第二位，再查询最后一位的属性。
     * 如果寻找过程中发现异常，则整个attr用SQL关键字：false代替，代表条件不符合。
     */
    public void buildSearchItem(String attrName) {
        //当前模型属性
        if (!attrName.contains(".")) {
            AttrVo attrVo = attrMapper.getAttrByCiIdAndName(this.ciId, attrName);
            if (attrVo == null) {
                throw new AttrNotFoundException(attrName);
            }
            this.searchItemMap.put(attrName, new SearchItem(ciId, attrVo));
        } else {
            String[] attrNames = attrName.split("\\.");
            Long currentCiId = ciId;
            List<SearchItem> searchItemList = new ArrayList<>();
            boolean isBreak = false;
            for (String name : attrNames) {
                AttrVo attrVo = attrMapper.getAttrByCiIdAndName(currentCiId, name);
                if (attrVo == null) {
                    RelVo relVo = relMapper.getRelByCiIdAndRelName(currentCiId, name);
                    if (relVo != null) {
                        SearchItem searchItem = new SearchItem(currentCiId, relVo);
                        if (searchItemList.size() > 0) {
                            searchItem.setPrev(searchItemList.get(searchItemList.size() - 1));
                        }
                        searchItemList.add(searchItem);
                        if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            currentCiId = relVo.getToCiId();
                        } else {
                            currentCiId = relVo.getFromCiId();
                        }
                    } else {
                        isBreak = true;
                        break;
                    }
                } else {
                    SearchItem searchItem = new SearchItem(currentCiId, attrVo);
                    if (searchItemList.size() > 0) {
                        searchItem.setPrev(searchItemList.get(searchItemList.size() - 1));
                    }
                    searchItemList.add(searchItem);
                    if (attrVo.getTargetCiId() != null) {
                        currentCiId = attrVo.getTargetCiId();
                    }
                }
            }
            if (!isBreak) {
                this.searchItemMap.put(attrName, searchItemList.get(searchItemList.size() - 1));
            }
        }
    }

    /**
     * 建立计算表达式之间的关系，建立计算表达式和比较表达式之间的关系
     * 此方法利用了DslVisitor的遍历顺序，buildCalculateExpression需要在buildSearchExpression执行完毕后才能执行，否则将无法建立计算表达式和比较表达式之间的关系
     */
    public void buildCalculateExpression(CmdbDSLParser.CalculateExpressionsContext ctx) {
        System.out.println(ctx.getText());
        CalculateExpression currentCalculateExpression = null;
        if (CollectionUtils.isNotEmpty(ctx.calculateExpressions())) {
            currentCalculateExpression = new CalculateExpression(CalculateExpression.Type.CALCULATE);
            if (ctx.PLUS() != null) {
                currentCalculateExpression.setCalculateOperator("+");
            } else if (ctx.SUBTRACT() != null) {
                currentCalculateExpression.setCalculateOperator("-");
            } else if (ctx.MULTIPLY() != null) {
                currentCalculateExpression.setCalculateOperator("*");
            } else if (ctx.DIVIDE() != null) {
                currentCalculateExpression.setCalculateOperator("/");
            }
            calculateExpressionMap.put(ctx.hashCode(), currentCalculateExpression);
        } else if (ctx.NUMBER() != null) {
            currentCalculateExpression = new CalculateExpression(CalculateExpression.Type.NUMBER);
            currentCalculateExpression.setNumber(ctx.NUMBER().getText());
            calculateExpressionMap.put(ctx.hashCode(), currentCalculateExpression);
        } else if (ctx.attrs() != null) {
            currentCalculateExpression = new CalculateExpression(CalculateExpression.Type.ATTR);
            currentCalculateExpression.setAttrs(ctx.attrs().getText());
            calculateExpressionMap.put(ctx.hashCode(), currentCalculateExpression);
        }
        if (currentCalculateExpression != null && ctx.getParent() != null) {
            if (ctx.getParent() instanceof CmdbDSLParser.CalculateExpressionsContext) {
                //建立计算表达式之间的关联关系
                CmdbDSLParser.CalculateExpressionsContext parent = (CmdbDSLParser.CalculateExpressionsContext) ctx.getParent();
                CalculateExpression parentCalculateExpression = calculateExpressionMap.get(parent.hashCode());
                if (parentCalculateExpression != null && CollectionUtils.isNotEmpty(parent.calculateExpressions())) {
                    if (parent.calculateExpressions().size() == 2) {
                        if (ctx == parent.calculateExpressions(0)) {
                            parentCalculateExpression.setLeftExpression(currentCalculateExpression);
                        } else if (ctx == parent.calculateExpressions(1)) {
                            parentCalculateExpression.setRightExpression(currentCalculateExpression);
                        }
                    } else if (parent.calculateExpressions().size() == 1 && parent.BRACKET_LEFT() != null && parent.BRACKET_RIGHT() != null) {
                        parentCalculateExpression.setParenthesisExpression(currentCalculateExpression);
                    }
                }
            } else if (ctx.getParent() instanceof CmdbDSLParser.ExpressionContext) {
                //建立计算表达试和比较比较式之间的关系
                SearchExpression searchExpression = searchExpressionMap.get(ctx.getParent().hashCode());
                if (searchExpression != null) {
                    searchExpression.setCalculateExpression(currentCalculateExpression);
                }
            }
        }
    }

    /**
     * 计算where条件表达式嵌套关系，解决括号优先级问题
     */
    public void buildSearchExpression(CmdbDSLParser.ExpressionContext ctx) {
        SearchExpression searchExpression = new SearchExpression(SearchExpression.Type.EXPRESSION);
        searchExpression.setAttr(ctx.attrs().getText());
        searchExpression.setComparisonOperator(ctx.comparisonOperator().getText());
        if (ctx.NUMBER() != null) {
            searchExpression.setValue(ctx.NUMBER().getText());
            searchExpression.setValueType(SearchExpression.ValueType.NUMBER);
        } else if (ctx.STRING() != null) {
            searchExpression.setValue(ctx.STRING().getText());
            searchExpression.setValueType(SearchExpression.ValueType.STRING);
        } else if (ctx.NUMBER_ARRAY() != null) {
            searchExpression.setValue(ctx.NUMBER_ARRAY().getText());
            searchExpression.setValueType(SearchExpression.ValueType.NUMBER_ARRAY);
        } else if (ctx.STRING_ARRAY() != null) {
            searchExpression.setValue(ctx.STRING_ARRAY().getText());
            searchExpression.setValueType(SearchExpression.ValueType.STRING_ARRAY);
        } else if (ctx.calculateExpressions() != null) {
            searchExpression.setValue(ctx.calculateExpressions().getText());
            searchExpression.setValueType(SearchExpression.ValueType.CALCULATE);
        }
        searchExpressionMap.put(ctx.hashCode(), searchExpression);
        RuleContext parentCtx = ctx.getParent();
        RuleContext currentCtx = ctx;
        SearchExpression currentSearchExpression = searchExpression;
        while (parentCtx != null) {
            //因为group节点下只会有一个子节点，所以不用处理，直接跳过，只需处理join的情况
            if (parentCtx instanceof CmdbDSLParser.ExpressionJoinContext) {
                CmdbDSLParser.ExpressionJoinContext joinCtx = (CmdbDSLParser.ExpressionJoinContext) parentCtx;
                SearchExpression parentExpression;
                if (searchExpressionMap.containsKey(joinCtx.hashCode())) {
                    parentExpression = searchExpressionMap.get(joinCtx.hashCode());
                } else {
                    parentExpression = new SearchExpression(SearchExpression.Type.JOIN);
                    parentExpression.setLogicalOperator(joinCtx.logicalOperator().getText());
                    searchExpressionMap.put(joinCtx.hashCode(), parentExpression);
                }
                currentSearchExpression.setParent(parentExpression);
                if (currentCtx == joinCtx.expressions().get(0)) {
                    parentExpression.setLeftExpression(currentSearchExpression);
                } else if (currentCtx == joinCtx.expressions().get(1)) {
                    parentExpression.setRightExpression(currentSearchExpression);
                }
                currentSearchExpression = parentExpression;
            }
            currentCtx = parentCtx;
            parentCtx = parentCtx.getParent();
        }
    }

    public static DslSearchManager build(Long ciId, String dsl) {
        return new DslSearchManager(ciId, dsl);
    }


    public DslSearchManager search() {
        CharStream input = CharStreams.fromString(dsl);
        CmdbDSLLexer lexer = new CmdbDSLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CmdbDSLParser parser = new CmdbDSLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object o, int row, int col, String s, RecognitionException e) {
                throw new DslSyntaxIrregularException(row, col, s);
            }
        });
        ParseTree tree = parser.expressions();
        DslVisitor visitor = new DslVisitor(this);
        visitor.visit(tree);
        return this;
    }

    public String getSql() {
        return this.buildSql();
    }

    //保存模型的子查询SQL
    private final Map<String, SelectFragment> ciSelectMap = new HashMap<>();

    private SelectFragment buildSelectFragment(SearchItem searchItem) {
        Table mainTable = new Table();
        mainTable.setSchemaName(TenantContext.get().getDbName());
        mainTable.setName("cmdb_cientity");
        mainTable.setAlias(new Alias("ci_base"));
        Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
        SelectBody selectBody = select.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("ci_base"))));
        /* 不能加条件，因为关系有可能只到抽象模型
        plainSelect.withWhere(new EqualsTo()
                .withLeftExpression(new Column()
                        .withTable(new Table("ci_base"))
                        .withColumnName("ci_id"))
                .withRightExpression(new LongValue(searchItem.getCiId())));*/
        SelectFragment selectFragment = new SelectFragment(select);
        selectFragment.setPrevItemList(searchItem.getPrevItemList());
        return selectFragment;
    }

    /**
     * 根据属性情况补充需要join的表
     */
    private void supplyJoinForSelectFragment(SelectFragment selectFragment, SearchItem searchItem) {
        if (searchItem.getAttrVo() != null && !selectFragment.isAttrExists(searchItem.getAttrVo().getId())) {
            selectFragment.addAttrToCheckSet(searchItem.getAttrVo().getId());
            SelectBody selectBody = selectFragment.getSelect().getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;
            //普通属性直接join对应的cmdb_xxx表
            if (searchItem.getAttrVo() != null && searchItem.getAttrVo().getTargetCiId() == null) {
                CiVo currentVo = ciService.getCiById(searchItem.getCiId());
                for (CiVo ci : currentVo.getUpwardCiList()) {
                    if (searchItem.getAttrVo().getCiId().equals(ci.getId())) {
                        plainSelect.addSelectItems(new SelectExpressionItem(new Column(searchItem.getAttrVo().getId().toString()).withTable(new Table("cmdb_" + ci.getId()))).withAlias(new Alias(searchItem.getAlias())));
                    }
                    //只需要join关系所在的模型和叶子模型，中间模型可以跳过，尽量减少没必要的join
                    if (!selectFragment.isCiExists(ci.getId()) && (searchItem.getAttrVo().getCiId().equals(ci.getId()) || ci.getId().equals(searchItem.getCiId()))) {
                        selectFragment.addCiToCheckSet(ci.getId());
                        plainSelect.addJoins(new Join().withRightItem(new Table().withName("cmdb_" + ci.getId()).withSchemaName(TenantContext.get().getDataDbName()).withAlias(new Alias("cmdb_" + ci.getId()))).withOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("cmdb_" + ci.getId())).withColumnName("cientity_id"))));
                    }
                }
            } else if (searchItem.getAttrVo() != null && searchItem.getAttrVo().getTargetCiId() != null) {
                plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("target_cientity"))).withAlias(new Alias(searchItem.getAlias())));
                plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_attrentity").withAlias(new Alias("cmdb_attrentity"))).withOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("cmdb_attrentity")).withColumnName("attr_id")).withRightExpression(new LongValue(searchItem.getAttrVo().getId()))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("cmdb_attrentity")).withColumnName("from_cientity_id"))))).addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_cientity").withAlias(new Alias("target_cientity"))).withOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("cmdb_attrentity")).withColumnName("to_cientity_id")).withRightExpression(new Column().withTable(new Table("target_cientity")).withColumnName("id"))));
            } else if (searchItem.getRelVo() != null) {
                plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("target_cientity"))).withAlias(new Alias(searchItem.getAlias())));
                plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_relentity").withAlias(new Alias("cmdb_relentity"))).withOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("cmdb_relentity")).withColumnName("rel_id")).withRightExpression(new LongValue(searchItem.getRelVo().getId()))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("cmdb_relentity")).withColumnName("from_cientity_id"))))).addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_cientity").withAlias(new Alias("target_cientity"))).withOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("cmdb_relentity")).withColumnName("to_cientity_id")).withRightExpression(new Column().withTable(new Table("target_cientity")).withColumnName("id"))));
            }
        }
    }

    /**
     * 创建完整的SQL语句
     */
    private String buildSql() {
        Table mainTable = new Table();
        mainTable.setSchemaName(TenantContext.get().getDbName());
        mainTable.setName("cmdb_cientity");
        mainTable.setAlias(new Alias("ci_base"));
        Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
        SelectBody selectBody = select.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;
        // plainSelect.withDistinct(new Distinct().addOnSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base")))));
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));


        if (MapUtils.isNotEmpty(this.searchItemMap)) {
            for (String key : this.searchItemMap.keySet()) {
                SearchItem searchItem = this.searchItemMap.get(key);
                if (!this.ciSelectMap.containsKey(searchItem.getCiPath())) {
                    this.ciSelectMap.put(searchItem.getCiPath(), buildSelectFragment(searchItem));
                }
                //检查是否需要补充属性，如果是关系，则直接使用配置项名称作为返回值
                SelectFragment selectFragment = this.ciSelectMap.get(searchItem.getCiPath());
                if (searchItem.getAttrVo() != null) {
                    this.supplyJoinForSelectFragment(selectFragment, searchItem);
                }
            }
            for (String key : this.ciSelectMap.keySet()) {
                SelectFragment selectFragment = this.ciSelectMap.get(key);
                //如果前面有属性或关系，则需要额外生成cmdb_attrentity或cmdb_relentity的join语句
                String prevTable = "ci_base";
                String prevColumn = "id";
                if (CollectionUtils.isNotEmpty(selectFragment.getPrevItemList())) {
                    for (SearchItem prev : selectFragment.getPrevItemList()) {
                        if (prev.getAttrVo() != null) {
                            Join join = new Join();
                            join.withRightItem(new Table().withName("cmdb_attrentity").withAlias(new Alias(prev.getTableAlias()))).withLeft(true).addOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table(prevTable)).withColumnName(prevColumn)).withRightExpression(new Column().withTable(new Table(prev.getTableAlias())).withColumnName("from_cientity_id"))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table(prev.getTableAlias())).withColumnName("attr_id")).withRightExpression(new LongValue(prev.getAttrVo().getId()))));
                            plainSelect.addJoins(join);
                            prevTable = prev.getTableAlias();
                            prevColumn = "to_cientity_id";
                        } else if (prev.getRelVo() != null) {
                            Join join = new Join();
                            join.withRightItem(new Table().withName("cmdb_relentity").withAlias(new Alias(prev.getTableAlias()))).withLeft(true).addOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table(prevTable)).withColumnName(prevColumn)).withRightExpression(new Column().withTable(new Table(prev.getTableAlias())).withColumnName(prev.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "from_cientity_id" : "to_cientity_id"))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table(prev.getTableAlias())).withColumnName("rel_id")).withRightExpression(new LongValue(prev.getRelVo().getId()))));
                            plainSelect.addJoins(join);
                            prevTable = prev.getTableAlias();
                            prevColumn = prev.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "to_cientity_id" : "from_cientity_id";
                        }

                    }
                }
                Join join = new Join();
                join.withRightItem(new SubSelect().withSelectBody(selectFragment.getSelect().getSelectBody()).withAlias(new Alias(selectFragment.getAlias()))).withLeft(true).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table(selectFragment.getAlias())).withColumnName("id")).withRightExpression(new Column().withTable(new Table(prevTable)).withColumnName(prevColumn)));
                plainSelect.addJoins(join);
            }
        }


        SearchExpression rootSearchExpression = null;
        if (MapUtils.isNotEmpty(searchExpressionMap)) {
            for (Integer key : searchExpressionMap.keySet()) {
                rootSearchExpression = searchExpressionMap.get(key);
                if (rootSearchExpression.getParent() == null) {
                    break;
                }
            }
        }
        if (rootSearchExpression != null) {
            Expression expression = buildWhereExpression(rootSearchExpression);
            if (expression != null) {
                plainSelect.withWhere(expression);
            }
        }
        Expression where = plainSelect.getWhere();
        if (where != null) {
            //增加模型id作为首要过滤条件
            plainSelect.withWhere(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("ci_id")).withRightExpression(new LongValue(ciId))).withRightExpression(where));
        } else {
            plainSelect.withWhere(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("ci_id")).withRightExpression(new LongValue(ciId)));
        }


        return select.toString();
    }

    /*
    生成where条件，目前支持=,>,<,>=,<=,like,in,not in
     */
    private Expression buildWhereExpression(SearchExpression searchExpression) {
        if (searchExpression.getType() == SearchExpression.Type.EXPRESSION) {
            SearchItem searchItem = searchItemMap.get(searchExpression.getAttr());
            if (searchItem != null) {
                Expression expression = searchExpression.getComparisonExpression();
                if (expression != null) {
                    if (expression instanceof ComparisonOperator) {
                        Object expressionValue = searchExpression.getExpressionValue(searchItemMap);
                        if (expressionValue instanceof Expression) {
                            ((ComparisonOperator) expression).withLeftExpression(new Column().withColumnName(searchItem.getAlias())).withRightExpression((Expression) expressionValue);
                        }
                    } else if (expression instanceof InExpression) {
                        Object expressionValue = searchExpression.getExpressionValue(searchItemMap);
                        if (expressionValue instanceof ItemsList) {
                            ((InExpression) expression).withLeftExpression(new Column().withColumnName(searchItem.getAlias())).withRightItemsList((ItemsList) expressionValue);
                        }
                    }
                    return expression;
                }
            }
        } else if (searchExpression.getType() == SearchExpression.Type.JOIN) {
            Expression expression = null;
            //所有join都要增加括号包裹
            if (searchExpression.getLogicalOperator().equals("&&")) {
                expression = new Parenthesis(new AndExpression().withLeftExpression(buildWhereExpression(searchExpression.getLeftExpression())).withRightExpression(buildWhereExpression(searchExpression.getRightExpression())));
            } else if (searchExpression.getLogicalOperator().equals("||")) {
                expression = new Parenthesis(new OrExpression().withLeftExpression(buildWhereExpression(searchExpression.getLeftExpression())).withRightExpression(buildWhereExpression(searchExpression.getRightExpression())));
            }
            return expression;
        }
        return null;
    }

}
