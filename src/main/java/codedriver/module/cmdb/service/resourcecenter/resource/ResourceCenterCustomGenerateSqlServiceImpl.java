/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.crossover.IResourceCenterCustomGenerateSqlCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
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
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByProtocolIdList(List<Long> protocolIdList, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (CollectionUtils.isNotEmpty(protocolIdList)) {
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
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByTagIdList(List<Long> tagIdList, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (CollectionUtils.isNotEmpty(tagIdList)) {
                    Table mainTable = (Table) plainSelect.getFromItem();
                    Table table = new Table("cmdb_resourcecenter_resource_tag").withAlias(new Alias("d").withUseAs(false));
                    EqualsTo equalsTo = new EqualsTo()
                            .withLeftExpression(new Column(table, "resource_id"))
                            .withRightExpression(new Column(mainTable, "id"));
                    InExpression inExpression = new InExpression();
                    inExpression.setLeftExpression(new Column(table, "tag_id"));
                    ExpressionList expressionList = new ExpressionList();
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
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByKeyword(String keyword, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (StringUtils.isNotBlank(keyword)) {
                    List<ResourceInfo> keywordList = new ArrayList<>();
                    keywordList.add(new ResourceInfo("resource_ipobject", "name"));
                    keywordList.add(new ResourceInfo("resource_ipobject", "ip"));
                    keywordList.add(new ResourceInfo("resource_softwareservice", "port"));
                    StringValue stringValue = new StringValue("%" + keyword + "%");
                    List<Expression> expressionList = new ArrayList<>();
                    for (ResourceInfo resourceInfo : keywordList) {
                        if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                            expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(stringValue));
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

    @Override
    public List<ResourceInfo> getTheadList() {
        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_ipobject", "id"));
        //1.IP地址:端口
        theadList.add(new ResourceInfo("resource_ipobject", "ip"));
        theadList.add(new ResourceInfo("resource_softwareservice", "port"));
        //2.类型
        theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
        theadList.add(new ResourceInfo("resource_ipobject", "type_name"));
        theadList.add(new ResourceInfo("resource_ipobject", "type_label"));
        //3.名称
        theadList.add(new ResourceInfo("resource_ipobject", "name"));
        //4.监控状态
        theadList.add(new ResourceInfo("resource_ipobject", "monitor_status"));
        theadList.add(new ResourceInfo("resource_ipobject", "monitor_time"));
        //5.巡检状态
        theadList.add(new ResourceInfo("resource_ipobject", "inspect_status"));
        theadList.add(new ResourceInfo("resource_ipobject", "inspect_time"));
        //12.网络区域
        theadList.add(new ResourceInfo("resource_ipobject", "network_area"));
        //14.维护窗口
        theadList.add(new ResourceInfo("resource_ipobject", "maintenance_window"));
        //16.描述
        theadList.add(new ResourceInfo("resource_ipobject", "description"));
        //6.模块
        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_id"));
        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_name"));
        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_abbr_name"));
        //7.应用
        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_id"));
        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_name"));
        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_abbr_name"));
        //8.IP列表
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_id"));
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_ip"));
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_label"));
        //9.所属部门
        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_id"));
        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_name"));
        //10.所有者
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_id"));
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_uuid"));
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_name"));
        //11.资产状态
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_id"));
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_name"));
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_label"));
        //环境状态
//        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
//        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
        return theadList;
    }
}
