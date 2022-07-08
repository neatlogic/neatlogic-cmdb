/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.crossover.IResourceCenterCustomGenerateSqlCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Service
public class ResourceCenterCustomGenerateSqlServiceImpl implements ResourceCenterCustomGenerateSqlService, IResourceCenterCustomGenerateSqlCrossoverService {
    @Override
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByProtocolIdList(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                JSONArray protocolIdArray = paramObj.getJSONArray("protocolIdList");
                if (CollectionUtils.isNotEmpty(protocolIdArray)) {
                    Table mainTable = (Table) plainSelect.getFromItem();
                    Table table = new Table("cmdb_resourcecenter_resource_account").withAlias(new Alias("b").withUseAs(false));
                    EqualsTo equalsTo = new EqualsTo()
                            .withLeftExpression(new Column(table, "resource_id"))
                            .withRightExpression(new Column(mainTable, "id"));
                    Join join1 = new Join().withRightItem(table).addOnExpression(equalsTo);
                    plainSelect.addJoins(join1);

                    Table table2 = new Table("cmdb_resourcecenter_account").withAlias(new Alias("c").withUseAs(false));
                    EqualsTo equalsTo1 = new EqualsTo()
                            .withLeftExpression(new Column(table2, "id"))
                            .withRightExpression(new Column(table, "account_id"));
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(new Column(table2, "protocol_id"));
                    ExpressionList expressionList = new ExpressionList();
                    List<Long> protocolIdList = protocolIdArray.toJavaList(Long.class);
                    for (Long protocolId : protocolIdList) {
                        expressionList.addExpressions(new LongValue(protocolId));
                    }
                    inExpression.setRightItemsList(expressionList);
                    Join join2 = new Join().withRightItem(table2).addOnExpression(new AndExpression(equalsTo1, inExpression));
                    plainSelect.addJoins(join2);
                }
            }
        };
        return biConsumer;
    }

    @Override
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByTagIdList(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                JSONArray tagIdArray = paramObj.getJSONArray("tagIdList");
                if (CollectionUtils.isNotEmpty(tagIdArray)) {
                    Table mainTable = (Table) plainSelect.getFromItem();
                    Table table = new Table("cmdb_resourcecenter_resource_tag").withAlias(new Alias("d").withUseAs(false));
                    EqualsTo equalsTo = new EqualsTo()
                            .withLeftExpression(new Column(table, "resource_id"))
                            .withRightExpression(new Column(mainTable, "id"));
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(new Column(table, "tag_id"));
                    ExpressionList expressionList = new ExpressionList();
                    List<Long> tagIdList = tagIdArray.toJavaList(Long.class);
                    for (Long tagId : tagIdList) {
                        expressionList.addExpressions(new LongValue(tagId));
                    }
                    inExpression.setRightItemsList(expressionList);
                    Join join1 = new Join().withRightItem(table).addOnExpression(new AndExpression(equalsTo, inExpression));
                    plainSelect.addJoins(join1);
                }
            }
        };
        return biConsumer;
    }

    @Override
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByKeyword(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                String keyword = paramObj.getString("keyword");
                if (StringUtils.isNotBlank(keyword)) {
                    List<ResourceInfo> keywordList = new ArrayList<>();
                    keywordList.add(new ResourceInfo("resource_ipobject", "name"));
                    keywordList.add(new ResourceInfo("resource_ipobject", "ip"));
                    keywordList.add(new ResourceInfo("resource_softwareservice", "port"));
                    keyword = "%" + keyword + "%";
                    List<Expression> expressionList = new ArrayList<>();
                    for (ResourceInfo resourceInfo : keywordList) {
                        if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                            expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(new StringValue(keyword)));
                        } else {
                            unavailableResourceInfoList.add(resourceInfo);
                        }
                    }
                    MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList);
                    resourceSearchGenerateSqlUtil.addWhere(plainSelect, multiOrExpression);
                }
            }
        };
        return biConsumer;
    }
}
