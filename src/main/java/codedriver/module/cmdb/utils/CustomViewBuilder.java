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
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CustomViewBuilder {

    private final String dataSchema = TenantContext.get().getDataDbName();
    private final String schema = TenantContext.get().getDbName();
    private static CiService ciService;
    private static Map<String, CustomViewLinkVo> relMap = new HashMap<>();
    private CustomViewVo customViewVo;

    @Autowired
    public CustomViewBuilder(CiService _ciService) {
        ciService = _ciService;
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
        if (CollectionUtils.isNotEmpty(customViewVo.getAttrList())) {
            Table mainTable = new Table();
            mainTable.setSchemaName(TenantContext.get().getDbName());
            mainTable.setName("cmdb_cientity");
            mainTable.setAlias(new Alias("ci_base"));
            Select select = SelectUtils.buildSelectFromTableAndSelectItems(mainTable);
            SelectBody selectBody = select.getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.addSelectItems(new SelectExpressionItem(new Column("id").withTable(new Table("ci_base"))));
            for (CustomViewAttrVo attrVo : customViewVo.getAttrList()) {
                plainSelect.addSelectItems(new SelectExpressionItem(new Column(attrVo.getUuid())));
            }

            for (int i = 0; i < customViewVo.getCiList().size(); i++) {
                CustomViewCiVo customViewCiVo = customViewVo.getCiList().get(i);
                if (i == 0) {
                    //第一个模型是驱动模型
                    plainSelect.addJoins(new Join()
                            .withLeft(true)
                            .withRightItem(new SubSelect()
                                    .withSelectBody(buildSubSelectForCi(customViewCiVo).getSelectBody())
                                    .withAlias(new Alias("ci_" + customViewCiVo.getUuid())))
                            .withOnExpression(new EqualsTo()
                                    .withLeftExpression(new Column()
                                            .withTable(new Table("ci_base"))
                                            .withColumnName("id"))
                                    .withRightExpression(new Column()
                                            .withTable(new Table("ci_" + customViewCiVo.getUuid()))
                                            .withColumnName("id"))));
                } else {
                    CustomViewCiVo prevCustomViewCiVo = customViewVo.getCiList().get(i - 1);
                    List<CustomViewLinkVo> linkList = customViewVo.getLinkListByFromToCustomCiUuid(prevCustomViewCiVo.getUuid(), customViewCiVo.getUuid());
                    //没有关系代表存在孤立节点，这个是不允许的
                    if (CollectionUtils.isNotEmpty(linkList)) {
                        for (CustomViewLinkVo linkVo : linkList) {
                            if (linkVo.getFromType().equals(RelType.ATTR.getValue()) && linkVo.getToType().equals(RelType.ATTR.getValue())) {
                                //属性之间关联
                                Join join = new Join();
                                if (linkVo.getJoinType().equals(JoinType.LEFTJOIN.getValue())) {
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
                            } else if (linkVo.getFromType().equals(RelType.REL.getValue()) && linkVo.getToType().equals(RelType.CI.getValue())) {
                                CustomViewRelVo customViewRelVo = prevCustomViewCiVo.getRelByUuid(linkVo.getFromUuid());
                                if (customViewRelVo != null) {
                                    //关系关联
                                    Join join = new Join();
                                    if (linkVo.getJoinType().equals(JoinType.LEFTJOIN.getValue())) {
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
                                    //关联目标模型
                                    Join join2 = new Join();
                                    if (linkVo.getJoinType().equals(JoinType.LEFTJOIN.getValue())) {
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
                                }
                            }
                        }
                    }
                }
            }
            System.out.println(select);
        }
    }

    private Select buildSubSelectForCi(CustomViewCiVo customViewCiVo) {
        CiVo ciVo = customViewCiVo.getCiVo();
        if (ciVo != null && CollectionUtils.isNotEmpty(ciVo.getUpwardCiList())) {
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
                            .withAlias(new Alias(viewAttrVo.getUuid())));
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("`" + attrVo.getId() + "_hash`")
                            .withTable(new Table("cmdb_" + attrVo.getCiId())))
                            .withAlias(new Alias(viewAttrVo.getUuid() + "_hash")));
                } else {
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("name")
                            .withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())))
                            .withAlias(new Alias(viewAttrVo.getUuid())));
                    plainSelect.addSelectItems(new SelectExpressionItem(new Column("id")
                            .withTable(new Table("attr_cientity_" + viewAttrVo.getUuid())))
                            .withAlias(new Alias(viewAttrVo.getUuid() + "_hash")));

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
