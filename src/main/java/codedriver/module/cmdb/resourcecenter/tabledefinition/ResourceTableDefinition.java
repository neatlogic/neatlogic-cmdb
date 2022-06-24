/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.resourcecenter.tabledefinition;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityAttrVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityJoinVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.resourcecenter.JoinType;
import codedriver.framework.jsqlparser.OrExpressionList;
import com.alibaba.fastjson.JSONArray;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ResourceTableDefinition {

    private static String fromViewName = "resource_ipobject";
    private static List<SearchConditionMapping> theadList = new ArrayList<>();
    private static Map<String, SearchConditionMapping> searchConditionMappingMap = new HashMap<>();
    private static List<SearchConditionMapping> keywordList = new ArrayList<>();
    static {
        theadList.add(new SearchConditionMapping("resource_ipobject", "id"));
        //1.IP地址:端口
        theadList.add(new SearchConditionMapping("resource_ipobject", "ip"));
        theadList.add(new SearchConditionMapping("resource_softwareservice", "port"));
        //2.类型
        theadList.add(new SearchConditionMapping("resource_ipobject", "type_id"));
        theadList.add(new SearchConditionMapping("resource_ipobject", "type_name"));
        theadList.add(new SearchConditionMapping("resource_ipobject", "type_label"));
        //3.名称
        theadList.add(new SearchConditionMapping("resource_ipobject", "name"));
        //4.监控状态
        theadList.add(new SearchConditionMapping("resource_ipobject", "monitor_status"));
        theadList.add(new SearchConditionMapping("resource_ipobject", "monitor_time"));
        //5.巡检状态
        theadList.add(new SearchConditionMapping("resource_ipobject", "inspect_status"));
        theadList.add(new SearchConditionMapping("resource_ipobject", "inspect_time"));
        //12.网络区域
        theadList.add(new SearchConditionMapping("resource_ipobject", "network_area"));
        //14.维护窗口
        theadList.add(new SearchConditionMapping("resource_ipobject", "maintenance_window"));
        //16.描述
        theadList.add(new SearchConditionMapping("resource_ipobject", "description"));
        //6.模块
        theadList.add(new SearchConditionMapping("resource_ipobject_appmodule", "app_module_id"));
        theadList.add(new SearchConditionMapping("resource_ipobject_appmodule", "app_module_name"));
        theadList.add(new SearchConditionMapping("resource_ipobject_appmodule", "app_module_abbr_name"));
        //7.应用
        theadList.add(new SearchConditionMapping("resource_appmodule_appsystem", "app_system_id"));
        theadList.add(new SearchConditionMapping("resource_appmodule_appsystem", "app_system_name"));
        theadList.add(new SearchConditionMapping("resource_appmodule_appsystem", "app_system_abbr_name"));
        //8.IP列表
        theadList.add(new SearchConditionMapping("resource_ipobject_allip", "allip_id"));
        theadList.add(new SearchConditionMapping("resource_ipobject_allip", "allip_ip"));
        theadList.add(new SearchConditionMapping("resource_ipobject_allip", "allip_label"));
        //9.所属部门
        theadList.add(new SearchConditionMapping("resource_ipobject_bg", "bg_id"));
        theadList.add(new SearchConditionMapping("resource_ipobject_bg", "bg_name"));
        //10.所有者
        theadList.add(new SearchConditionMapping("resource_ipobject_owner", "user_id"));
        theadList.add(new SearchConditionMapping("resource_ipobject_owner", "user_uuid"));
        theadList.add(new SearchConditionMapping("resource_ipobject_owner", "user_name"));
        //11.资产状态
        theadList.add(new SearchConditionMapping("resource_ipobject_state", "state_id"));
        theadList.add(new SearchConditionMapping("resource_ipobject_state", "state_name"));
        theadList.add(new SearchConditionMapping("resource_ipobject_state", "state_label"));

        searchConditionMappingMap.put("typeIdList", new SearchConditionMapping("resource_ipobject","type_id", false));
        searchConditionMappingMap.put("stateIdList", new SearchConditionMapping("resource_ipobject_state","state_id", false));
        searchConditionMappingMap.put("envIdList", new SearchConditionMapping("resource_softwareservice_env","env_id", false));
        searchConditionMappingMap.put("appSystemIdList", new SearchConditionMapping("resource_appmodule_appsystem","app_system_id", false));
        searchConditionMappingMap.put("appModuleIdList", new SearchConditionMapping("resource_ipobject_appmodule","app_module_id", false));
        searchConditionMappingMap.put("defaultValue", new SearchConditionMapping("resource_ipobject","id", false));
        searchConditionMappingMap.put("inspectStatusList", new SearchConditionMapping("resource_ipobject","inspect_status", false));

        keywordList.add(new SearchConditionMapping("resource_ipobject", "name"));
        keywordList.add(new SearchConditionMapping("resource_ipobject", "ip"));
        keywordList.add(new SearchConditionMapping("resource_softwareservice", "port"));
    }

    private final List<ResourceEntityVo> resourceEntityList;

    public ResourceTableDefinition(List<ResourceEntityVo> resourceEntityList) {
        this.resourceEntityList = resourceEntityList;
    }
    private PlainSelect filterPlainSelect;

    public String getResourceCountSql(ResourceSearchVo searchVo) {
        if (filterPlainSelect == null) {
            filterPlainSelect = getPlainSelectBySearchCondition(searchVo);
        }
        Table fromTable = (Table)filterPlainSelect.getFromItem();
        filterPlainSelect.setSelectItems(Arrays.asList(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(new Column(fromTable, "id")))))));
        return filterPlainSelect.toString();
    }
    public String getResourceIdListSql(ResourceSearchVo searchVo) {
        if (filterPlainSelect == null) {
            filterPlainSelect = getPlainSelectBySearchCondition(searchVo);
        }
        Table fromTable = (Table)filterPlainSelect.getFromItem();
        List<OrderByElement> orderByElements = new ArrayList<>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.withExpression(new Column(fromTable, "id")).withAsc(true);
        orderByElements.add(orderByElement);
        filterPlainSelect.withOrderByElements(orderByElements);
        filterPlainSelect.withDistinct(new Distinct()).setSelectItems(Arrays.asList((new SelectExpressionItem(new Column(fromTable, "id")))));
        filterPlainSelect.withLimit(new Limit().withOffset(new LongValue(searchVo.getStartNum())).withRowCount(new LongValue(searchVo.getPageSize())));
        return filterPlainSelect.toString();
    }

    public String getResourceListByIdListSql(List<Long> idList) {
        PlainSelect plainSelect = getPlainSelect();
        if (CollectionUtils.isNotEmpty(idList)) {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column((Table) plainSelect.getFromItem(), "id"));
            ExpressionList expressionList = new ExpressionList();
            for (Object id : idList) {
                if (id instanceof Long) {
                    expressionList.addExpressions(new LongValue((Long)id));
                } else if (id instanceof String) {
                    expressionList.addExpressions(new StringValue((String)id));
                }
            }
            inExpression.setRightItemsList(expressionList);
            plainSelect.setWhere(inExpression);
        }
        return plainSelect.toString();
    }
    public PlainSelect getPlainSelect() {
        CiVo fromTableCiVo = getFromTableCiByFromViewName(fromViewName);
//        String fromTableName = fromTableCiVo.getCiTableName();
        String fromTableAlias = fromTableCiVo.getName();
        Table fromTable = new Table("cmdb_cientity").withAlias(new Alias(fromTableAlias).withUseAs(false));
        PlainSelect plainSelect = new PlainSelect()
                .withFromItem(fromTable);
        Table cmdbCi = new Table("cmdb_ci").withAlias(new Alias("ci_" + fromTableAlias).withUseAs(false));
        Join joinCmdbCi = new Join().withRightItem(cmdbCi).addOnExpression(new EqualsTo(new Column(cmdbCi, "id"), new Column(fromTable, "ci_id")));
        plainSelect.addJoins(joinCmdbCi);
        for (SearchConditionMapping searchConditionMapping : theadList) {
            parse(searchConditionMapping);
            parse2(searchConditionMapping, plainSelect);
//            System.out.println(plainSelect.toString()+ ";");
        }
        return plainSelect;
    }

    private PlainSelect getPlainSelectBySearchCondition(ResourceSearchVo searchVo) {
//        JSONArray defaultValueArray = new JSONArray();
//        defaultValueArray.add(99L);
//        defaultValueArray.add(100L);
//        defaultValueArray.add(101L);
//        searchVo.setDefaultValue(defaultValueArray);
        CiVo fromTableCiVo = getFromTableCiByFromViewName(fromViewName);
//        String fromTableName = fromTableCiVo.getCiTableName();
        String fromTableAlias = fromTableCiVo.getName();
        Table fromTable = new Table("cmdb_cientity").withAlias(new Alias(fromTableAlias).withUseAs(false));
        PlainSelect plainSelect = new PlainSelect()
                .withFromItem(fromTable);
        Table cmdbCi = new Table("cmdb_ci").withAlias(new Alias("ci_" + fromTableAlias).withUseAs(false));
        Join joinCmdbCi = new Join().withRightItem(cmdbCi).addOnExpression(new EqualsTo(new Column(cmdbCi, "id"), new Column(fromTable, "ci_id")));
        plainSelect.addJoins(joinCmdbCi);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("defaultValue");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : idList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        List<Long> typeIdList = searchVo.getTypeIdList();
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("typeIdList");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : typeIdList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        List<String> inspectStatusList = searchVo.getInspectStatusList();
        if (CollectionUtils.isNotEmpty(inspectStatusList)) {
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("inspectStatusList");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (String inspectStatus : inspectStatusList) {
                expressionList.addExpressions(new StringValue(inspectStatus));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        List<Long> stateIdList = searchVo.getStateIdList();
        if (CollectionUtils.isNotEmpty(stateIdList)) {
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("stateIdList");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : stateIdList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        List<Long> envIdList = searchVo.getEnvIdList();
        if (CollectionUtils.isNotEmpty(envIdList)) {
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("envIdList");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : envIdList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        List<Long> appModuleIdList = searchVo.getAppModuleIdList();
        List<Long> appSystemIdList = searchVo.getAppSystemIdList();
        if (CollectionUtils.isNotEmpty(appModuleIdList) || CollectionUtils.isNotEmpty(appSystemIdList)) {
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("appModuleIdList");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : appModuleIdList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            SearchConditionMapping searchConditionMapping = searchConditionMappingMap.get("appSystemIdList");
            parse(searchConditionMapping);
            Column column = parse2(searchConditionMapping, plainSelect);
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(column);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : appSystemIdList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        }
        List<Long> protocolIdList = searchVo.getProtocolIdList();
        if (CollectionUtils.isNotEmpty(protocolIdList)) {
            Table table = new Table("cmdb_resourcecenter_resource_account").withAlias(new Alias("b").withUseAs(false));
            EqualsTo equalsTo = new EqualsTo()
                    .withLeftExpression(new Column(table, "resource_id"))
                    .withRightExpression(new Column(fromTable, "id"));
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
        List<Long> tagIdList = searchVo.getTagIdList();
        if (CollectionUtils.isNotEmpty(tagIdList)) {
            Table table = new Table("cmdb_resourcecenter_resource_tag").withAlias(new Alias("d").withUseAs(false));
            EqualsTo equalsTo = new EqualsTo()
                    .withLeftExpression(new Column(table, "resource_id"))
                    .withRightExpression(new Column(fromTable, "id"));
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
//        System.out.println(plainSelect.toString()+ ";");
        String keyword = searchVo.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            keyword = "%" + keyword + "%";
            OrExpressionList orExpressionList = new OrExpressionList();
            for (SearchConditionMapping searchConditionMapping : keywordList) {
                parse(searchConditionMapping);
                Column column = parse2(searchConditionMapping, plainSelect);
                orExpressionList.addExpressions(new LikeExpression().withLeftExpression(column).withRightExpression(new StringValue(keyword)));
            }
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(orExpressionList);
            } else {
                plainSelect.setWhere(new AndExpression(where, orExpressionList));
            }
        }
        return plainSelect;
    }


    private CiVo getFromTableCiByFromViewName(String fromViewName) {
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            if (Objects.equals(resourceEntityVo.getName(), fromViewName)) {
                return resourceEntityVo.getCi();
            }
        }
        return null;
    }

    private void parse(SearchConditionMapping searchConditionMapping) {
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            if (Objects.equals(resourceEntityVo.getName(), searchConditionMapping.getTableName())) {
                CiVo ciVo = resourceEntityVo.getCi();
                searchConditionMapping.setFromTableAlias(ciVo.getName());
                searchConditionMapping.setFromTableCiId(ciVo.getId());
                Set<ResourceEntityJoinVo> joinList = resourceEntityVo.getJoinList();
                if (CollectionUtils.isNotEmpty(joinList)) {
                    for (ResourceEntityJoinVo joinVo : joinList) {
                        if (Objects.equals(joinVo.getField(), searchConditionMapping.getColumnName())) {
                            CiVo joinCiVo = joinVo.getCi();
                            String joinAttrName = joinVo.getJoinAttrName();
                            if (joinVo.getJoinType() == JoinType.ATTR) {
                                searchConditionMapping.setJoinType(JoinType.ATTR);
                                List<AttrVo> attrList = ciVo.getAttrList();
                                for (AttrVo attrVo : attrList) {
                                    if (Objects.equals(attrVo.getName(), joinAttrName) && Objects.equals(attrVo.getTargetCiId(), joinCiVo.getId())) {
                                        searchConditionMapping.setFromTableAttrId(attrVo.getId());
                                        searchConditionMapping.setFromTableAttrName(joinAttrName);
                                        searchConditionMapping.setJoinTableCiIsVirtual(joinCiVo.getIsVirtual());
                                        searchConditionMapping.setJoinTableAlias(joinCiVo.getName());
                                        searchConditionMapping.setJoinTableId(joinCiVo.getId());
                                    }
                                }
                            } else {
                                searchConditionMapping.setJoinType(JoinType.REL);
                                searchConditionMapping.setDirection(joinVo.getDirection());
                                searchConditionMapping.setJoinTableAlias(joinCiVo.getName());
                                searchConditionMapping.setJoinTableId(joinCiVo.getId());
                            }
//                            System.out.println(JSONObject.toJSONString(searchConditionMapping));
                            return;
                        }
                    }
                }
                Set<ResourceEntityAttrVo> attrList = resourceEntityVo.getAttrList();
                for (ResourceEntityAttrVo resourceEntityAttrVo : attrList) {
                    if (Objects.equals(resourceEntityAttrVo.getField(), searchConditionMapping.getColumnName())) {
                        CiVo attrCiVo = resourceEntityAttrVo.getCi();
                        if (attrCiVo != null) {
                            searchConditionMapping.setJoinTableAlias(attrCiVo.getName());
                            searchConditionMapping.setJoinTableId(attrCiVo.getId());
                            searchConditionMapping.setJoinTableCiIsVirtual(attrCiVo.getIsVirtual());
                        } else {
                            searchConditionMapping.setJoinTableAlias(ciVo.getName());
                            searchConditionMapping.setJoinTableId(ciVo.getId());
                            searchConditionMapping.setJoinTableCiIsVirtual(ciVo.getIsVirtual());
                        }
                        String attr = resourceEntityAttrVo.getAttr();
                        if ("_id".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("id");
                        } else if ("_uuid".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("uuid");
                        } else if ("_name".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("name");
                        } else if ("_fcu".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("fcu");
                        } else if ("_fcd".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("fcd");
                        } else if ("_lcu".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("lcu");
                        } else if ("_lcd".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("lcd");
                        } else if ("_inspectStatus".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("inspect_status");
                        } else if ("_inspectTime".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("inspect_time");
                        } else if ("_monitorStatus".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("monitor_status");
                        } else if ("_monitorTime".equals(attr)) {
                            searchConditionMapping.setFromTableAttrName("monitor_time");
                        } else if ("_typeId".equals(attr)) {
                            searchConditionMapping.setFromTableAlias("ci_" + resourceEntityAttrVo.getCiName());
                            searchConditionMapping.setFromTableAttrName("id");
                        } else if ("_typeName".equals(attr)) {
                            searchConditionMapping.setFromTableAlias("ci_" + resourceEntityAttrVo.getCiName());
                            searchConditionMapping.setFromTableAttrName("name");
                        } else if ("_typeLabel".equals(attr)) {
                            searchConditionMapping.setFromTableAlias("ci_" + resourceEntityAttrVo.getCiName());
                            searchConditionMapping.setFromTableAttrName("label");
                        } else {
                            searchConditionMapping.setFromTableAttrName(attr);
                            if (resourceEntityAttrVo.getAttrId() == null) {
//                                searchConditionMapping.setFromTableAlias(resourceEntityAttrVo.getCiName());
//                                searchConditionMapping.setFromTableCiId(resourceEntityAttrVo.getCiId());
                            } else {
                                searchConditionMapping.setFromTableAttrId(resourceEntityAttrVo.getAttrId());
                                if (attrCiVo != null) {
                                    List<AttrVo> attrVoList = attrCiVo.getAttrList();
                                    for (AttrVo attrVo : attrVoList) {
                                        if (Objects.equals(attrVo.getId(), resourceEntityAttrVo.getAttrId())) {
                                            searchConditionMapping.setFromTableAttrCiId(attrVo.getCiId());
                                            searchConditionMapping.setFromTableAttrCiName(attrVo.getCiName());
                                        }
                                    }
                                } else {
                                    List<AttrVo> attrVoList = ciVo.getAttrList();
                                    for (AttrVo attrVo : attrVoList) {
                                        if (Objects.equals(attrVo.getId(), resourceEntityAttrVo.getAttrId())) {
                                            searchConditionMapping.setFromTableAttrCiId(attrVo.getCiId());
                                            searchConditionMapping.setFromTableAttrCiName(attrVo.getCiName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.println(JSONObject.toJSONString(searchConditionMapping));
    }

    private Column parse2(SearchConditionMapping searchConditionMapping, PlainSelect plainSelect) {
        Table fromTable = (Table) plainSelect.getFromItem();
        String fromTableAlias = fromTable.getAlias().getName();
        if (searchConditionMapping.getJoinType() == JoinType.ATTR) {
            // 下拉框类型属性
            if (!Objects.equals(searchConditionMapping.getFromTableAlias(), fromTableAlias)) {
                //属性所在的模型如果不是fromTable模型不同
                Table table = new Table("cmdb_cientity").withAlias(new Alias(searchConditionMapping.getFromTableAlias()).withUseAs(false));
                EqualsTo equalsTo = new EqualsTo()
                        .withLeftExpression(new Column(table, "id"))
                        .withRightExpression(new Column(fromTable, "id"));
                Join join = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Table table = new Table("cmdb_attrentity").withAlias(new Alias("cmdb_attrentity_" + searchConditionMapping.getFromTableAttrName()).withUseAs(false));
            EqualsTo equalsTo1 = new EqualsTo()
                    .withLeftExpression(new Column(table, "from_cientity_id"))
                    .withRightExpression(new Column(new Table(searchConditionMapping.getFromTableAlias()), "id"));
            EqualsTo equalsTo2 = new EqualsTo()
                    .withLeftExpression(new Column(table, "attr_id"))
                    .withRightExpression(new LongValue(searchConditionMapping.getFromTableAttrId()));
            Expression onExpression = new AndExpression(equalsTo1, equalsTo2);
            Join join = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table).addOnExpression(onExpression);
            plainSelect.addJoins(join);

            if (Objects.equals(searchConditionMapping.getJoinTableCiIsVirtual(), 1)) {
                Table table3 = new Table(TenantContext.get().getDataDbName(),"cmdb_" + searchConditionMapping.getJoinTableId()).withAlias(new Alias("cmdb_" + searchConditionMapping.getJoinTableId() + "_" + searchConditionMapping.getJoinTableAlias()).withUseAs(false));
                EqualsTo equalsTo3 = new EqualsTo()
                        .withLeftExpression(new Column(table3, "id"))
                        .withRightExpression(new Column(table, "to_cientity_id"));
                Join join3 = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table3).addOnExpression(equalsTo3);
                plainSelect.addJoins(join3);
                plainSelect.addSelectItems(new SelectExpressionItem(new Column(table3, "id")).withAlias(new Alias(searchConditionMapping.getColumnName())));
            } else {
                Table table3 = new Table("cmdb_cientity").withAlias(new Alias(searchConditionMapping.getJoinTableAlias()).withUseAs(false));
                EqualsTo equalsTo3 = new EqualsTo()
                        .withLeftExpression(new Column(table3, "id"))
                        .withRightExpression(new Column(table, "to_cientity_id"));
                Join join3 = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table3).addOnExpression(equalsTo3);
                plainSelect.addJoins(join3);
                plainSelect.addSelectItems(new SelectExpressionItem(new Column(table3, "id")).withAlias(new Alias(searchConditionMapping.getColumnName())));
            }
            return new Column(table, "to_cientity_id");
        } else if (searchConditionMapping.getJoinType() == JoinType.REL) {
            //关系
            if (Objects.equals(searchConditionMapping.getDirection(), RelDirectionType.FROM.getValue())) {
                return null;
            } else {
                Table table = new Table("cmdb_relentity").withAlias(new Alias("cmdb_relentity_" + searchConditionMapping.getJoinTableAlias()).withUseAs(false));
                EqualsTo equalsTo = new EqualsTo()
                        .withLeftExpression(new Column(table, "to_cientity_id"))
                        .withRightExpression(new Column(new Table(searchConditionMapping.getFromTableAlias()), "id"));
                Expression onExpression = equalsTo;
                Join join1 = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table).addOnExpression(onExpression);
                plainSelect.addJoins(join1);
                Table table2 = new Table("cmdb_rel").withAlias(new Alias("cmdb_rel_" + searchConditionMapping.getJoinTableAlias()).withUseAs(false));
                EqualsTo equalsTo1 = new EqualsTo()
                        .withLeftExpression(new Column(table2, "id"))
                        .withRightExpression(new Column(table, "rel_id"));
                EqualsTo equalsTo2 = new EqualsTo()
                        .withLeftExpression(new Column(table2, "from_ci_id"))
                        .withRightExpression(new LongValue(searchConditionMapping.getJoinTableId()));
                Join join2 = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table2).addOnExpression(new AndExpression(equalsTo1, equalsTo2));
                plainSelect.addJoins(join2);
                Table table3 = new Table("cmdb_cientity").withAlias(new Alias(searchConditionMapping.getJoinTableAlias()).withUseAs(false));
                EqualsTo equalsTo3 = new EqualsTo()
                        .withLeftExpression(new Column(table3, "id"))
                        .withRightExpression(new Column(table, "from_cientity_id"));
                Join join3 = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table3).addOnExpression(equalsTo3);
                plainSelect.addJoins(join3);
                plainSelect.addSelectItems(new SelectExpressionItem(new Column(table3, "id")).withAlias(new Alias(searchConditionMapping.getColumnName())));
                return new Column(table, "from_cientity_id");
            }
        } else {
            //非下拉框属性
            if (searchConditionMapping.getFromTableAttrId() == null) {
                if (Objects.equals(searchConditionMapping.getJoinTableCiIsVirtual(), 1)) {
                    Column column = new Column(new Table("cmdb_" + searchConditionMapping.getJoinTableId() + "_" + searchConditionMapping.getJoinTableAlias()), "`" + searchConditionMapping.getFromTableAttrName() + "`");
                    plainSelect.addSelectItems(new SelectExpressionItem(column).withAlias(new Alias(searchConditionMapping.getColumnName())));
                    return column;
                } else {
                    Column column = new Column(new Table(searchConditionMapping.getFromTableAlias()), searchConditionMapping.getFromTableAttrName());
                    plainSelect.addSelectItems(new SelectExpressionItem(column).withAlias(new Alias(searchConditionMapping.getColumnName())));
                    return column;
                }
            } else {
                if (Objects.equals(searchConditionMapping.getJoinTableCiIsVirtual(), 1)) {
                    Column column = new Column(new Table("cmdb_" + searchConditionMapping.getJoinTableId() + "_" + searchConditionMapping.getJoinTableAlias()), "`" + searchConditionMapping.getFromTableAttrId() + "`");
                    plainSelect.addSelectItems(new SelectExpressionItem(column).withAlias(new Alias(searchConditionMapping.getColumnName())));
                    return column;
                } else {
                    Table table2 = null;
                    String tableAlias2 = searchConditionMapping.getJoinTableAlias();
                    if (Objects.equals(fromTableAlias, tableAlias2)) {
                        table2 = fromTable;
                    }

                    Table table = null;
                    String tableName = "cmdb_" + searchConditionMapping.getFromTableAttrCiId();
                    String tableAlias = tableName + "_" + searchConditionMapping.getJoinTableAlias();
                    List<Join> joinList = plainSelect.getJoins();
                    for (Join Join : joinList) {
                        FromItem fromItem = Join.getRightItem();
                        if (fromItem instanceof Table) {
                            Table rightItem = (Table) fromItem;
                            if (table == null) {
                                if (Objects.equals(rightItem.getAlias().getName(), tableAlias)) {
                                    table = rightItem;
                                    continue;
                                }
                            }
                            if (table2 == null) {
                                if (Objects.equals(rightItem.getAlias().getName(), tableAlias2)) {
                                    table2 = rightItem;
                                    continue;
                                }
                            }
                            if (table != null && table2 != null) {
                                break;
                            }
                        }
                    }
                    if (table2 == null) {
                        table2 = new Table("cmdb_cientity").withAlias(new Alias(tableAlias2).withUseAs(false));
                        EqualsTo equalsTo = new EqualsTo()
                                .withLeftExpression(new Column(table2, "id"))
                                .withRightExpression(new Column(fromTable, "id"));
                        Join join = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table2).addOnExpression(equalsTo);
                        plainSelect.addJoins(join);
                    }
                    if (table == null) {
                        table = new Table(TenantContext.get().getDataDbName(), tableName).withAlias(new Alias(tableAlias).withUseAs(false));
                        EqualsTo equalsTo = new EqualsTo()
                                .withLeftExpression(new Column(table, "cientity_id"))
                                .withRightExpression(new Column(table2, "id"));
                        Join join = new Join().withLeft(searchConditionMapping.getLeft()).withRightItem(table).addOnExpression(equalsTo);
                        plainSelect.addJoins(join);
                    }
                    Column column = new Column(table, "`" + searchConditionMapping.getFromTableAttrId() + "`");
                    plainSelect.addSelectItems(new SelectExpressionItem(column).withAlias(new Alias(searchConditionMapping.getColumnName())));
                    return column;
                }
            }
        }
    }

}
