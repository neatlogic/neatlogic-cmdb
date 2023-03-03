/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
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

package neatlogic.module.cmdb.utils;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.customview.JoinType;
import neatlogic.framework.cmdb.enums.customview.RelType;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.rel.RelNotFoundException;
import neatlogic.module.cmdb.service.ci.CiService;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
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
        CustomViewCiVo startCustomViewCiVo = customViewVo.getStartCustomViewCi();
        if (startCustomViewCiVo.getCiVo().getIsVirtual().equals(0)) {
            mainTable.setSchemaName(TenantContext.get().getDbName());
            mainTable.setName("cmdb_cientity");
            mainTable.setAlias(new Alias("ci_base"));
        } else {
            mainTable.setSchemaName(TenantContext.get().getDataDbName());
            mainTable.setName("cmdb_" + startCustomViewCiVo.getCiVo().getId());
            mainTable.setAlias(new Alias("ci_base"));
        }
        Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
        SelectBody selectBody = select.getSelectBody();
        PlainSelect plainSelect = (PlainSelect) selectBody;
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));
        plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("ci_base"))));
        for (CustomViewCiVo ciVo : customViewVo.getCiList()) {
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("`id`").withTable(new Table("ci_" + ciVo.getUuid()))).withAlias(new Alias("`" + ciVo.getUuid() + "_id`")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("`name`").withTable(new Table("ci_" + ciVo.getUuid()))).withAlias(new Alias("`" + ciVo.getUuid() + "_name`")));
            if (CollectionUtils.isNotEmpty(ciVo.getConstAttrList())) {
                for (CustomViewConstAttrVo constAttr : ciVo.getConstAttrList()) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + constAttr.getConstName() + "`").withTable(new Table("ci_" + ciVo.getUuid()))).withAlias(new Alias("`" + constAttr.getUuid() + "`")));
                }
            }
        }

        plainSelect.addJoins(new Join().withRightItem(new SubSelect().withSelectBody(buildSubSelectForCi(startCustomViewCiVo).getSelectBody()).withAlias(new Alias("ci_" + startCustomViewCiVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("ci_" + startCustomViewCiVo.getUuid())).withColumnName("id"))));
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
                if ((linkVo.getFromType().equals(RelType.ATTR.getValue()) || linkVo.getFromType().equals(RelType.CONST_ATTR.getValue()))
                        && (linkVo.getToType().equals(RelType.ATTR.getValue()) || linkVo.getToType().equals(RelType.CONST_ATTR.getValue()))) {
                    if (!joinMap.containsKey(linkVo.getToCustomViewCiUuid())) {
                        Join join = new Join();
                        joinMap.put(linkVo.getToCustomViewCiUuid(), new JoinWrapper(join, joinMap.size()));

                        if (linkVo.getJoinType().equalsIgnoreCase(JoinType.LEFTJOIN.getValue())) {
                            join.withLeft(true);
                        } else if (linkVo.getJoinType().equalsIgnoreCase(JoinType.RIGHTJOIN.getValue())) {
                            join.withRight(true);
                        }
                        join.withRightItem(new SubSelect().withSelectBody(buildSubSelectForCi(customViewCiVo).getSelectBody()).withAlias(new Alias("ci_" + customViewCiVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_" + linkVo.getFromCustomViewCiUuid())).withColumnName(linkVo.getFromUuid() + "_hash")).withRightExpression(new Column().withTable(new Table("ci_" + linkVo.getToCustomViewCiUuid())).withColumnName(linkVo.getToUuid() + "_hash")));
                        plainSelect.addJoins(join);
                    } else {
                        JoinWrapper toJoinWrapper = joinMap.get(linkVo.getToCustomViewCiUuid());
                        JoinWrapper fromJoinWrapper = joinMap.get(linkVo.getFromCustomViewCiUuid());
                        Join join = (fromJoinWrapper != null && toJoinWrapper.getIndex() < fromJoinWrapper.getIndex()) ? fromJoinWrapper.getJoin() : toJoinWrapper.getJoin();
                        Expression oldExpression = join.getOnExpression();
                        join.addOnExpression(new AndExpression().withLeftExpression(oldExpression).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_" + linkVo.getFromCustomViewCiUuid())).withColumnName(linkVo.getFromUuid() + "_hash")).withRightExpression(new Column().withTable(new Table("ci_" + linkVo.getToCustomViewCiUuid())).withColumnName(linkVo.getToUuid() + "_hash"))));

                    }
                } else if (linkVo.getFromType().equals(RelType.REL.getValue()) && linkVo.getToType().equals(RelType.CI.getValue())) {
                    CustomViewRelVo customViewRelVo = customViewVo.getCustomCiByUuid(linkVo.getFromCustomViewCiUuid()).getRelByUuid(linkVo.getFromUuid());
                    if (customViewRelVo != null) {
                        Join join = new Join();
                        if (linkVo.getJoinType().equalsIgnoreCase(JoinType.LEFTJOIN.getValue())) {
                            join.withLeft(true);
                        } else if (linkVo.getJoinType().equalsIgnoreCase(JoinType.RIGHTJOIN.getValue())) {
                            join.withRight(true);
                        }
                        join.withRightItem(new Table().withName("cmdb_relentity").withSchemaName(TenantContext.get().getDbName()).withAlias(new Alias("rel_" + linkVo.getUuid()))).addOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_" + linkVo.getFromCustomViewCiUuid())).withColumnName("id")).withRightExpression(new Column().withTable(new Table("rel_" + linkVo.getUuid())).withColumnName(customViewRelVo.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "from_cientity_id" : "to_cientity_id"))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("rel_" + linkVo.getUuid())).withColumnName("rel_id")).withRightExpression(new LongValue(customViewRelVo.getRelVo().getId()))));
                        plainSelect.addJoins(join);

                        if (!joinMap.containsKey(linkVo.getToCustomViewCiUuid())) {
                            //关联目标模型
                            Join join2 = new Join();
                            joinMap.put(linkVo.getToCustomViewCiUuid(), new JoinWrapper(join2, joinMap.size()));

                            if (linkVo.getJoinType().equalsIgnoreCase(JoinType.LEFTJOIN.getValue())) {
                                join2.withLeft(true);
                            } else if (linkVo.getJoinType().equalsIgnoreCase(JoinType.RIGHTJOIN.getValue())) {
                                join2.withRight(true);
                            }
                            join2.withRightItem(new SubSelect().withSelectBody(buildSubSelectForCi(customViewCiVo).getSelectBody()).withAlias(new Alias("ci_" + customViewCiVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("rel_" + linkVo.getUuid())).withColumnName(customViewRelVo.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "to_cientity_id" : "from_cientity_id")).withRightExpression(new Column().withTable(new Table("ci_" + customViewCiVo.getUuid())).withColumnName("id")));
                            plainSelect.addJoins(join2);
                        } else {
                            JoinWrapper toJoinWrapper = joinMap.get(linkVo.getToCustomViewCiUuid());
                            JoinWrapper fromJoinWrapper = joinMap.get(linkVo.getFromCustomViewCiUuid());
                            Join join2 = (fromJoinWrapper != null && toJoinWrapper.getIndex() < fromJoinWrapper.getIndex()) ? fromJoinWrapper.getJoin() : toJoinWrapper.getJoin();
                            Expression oldExpression = join2.getOnExpression();
                            join2.addOnExpression(new AndExpression().withLeftExpression(oldExpression).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("rel_" + linkVo.getUuid())).withColumnName(customViewRelVo.getRelVo().getDirection().equals(RelDirectionType.FROM.getValue()) ? "to_cientity_id" : "from_cientity_id")).withRightExpression(new Column().withTable(new Table("ci_" + customViewCiVo.getUuid())).withColumnName("id"))));
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
        String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + ".customview_" + customViewVo.getId() + " AS " + select;
        //System.out.println(sql);
        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }
        //System.out.println(sql);
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
            //plainSelect.addSelectItems(new SelectExpressionItem(new StringValue(ciVo.getName())).withAlias(new Alias("ciName")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("label").withTable(new Table("ci_info"))).withAlias(new Alias("ciName")));
            Function lcdFuc = new Function();
            lcdFuc.setName("DATE_FORMAT");
            ExpressionList lcdExpressionList = new ExpressionList();
            lcdExpressionList.addExpressions(new Column("lcd").withTable(new Table("ci_base")));
            lcdExpressionList.addExpressions(new StringValue("%Y-%m-%d"));
            lcdFuc.setParameters(lcdExpressionList);
            plainSelect.addSelectItems(new SelectExpressionItem(lcdFuc).withAlias(new Alias("lcd")));
            Function fcdFuc = new Function();
            fcdFuc.setName("DATE_FORMAT");
            ExpressionList fcdExpressionList = new ExpressionList();
            fcdExpressionList.addExpressions(new Column("fcd").withTable(new Table("ci_base")));
            fcdExpressionList.addExpressions(new StringValue("%Y-%m-%d"));
            fcdFuc.setParameters(fcdExpressionList);
            plainSelect.addSelectItems(new SelectExpressionItem(fcdFuc).withAlias(new Alias("fcd")));
            //plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("cmdb_ci"))).withAlias(new Alias("ciName")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("uuid").withTable(new Table("ci_base"))));

            plainSelect.addJoins(new Join()
                    .withRightItem(new Table()
                            .withName("cmdb_ci")
                            .withAlias(new Alias("ci_info"))
                            .withSchemaName(TenantContext.get().getDbName()))
                    .addOnExpression(new EqualsTo()
                            .withLeftExpression(new Column()
                                    .withTable(new Table("ci_base"))
                                    .withColumnName("ci_id"))
                            .withRightExpression(new Column()
                                    .withTable(new Table("ci_info"))
                                    .withColumnName("id"))));

            for (CustomViewConstAttrVo viewConstAttrVo : customViewCiVo.getConstAttrList()) {
                //由于内部属性来自不同的表，暂时先特殊处理
                if (viewConstAttrVo.getConstName().equalsIgnoreCase("ciName")) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("label").withTable(new Table("ci_info"))).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "`")));
                    //plainSelect.addSelectItems(new SelectExpressionItem(new StringValue(ciVo.getName())).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "`")));
                    Function function = new Function();
                    function.setName("md5");
                    ExpressionList expressionList = new ExpressionList();
                    //expressionList.addExpressions(new Column("name").withTable(new Table("cmdb_ci")));
                    expressionList.addExpressions(new StringValue(ciVo.getName()));
                    function.setParameters(expressionList);
                    plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "_hash`")));
                } else {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + viewConstAttrVo.getConstName() + "`").withTable(new Table("ci_base"))).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "`")));
                    Function function = new Function();
                    function.setName("md5");
                    ExpressionList expressionList = new ExpressionList();
                    expressionList.addExpressions(new Column("`" + viewConstAttrVo.getConstName() + "`").withTable(new Table("ci_base")));
                    function.setParameters(expressionList);
                    plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "_hash`")));
                }
            }

            for (CustomViewAttrVo viewAttrVo : customViewCiVo.getAttrList()) {
                AttrVo attrVo = viewAttrVo.getAttrVo();
                if (attrVo.getTargetCiId() == null) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "`").withTable(new Table("cmdb_" + attrVo.getCiId()))).withAlias(new Alias("`" + viewAttrVo.getUuid() + "`")));
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "_hash`").withTable(new Table("cmdb_" + attrVo.getCiId()))).withAlias(new Alias("`" + viewAttrVo.getUuid() + "_hash`")));
                } else {
                    CiVo targetCiVo = ciService.getCiById(attrVo.getTargetCiId());
                    if (targetCiVo != null) {
                        plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("attr_cientity_" + viewAttrVo.getUuid()))).withAlias(new Alias("`" + viewAttrVo.getUuid() + "`")));
                        Function function = new Function();
                        function.setName("md5");
                        ExpressionList expressionList = new ExpressionList();
                        expressionList.addExpressions(new Column("id").withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())));
                        function.setParameters(expressionList);
                        plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewAttrVo.getUuid() + "_hash`")));


                        plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_attrentity").withSchemaName(TenantContext.get().getDbName()).withAlias(new Alias("attr_" + viewAttrVo.getUuid()))).addOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("from_cientity_id"))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("attr_id")).withRightExpression(new LongValue(attrVo.getId())))));
                        if (targetCiVo.getIsVirtual().equals(0)) {
                            plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_cientity").withSchemaName(TenantContext.get().getDbName()).withAlias(new Alias("attr_cientity_" + viewAttrVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("to_cientity_id")).withRightExpression(new Column().withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())).withColumnName("id"))));
                        } else {
                            plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName(targetCiVo.getCiTableName()).withAlias(new Alias("attr_cientity_" + viewAttrVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("to_cientity_id")).withRightExpression(new Column().withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())).withColumnName("id"))));
                        }
                    }
                }
            }
            //生成主SQL，需要join所有父模型数据表
            for (CiVo ci : ciVo.getUpwardCiList()) {
                plainSelect.addJoins(new Join().withRightItem(new Table().withName("cmdb_" + ci.getId()).withSchemaName(TenantContext.get().getDataDbName()).withAlias(new Alias("cmdb_" + ci.getId()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("cmdb_" + ci.getId())).withColumnName(ci.getIsVirtual().equals(0) ? "cientity_id" : "id"))));
            }
            //隐藏超时数据
            plainSelect.withWhere(getExpiredExpression());

            //增加where条件，限制数据在自己模型，不要查出子模型数据
            /*plainSelect.withWhere(new EqualsTo()
                    .withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("ci_id"))
                    .withRightExpression(new LongValue(customViewCiVo.getCiId())));*/
            return select;
        } else {
            Table mainTable = new Table();
            mainTable.setSchemaName(TenantContext.get().getDataDbName());
            mainTable.setName("cmdb_" + ciVo.getId());
            Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
            SelectBody selectBody = select.getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("id")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("name")));
            //plainSelect.addSelectItems(new SelectExpressionItem(new StringValue(ciVo.getName())).withAlias(new Alias("ciName")));
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("label").withTable(new Table("ci_info"))).withAlias(new Alias("ciName")));

            plainSelect.addSelectItems(new SelectExpressionItem(new Column("uuid")));

            for (CustomViewConstAttrVo viewConstAttrVo : customViewCiVo.getConstAttrList()) {
                //由于内部属性来自不同的表，暂时先特殊处理
                if (viewConstAttrVo.getConstName().equalsIgnoreCase("ciName")) {
                    //plainSelect.addSelectItems(new SelectExpressionItem(new StringValue(ciVo.getName())).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "`")));
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("label").withTable(new Table("ci_info"))).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "`")));

                    Function function = new Function();
                    function.setName("md5");
                    ExpressionList expressionList = new ExpressionList();
                    expressionList.addExpressions(new StringValue(ciVo.getName()));
                    function.setParameters(expressionList);
                    plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "_hash`")));
                } else {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + viewConstAttrVo.getConstName() + "`")).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "`")));
                    Function function = new Function();
                    function.setName("md5");
                    ExpressionList expressionList = new ExpressionList();
                    expressionList.addExpressions(new Column("`" + viewConstAttrVo.getConstName() + "`"));
                    function.setParameters(expressionList);
                    plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewConstAttrVo.getUuid() + "_hash`")));
                }
            }

            for (CustomViewAttrVo viewAttrVo : customViewCiVo.getAttrList()) {
                AttrVo attrVo = viewAttrVo.getAttrVo();
                if (attrVo.getTargetCiId() == null) {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "`").withTable(new Table("cmdb_" + attrVo.getCiId()))).withAlias(new Alias("`" + viewAttrVo.getUuid() + "`")));
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "_hash`").withTable(new Table("cmdb_" + attrVo.getCiId()))).withAlias(new Alias("`" + viewAttrVo.getUuid() + "_hash`")));
                } else {
                    CiVo targetCiVo = ciService.getCiById(attrVo.getTargetCiId());
                    if (targetCiVo != null) {
                        plainSelect.addSelectItems(new SelectExpressionItem(new Column("name").withTable(new Table("attr_cientity_" + viewAttrVo.getUuid()))).withAlias(new Alias("`" + viewAttrVo.getUuid() + "`")));
                        Function function = new Function();
                        function.setName("md5");
                        ExpressionList expressionList = new ExpressionList();
                        expressionList.addExpressions(new Column("id").withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())));
                        function.setParameters(expressionList);
                        plainSelect.addSelectItems(new SelectExpressionItem(function).withAlias(new Alias("`" + viewAttrVo.getUuid() + "_hash`")));


                        plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_attrentity").withSchemaName(TenantContext.get().getDbName()).withAlias(new Alias("attr_" + viewAttrVo.getUuid()))).addOnExpression(new AndExpression().withLeftExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("ci_base")).withColumnName("id")).withRightExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("from_cientity_id"))).withRightExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("attr_id")).withRightExpression(new LongValue(attrVo.getId())))));
                        if (targetCiVo.getIsVirtual().equals(0)) {
                            plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName("cmdb_cientity").withSchemaName(TenantContext.get().getDbName()).withAlias(new Alias("attr_cientity_" + viewAttrVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("to_cientity_id")).withRightExpression(new Column().withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())).withColumnName("id"))));
                        } else {
                            plainSelect.addJoins(new Join().withLeft(true).withRightItem(new Table().withName(targetCiVo.getCiTableName()).withAlias(new Alias("attr_cientity_" + viewAttrVo.getUuid()))).addOnExpression(new EqualsTo().withLeftExpression(new Column().withTable(new Table("attr_" + viewAttrVo.getUuid())).withColumnName("to_cientity_id")).withRightExpression(new Column().withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())).withColumnName("id"))));
                        }
                    }
                }
            }
            //隐藏超时数据
            plainSelect.withWhere(getExpiredExpression());
            return select;
        }
    }

    private Expression getExpiredExpression() {
          /*
               (not exists (select 1 from cmdb_cientity_expiredtime xx where xx.cientity_id = `ci_base`.id) or exists
            (select 1 from cmdb_cientity_expiredtime xx where xx.cientity_id = `ci_base`.id
            and xx.expired_time &gt;= NOW()))
             */
        return new OrExpression()
                .withLeftExpression(new ExistsExpression()
                        .withNot(true)
                        .withRightExpression(new SubSelect()
                                .withSelectBody(new PlainSelect()
                                        .withFromItem(new Table("cmdb_cientity_expiredtime").withAlias(new Alias("ex")))
                                        .addSelectItems(new SelectExpressionItem(new Column("1")))
                                        .withWhere(new EqualsTo(new Column("ex.cientity_id"), new Column("ci_base.id"))))))
                .withRightExpression(new ExistsExpression().withRightExpression(new SubSelect()
                        .withSelectBody((new PlainSelect()
                                .withFromItem(new Table("cmdb_cientity_expiredtime").withAlias(new Alias("ex")))
                                .addSelectItems(new SelectExpressionItem(new Column("1")))
                                .withWhere(new AndExpression().withLeftExpression(new EqualsTo(new Column("ex.cientity_id"), new Column("ci_base.id")))
                                        .withRightExpression(new GreaterThanEquals().withLeftExpression(new Column("ex.expired_time")).withRightExpression(new Function().withName("now"))))))));
    }

}
