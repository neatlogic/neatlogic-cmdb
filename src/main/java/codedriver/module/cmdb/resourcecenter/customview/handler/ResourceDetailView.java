/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.resourcecenter.customview.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.dto.resourcecenter.customview.ICustomView;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigNotFoundException;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
import codedriver.module.cmdb.utils.ResourceEntityViewBuilder;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2022/2/8 17:03
 **/
//@Component
public class ResourceDetailView implements ICustomView {
    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;
    @Override
    public String getName() {
        return "resource_ipobject_detail";
    }

    @Override
    public String getComment() {
        return "IP软硬件详情";
    }

    @Override
    public String getSelectBody() {
//        String sql =
//                "SELECT a.`id`, a.`name`, a.`type_id`, a.`type_name`, a.`type_label`, a.`maintenance_window`, a.`description`,\n" +
//                "       a.`network_area`, a.`ip`, a.`inspect_status`, a.`inspect_time`, a.`monitor_status`, a.`monitor_time`,\n" +
//                "       b.`bg_id`, b.`bg_name`,\n" +
//                "       c.`ip_id` AS allip_id, c.`ip` AS allip_ip, c.`ip_label` AS allip_label,\n" +
//                "       e.`user_id`, e.`user_name`, e.`user_uuid`,\n" +
//                "       f.`state_id`, f.`state_name`,\n" +
//                "       g.`datacenter_id`, g.`datacenter_name`,\n" +
//                "       h.`env_id`, h.`env_name`,\n" +
//                "       i.`app_module_id`, i.`app_module_name`,\n" +
//                "       l.`id` AS `app_system_id`, l.`name` AS app_system_name, l.`abbr_name` AS app_system_abbr_name,\n" +
//                "       m.`port`\n" +
//                "FROM `resource_ipobject` a\n" +
//                "LEFT JOIN `resource_ipobject_bg` b ON b.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_ipobject_allip` c ON c.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_ipobject_owner` e ON e.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_ipobject_state` f ON f.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_ipobject_datacenter` g ON g.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_softwareservice_env` h ON h.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_ipobject_appmodule` i ON i.`resource_id` = a.`id`\n" +
//                "LEFT JOIN `resource_appsystem_appmodule` j ON j.`app_module_id` = i.`app_module_id`\n" +
//                "LEFT JOIN `resource_appsystem` l ON l.`id` = j.`resource_id`\n" +
//                "LEFT JOIN `resource_softwareservice` m ON m.`id` = a.`id`";
//        return sql;
//        Table mainTable = new Table(TenantContext.get().getDataDbName(),"resource_ipobject").withAlias(new Alias("a").withUseAs(false));
//        PlainSelect plainSelect = new PlainSelect()
//                .withFromItem(mainTable)
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "id")).withAlias(new Alias("id")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "name")).withAlias(new Alias("name")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "type_id")).withAlias(new Alias("type_id")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "type_name")).withAlias(new Alias("type_name")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "type_label")).withAlias(new Alias("type_label")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "maintenance_window")).withAlias(new Alias("maintenance_window")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "description")).withAlias(new Alias("description")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "network_area")).withAlias(new Alias("network_area")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "ip")).withAlias(new Alias("ip")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "inspect_status")).withAlias(new Alias("inspect_status")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "inspect_time")).withAlias(new Alias("inspect_time")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "monitor_status")).withAlias(new Alias("monitor_status")))
//                .addSelectItems(new SelectExpressionItem(new Column(mainTable, "monitor_time")).withAlias(new Alias("monitor_time")))
//                ;
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_ipobject_bg").withAlias(new Alias("b").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "bg_id")).withAlias(new Alias("bg_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "bg_name")).withAlias(new Alias("bg_name")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_ipobject_allip").withAlias(new Alias("c").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "allip_id")).withAlias(new Alias("allip_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "allip_ip")).withAlias(new Alias("allip_ip")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "allip_label")).withAlias(new Alias("allip_label")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_ipobject_owner").withAlias(new Alias("d").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "user_id")).withAlias(new Alias("user_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "user_name")).withAlias(new Alias("user_name")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "user_uuid")).withAlias(new Alias("user_uuid")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_softwareservice").withAlias(new Alias("e").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "port")).withAlias(new Alias("port")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_ipobject_state").withAlias(new Alias("f").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "state_id")).withAlias(new Alias("state_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "state_name")).withAlias(new Alias("state_name")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "state_label")).withAlias(new Alias("state_label")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_ipobject_datacenter").withAlias(new Alias("g").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "datacenter_id")).withAlias(new Alias("datacenter_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "datacenter_name")).withAlias(new Alias("datacenter_name")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_softwareservice_env").withAlias(new Alias("h").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "env_id")).withAlias(new Alias("env_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "env_name")).withAlias(new Alias("env_name")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_ipobject_appmodule").withAlias(new Alias("i").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column(mainTable, "id"))
//                    .withRightExpression(new Column(table, "resource_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "app_module_id")).withAlias(new Alias("app_module_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "app_module_abbr_name")).withAlias(new Alias("app_module_abbr_name")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "app_module_name")).withAlias(new Alias("app_module_name")));
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_appsystem_appmodule").withAlias(new Alias("j").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column("i.app_module_id"))
//                    .withRightExpression(new Column(table, "app_module_id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join);
//        }
//        {
//            Table table = new Table(TenantContext.get().getDataDbName(),"resource_appsystem").withAlias(new Alias("k").withUseAs(false));
//            EqualsTo equalsTo = new EqualsTo()
//                    .withLeftExpression(new Column("j.resource_id"))
//                    .withRightExpression(new Column(table, "id"));
//            Join join = new Join().withLeft(true).withRightItem(table).addOnExpression(equalsTo);
//            plainSelect.addJoins(join)
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "id")).withAlias(new Alias("app_system_id")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "abbr_name")).withAlias(new Alias("app_system_abbr_name")))
//                    .addSelectItems(new SelectExpressionItem(new Column(table, "name")).withAlias(new Alias("app_system_name")));
//        }
//        return plainSelect.toString();


        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_ipobject", "id"));
        //3.名称
        theadList.add(new ResourceInfo("resource_ipobject", "name"));
        //2.类型
        theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
        theadList.add(new ResourceInfo("resource_ipobject", "type_name"));
        theadList.add(new ResourceInfo("resource_ipobject", "type_label"));
        //14.维护窗口
        theadList.add(new ResourceInfo("resource_ipobject", "maintenance_window"));
        //16.描述
        theadList.add(new ResourceInfo("resource_ipobject", "description"));
        //12.网络区域
        theadList.add(new ResourceInfo("resource_ipobject", "network_area"));
        //1.IP地址:端口
        theadList.add(new ResourceInfo("resource_ipobject", "ip"));
        theadList.add(new ResourceInfo("resource_softwareservice", "port"));
        //4.监控状态
        theadList.add(new ResourceInfo("resource_ipobject", "monitor_status"));
        theadList.add(new ResourceInfo("resource_ipobject", "monitor_time"));
        //5.巡检状态
        theadList.add(new ResourceInfo("resource_ipobject", "inspect_status"));
        theadList.add(new ResourceInfo("resource_ipobject", "inspect_time"));
        //9.所属部门
        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_id"));
        theadList.add(new ResourceInfo("resource_ipobject_bg", "bg_name"));
        //8.IP列表
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_id"));
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_ip"));
        theadList.add(new ResourceInfo("resource_ipobject_allip", "allip_label"));
        //10.所有者
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_id"));
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_uuid"));
        theadList.add(new ResourceInfo("resource_ipobject_owner", "user_name"));
        //11.资产状态
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_id"));
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_name"));
        theadList.add(new ResourceInfo("resource_ipobject_state", "state_label"));
        //环境状态
        theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_id"));
        theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_name"));
        //环境状态
        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
        theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
        //6.模块
        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_id"));
        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_name"));
        theadList.add(new ResourceInfo("resource_ipobject_appmodule", "app_module_abbr_name"));
        //7.应用
        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_id"));
        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_name"));
        theadList.add(new ResourceInfo("resource_appmodule_appsystem", "app_system_abbr_name"));

        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId("resource_ipobject");
        if (plainSelect == null) {
            return null;
        }
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            }
        }
        return plainSelect.toString();
    }

}
