/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.customview.*;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.customview.JoinType;
import codedriver.framework.cmdb.enums.customview.RelType;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.rel.RelNotFoundException;
import codedriver.module.cmdb.service.ci.CiService;
import codedriver.module.cmdb.service.customview.CustomViewService;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomViewBuilder {
    private final static Logger logger = LoggerFactory.getLogger(CustomViewBuilder.class);

    private static CiService ciService;
    private static CustomViewService customViewService;
    private CustomViewVo customViewVo;

    @Autowired
    public CustomViewBuilder(CiService _ciService, CustomViewService _customViewService) {
        ciService = _ciService;
        customViewService = _customViewService;
    }

    public CustomViewBuilder(CustomViewVo _customViewVo) {
        customViewVo = _customViewVo;
        Map<Long, CiVo> ciMap = new HashMap<>();
        for (CustomViewCiVo customViewCiVo : customViewVo.getCiList()) {
            CiVo ciVo = ciMap.get(customViewCiVo.getCiId());
            if (ciVo == null) {
                ciVo = ciService.getCiById(customViewCiVo.getCiId());
                if (ciVo == null) {
                    throw new CiNotFoundException(customViewCiVo.getCiId());
                }
                ciMap.put(customViewCiVo.getCiId(), ciVo);
            }
            if (CollectionUtils.isNotEmpty(customViewCiVo.getAttrList())) {
                for (CustomViewAttrVo customViewAttrVo : customViewCiVo.getAttrList()) {
                    AttrVo attrVo = ciVo.getAttrById(customViewAttrVo.getAttrId());
                    if (attrVo == null) {
                        throw new AttrNotFoundException(customViewAttrVo.getAttrId());
                    }
                    customViewAttrVo.setAttrVo(attrVo);
                }
            }

            if (CollectionUtils.isNotEmpty(customViewCiVo.getRelList())) {
                for (CustomViewRelVo customViewRelVo : customViewCiVo.getRelList()) {
                    RelVo relVo = ciVo.getRelById(customViewRelVo.getRelId());
                    if (relVo == null) {
                        throw new RelNotFoundException(customViewRelVo.getRelId());
                    }
                    customViewRelVo.setRelVo(relVo);
                }
            }
            customViewCiVo.setCiVo(ciVo);
        }

    }

    public void buildView() {
        Table mainTable = new Table();
        mainTable.setSchemaName(TenantContext.get().getDbName());
        mainTable.setName("cmdb_cientity");
        mainTable.setAlias(new Alias("ci_base"));
        Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
        SelectBody selectBody = select.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("id")
                .withTable(new Table("ci_base"))));
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("name")
                .withTable(new Table("ci_base"))));
        for (CustomViewCiVo ciVo : customViewVo.getCiList()) {
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("`id`")
                    .withTable(new Table("ci_" + ciVo.getUuid())))
                    .withAlias(new Alias("`" + ciVo.getUuid() + "_id`")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("`name`")
                    .withTable(new Table("ci_" + ciVo.getUuid())))
                    .withAlias(new Alias("`" + ciVo.getUuid() + "_name`")));
        }
        CustomViewCiVo startCustomViewCiVo = customViewVo.getStartCustomViewCi();
        plainSelect.addJoins(new Join()
                .withRightItem(new SubSelect()
                        .withSelectBody(buildSubSelectForCi(startCustomViewCiVo).getSelectBody())
                        .withAlias(new Alias("ci_" + startCustomViewCiVo.getUuid())))
                .withOnExpression(new EqualsTo()
                        .withLeftExpression(new Column()
                                .withTable(new Table("ci_base"))
                                .withColumnName("id"))
                        .withRightExpression(new Column()
                                .withTable(new Table("ci_" + startCustomViewCiVo.getUuid()))
                                .withColumnName("id"))));
        if (CollectionUtils.isNotEmpty(customViewVo.getAttrList())) {
            for (CustomViewAttrVo attrVo : customViewVo.getAttrList()) {
                plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getUuid() + "`")));
                plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getUuid() + "_hash`")));
            }

            //记录哪些表已经创建，如果已经创建则可以直接join
            Map<String, JoinWrapper> joinMap = new HashMap<>();
            //记录是否已经产生过join
            Set<String> linkSet = new HashSet<>();
            //先创建驱动表


            List<CustomViewLinkVo> linkList = customViewVo.getLinkListByFromCustomCiUuid(startCustomViewCiVo.getUuid());
            while (CollectionUtils.isNotEmpty(linkList)) {
                Set<CustomViewLinkVo> nextLinkList = new HashSet<>();
                for (CustomViewLinkVo linkVo : linkList) {
                    CustomViewCiVo customViewCiVo = customViewVo.getCustomCiByUuid(linkVo.getToCustomViewCiUuid());
                    if (linkVo.getFromType().equals(RelType.ATTR.getValue()) && linkVo.getToType().equals(RelType.ATTR.getValue())) {
                        if (!joinMap.containsKey(linkVo.getToCustomViewCiUuid())) {
                            Join join = new Join();
                            joinMap.put(linkVo.getToCustomViewCiUuid(), new JoinWrapper(join, joinMap.size()));

                            if (linkVo.getJoinType().equalsIgnoreCase(JoinType.LEFTJOIN.getValue())) {
                                join.withLeft(true);
                            }
                            join.withRightItem(new SubSelect()
                                    .withSelectBody(buildSubSelectForCi(customViewCiVo).getSelectBody())
                                    .withAlias(new Alias("ci_" + customViewCiVo.getUuid())))
                                    .withOnExpression(new EqualsTo()
                                            .withLeftExpression(new Column()
                                                    .withTable(new Table("ci_" + linkVo.getFromCustomViewCiUuid()))
                                                    .withColumnName(linkVo.getFromUuid() + "_hash"))
                                            .withRightExpression(new Column().withTable(new Table("ci_" + linkVo.getToCustomViewCiUuid()))
                                                    .withColumnName(linkVo.getToUuid() + "_hash")));
                            plainSelect.addJoins(join);
                        } else {
                            JoinWrapper toJoinWrapper = joinMap.get(linkVo.getToCustomViewCiUuid());
                            JoinWrapper fromJoinWrapper = joinMap.get(linkVo.getFromCustomViewCiUuid());
                            Join join = (fromJoinWrapper != null && toJoinWrapper.getIndex() < fromJoinWrapper.getIndex()) ? fromJoinWrapper.getJoin() : toJoinWrapper.getJoin();
                            Expression oldExpression = join.getOnExpression();
                            join.withOnExpression(new AndExpression()
                                    .withLeftExpression(oldExpression)
                                    .withRightExpression(new EqualsTo()
                                            .withLeftExpression(new Column()
                                                    .withTable(new Table("ci_" + linkVo.getFromCustomViewCiUuid()))
                                                    .withColumnName(linkVo.getFromUuid() + "_hash"))
                                            .withRightExpression(new Column().withTable(new Table("ci_" + linkVo.getToCustomViewCiUuid()))
                                                    .withColumnName(linkVo.getToUuid() + "_hash"))));

                        }
                    } else if (linkVo.getFromType().equals(RelType.REL.getValue()) && linkVo.getToType().equals(RelType.CI.getValue())) {
                        CustomViewRelVo customViewRelVo = customViewVo.getCustomCiByUuid(linkVo.getFromCustomViewCiUuid()).getRelByUuid(linkVo.getFromUuid());
                        if (customViewRelVo != null) {
                            Join join = new Join();
                            if (linkVo.getJoinType().equalsIgnoreCase(JoinType.LEFTJOIN.getValue())) {
                                join.withLeft(true);
                            }
                            join.withRightItem(new Table().withName("cmdb_relentity")
                                    .withSchemaName(TenantContext.get().getDbName())
                                    .withAlias(new Alias("rel_" + linkVo.getUuid())))
                                    .withOnExpression(new AndExpression()
                                            .withLeftExpression(new EqualsTo()
                                                    .withLeftExpression(new Column()
                                                            .withTable(new Table("ci_" + linkVo.getFromCustomViewCiUuid()))
                                                            .withColumnName("id"))
                                                    .withRightExpression(new Column()
                                                            .withTable(new Table("rel_" + linkVo.getUuid()))
                                                            .withColumnName(customViewRelVo.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "from_cientity_id" : "to_cientity_id")))
                                            .withRightExpression(new EqualsTo()
                                                    .withLeftExpression(new Column()
                                                            .withTable(new Table("rel_" + linkVo.getUuid()))
                                                            .withColumnName("rel_id"))
                                                    .withRightExpression(new LongValue(customViewRelVo.getRelVo().getId()))));
                            plainSelect.addJoins(join);

                            if (!joinMap.containsKey(linkVo.getToCustomViewCiUuid())) {
                                //关联目标模型
                                Join join2 = new Join();
                                joinMap.put(linkVo.getToCustomViewCiUuid(), new JoinWrapper(join2, joinMap.size()));

                                if (linkVo.getJoinType().equalsIgnoreCase(JoinType.LEFTJOIN.getValue())) {
                                    join2.withLeft(true);
                                }
                                join2.withRightItem(new SubSelect()
                                        .withSelectBody(buildSubSelectForCi(customViewCiVo).getSelectBody())
                                        .withAlias(new Alias("ci_" + customViewCiVo.getUuid())))
                                        .withOnExpression(new EqualsTo()
                                                .withLeftExpression(new Column()
                                                        .withTable(new Table("rel_" + linkVo.getUuid()))
                                                        .withColumnName(customViewRelVo.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "to_cientity_id" : "from_cientity_id"))
                                                .withRightExpression(new Column().withTable(new Table("ci_" + customViewCiVo.getUuid()))
                                                        .withColumnName("id")));
                                plainSelect.addJoins(join2);
                            } else {
                                JoinWrapper toJoinWrapper = joinMap.get(linkVo.getToCustomViewCiUuid());
                                JoinWrapper fromJoinWrapper = joinMap.get(linkVo.getFromCustomViewCiUuid());
                                Join join2 = (fromJoinWrapper != null && toJoinWrapper.getIndex() < fromJoinWrapper.getIndex()) ? fromJoinWrapper.getJoin() : toJoinWrapper.getJoin();
                                Expression oldExpression = join2.getOnExpression();
                                join2.withOnExpression(new AndExpression()
                                        .withLeftExpression(oldExpression)
                                        .withRightExpression(new EqualsTo()
                                                .withLeftExpression(new Column()
                                                        .withTable(new Table("rel_" + linkVo.getUuid()))
                                                        .withColumnName(customViewRelVo.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "to_cientity_id" : "from_cientity_id"))
                                                .withRightExpression(new Column().withTable(new Table("ci_" + customViewCiVo.getUuid()))
                                                        .withColumnName("id"))));
                            }
                        }
                    }

                    linkSet.add(linkVo.getFromUuid() + "-" + linkVo.getToUuid());

                    //获取下一个节点的关系列表
                    CustomViewCiVo toCustomViewCiVo = customViewVo.getCustomCiByUuid(linkVo.getToCustomViewCiUuid());
                    nextLinkList.addAll(customViewVo.getLinkListByFromCustomCiUuid(toCustomViewCiVo.getUuid()));

                }
                //排除已经处理过的关系
                nextLinkList.removeIf(link -> linkSet.contains(link.getFromUuid() + "-" + link.getToUuid()));
                linkList = new ArrayList<>(nextLinkList);
            }
            /*
            GroupByElement groupBy = new GroupByElement();
            groupBy.addGroupByExpression(new Column("id").withTable(new Table("ci_base")));
            plainSelect.setGroupByElement(groupBy);
            */
        }
        String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + ".customview_" + customViewVo.getId() + " AS " + select;
        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }
        //System.out.println(select);
        customViewService.buildCustomView(sql);
    }

    static class JoinWrapper {
        private final Join join;
        private final int index;

        public JoinWrapper(Join join, int index) {
            this.join = join;
            this.index = index;
        }

        public Join getJoin() {
            return join;
        }


        public int getIndex() {
            return index;
        }

    }

    private Select buildSubSelectForCi(CustomViewCiVo customViewCiVo) {
        CiVo ciVo = customViewCiVo.getCiVo();
        if (CollectionUtils.isNotEmpty(ciVo.getUpwardCiList())) {
            Table mainTable = new Table();
            mainTable.setSchemaName(TenantContext.get().getDbName());
            mainTable.setName("cmdb_cientity");
            mainTable.setAlias(new Alias("ci_base"));
            Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
            SelectBody selectBody = select.getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("ci_base"))));
            for (CustomViewAttrVo viewAttrVo : customViewCiVo.getAttrList()) {
                AttrVo attrVo = viewAttrVo.getAttrVo();
                if (attrVo.getTargetCiId() == null) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "`")
                            .withTable(new Table("cmdb_" + attrVo.getCiId())))
                            .withAlias(new Alias("`" + viewAttrVo.getUuid() + "`")));
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "_hash`")
                            .withTable(new Table("cmdb_" + attrVo.getCiId())))
                            .withAlias(new Alias("`" + viewAttrVo.getUuid() + "_hash`")));
                } else {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("name")
                            .withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())))
                            .withAlias(new Alias("`" + viewAttrVo.getUuid() + "`")));
                    Function function = new Function();
                    function.setName("md5");
                    ExpressionList expressionList = new ExpressionList();
                    expressionList.addExpressions(new Column("id")
                            .withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())));
                    function.setParameters(expressionList);
                    plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewAttrVo.getUuid() + "_hash`")));


                    plainSelect.addJoins(new Join()
                            .withLeft(true)
                            .withRightItem(new Table()
                                    .withName("cmdb_attrentity")
                                    .withSchemaName(TenantContext.get().getDbName())
                                    .withAlias(new Alias("attr_" + viewAttrVo.getUuid())))
                            .withOnExpression(new AndExpression()
                                    .withLeftExpression(new EqualsTo()
                                            .withLeftExpression(new Column()
                                                    .withTable(new Table("ci_base"))
                                                    .withColumnName("id"))
                                            .withRightExpression(new Column()
                                                    .withTable(new Table("attr_" + viewAttrVo.getUuid()))
                                                    .withColumnName("from_cientity_id")))
                                    .withRightExpression(new EqualsTo()
                                            .withLeftExpression(new Column()
                                                    .withTable(new Table("attr_" + viewAttrVo.getUuid()))
                                                    .withColumnName("attr_id"))
                                            .withRightExpression(new LongValue(attrVo.getId())))));
                    plainSelect.addJoins(new Join()
                            .withLeft(true)
                            .withRightItem(new Table()
                                    .withName("cmdb_cientity")
                                    .withSchemaName(TenantContext.get().getDbName())
                                    .withAlias(new Alias("attr_cientity_" + viewAttrVo.getUuid())))
                            .withOnExpression(new EqualsTo()
                                    .withLeftExpression(new Column()
                                            .withTable(new Table("attr_" + viewAttrVo.getUuid()))
                                            .withColumnName("to_cientity_id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table("attr_cientity_" + viewAttrVo.getUuid()))
                                            .withColumnName("id"))));
                }
            }
            //生成主SQL，需要join所有父模型数据表
            for (CiVo ci : ciVo.getUpwardCiList()) {
                plainSelect.addJoins(new Join()
                        .withRightItem(new Table()
                                .withName("cmdb_" + ci.getId())
                                .withSchemaName(TenantContext.get().getDataDbName())
                                .withAlias(new Alias("cmdb_" + ci.getId())))
                        .withOnExpression(new EqualsTo()
                                .withLeftExpression(new Column()
                                        .withTable(new Table("ci_base"))
                                        .withColumnName("id"))
                                .withRightExpression(new Column()
                                        .withTable(new Table("cmdb_" + ci.getId()))
                                        .withColumnName("cientity_id"))));

            }
            return select;
        } else {
            Table mainTable = new Table();
            mainTable.setSchemaName(TenantContext.get().getDataDbName());
            mainTable.setName("cmdb_" + ciVo.getId());
            return SelectUtils.buildSelectFromTable(mainTable);
        }
    }

}
