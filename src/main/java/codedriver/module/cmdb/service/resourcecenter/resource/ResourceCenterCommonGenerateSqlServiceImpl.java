/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.crossover.IResourceCenterCommonGenerateSqlCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigNotFoundException;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import codedriver.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;

@Service
public class ResourceCenterCommonGenerateSqlServiceImpl implements ResourceCenterCommonGenerateSqlService, IResourceCenterCommonGenerateSqlCrossoverService {

    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public PlainSelect getResourceCountPlainSelect(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList) {
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
        if (CollectionUtils.isNotEmpty(biConsumerList)) {
            for (BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer : biConsumerList) {
                biConsumer.accept(resourceSearchGenerateSqlUtil, plainSelect);
            }
        }
        Table fromTable = (Table)plainSelect.getFromItem();
        plainSelect.setSelectItems(Arrays.asList(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(new Column(fromTable, "id")))))));
        return plainSelect;
    }

    @Override
    public String getResourceCountSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList) {
        PlainSelect filterPlainSelect = getResourceCountPlainSelect(mainResourceId, biConsumerList);
        if (filterPlainSelect == null) {
            return null;
        }
        return filterPlainSelect.toString();
    }

    @Override
    public String getResourceIdListSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList, int startNum, int pageSize) {
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
        if (CollectionUtils.isNotEmpty(biConsumerList)) {
            for (BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer : biConsumerList) {
                biConsumer.accept(resourceSearchGenerateSqlUtil, plainSelect);
            }
        }
        Table mainTable = (Table)plainSelect.getFromItem();
        List<OrderByElement> orderByElements = new ArrayList<>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.withExpression(new Column(mainTable, "id")).withAsc(true);
        orderByElements.add(orderByElement);
        plainSelect.withOrderByElements(orderByElements);
        plainSelect.withDistinct(new Distinct()).setSelectItems(Arrays.asList((new SelectExpressionItem(new Column(mainTable, "id")))));
        plainSelect.withLimit(new Limit().withOffset(new LongValue(startNum)).withRowCount(new LongValue(pageSize)));
        return plainSelect.toString();
    }

    @Override
    public String getResourceIdListSql(PlainSelect plainSelect) {
        Table mainTable = (Table)plainSelect.getFromItem();
        List<OrderByElement> orderByElements = new ArrayList<>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.withExpression(new Column(mainTable, "id")).withAsc(true);
        orderByElements.add(orderByElement);
        plainSelect.withOrderByElements(orderByElements);
        plainSelect.withDistinct(new Distinct()).setSelectItems(Arrays.asList((new SelectExpressionItem(new Column(mainTable, "id")))));
        return plainSelect.toString();
    }

    @Override
    public String getResourceIdListSql(PlainSelect plainSelect, int startNum, int pageSize) {
        getResourceIdListSql(plainSelect);
        plainSelect.withLimit(new Limit().withOffset(new LongValue(startNum)).withRowCount(new LongValue(pageSize)));
        return plainSelect.toString();
    }

    @Override
    public String getResourceIdSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList) {
        return getResourceIdListSql(mainResourceId, biConsumerList, 0, 1);
    }

    @Override
    public String getResourceListSql(PlainSelect plainSelect, List<ResourceInfo> unavailableResourceInfoList, List<ResourceInfo> theadList) {
        long startTime = System.currentTimeMillis();
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        System.out.println("A=" + (System.currentTimeMillis() - startTime));
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        System.out.println("B=" + (System.currentTimeMillis() - startTime));
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        plainSelect.setSelectItems(null);
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        return plainSelect.toString();
    }

    @Override
    public String getResourceListByIdListSql(List<ResourceInfo> theadList, List<Long> idList, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId) {
        if (CollectionUtils.isEmpty(idList)) {
            return null;
        }
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
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
        return plainSelect.toString();
    }

    @Override
    public List<ResourceVo> getResourceList(
            List<BiConsumer<ResourceSearchGenerateSqlUtil,
                    PlainSelect>> biConsumerList,
            BasePageVo basePageVo,
            List<ResourceInfo> unavailableResourceInfoList,
            String mainResourceId,
            List<ResourceInfo> theadList
    ) {
        List<ResourceVo> resourceList = new ArrayList<>();
        PlainSelect plainSelect = getResourceCountPlainSelect(mainResourceId, biConsumerList);
        if (plainSelect == null) {
            return resourceList;
        }
        int rowNum = getCount(plainSelect.toString());
        if (rowNum == 0) {
            return resourceList;
        }
        basePageVo.setRowNum(rowNum);
        String sql = getResourceIdListSql(plainSelect);
        List<Long> idList = getIdList(sql);
        if (CollectionUtils.isEmpty(idList)) {
            return resourceList;
        }
        sql = getResourceListByIdListSql(theadList, idList, unavailableResourceInfoList, mainResourceId);
        if (StringUtils.isBlank(sql)) {
            return resourceList;
        }
        resourceList = getResourceList(sql);
        return resourceList;
    }

    @Override
    public String getResourceListSql(
            List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList,
            List<ResourceInfo> unavailableResourceInfoList,
            String mainResourceId,
            List<ResourceInfo> theadList) {
        PlainSelect plainSelect = getResourceCountPlainSelect(mainResourceId, biConsumerList);
        if (plainSelect == null) {
            return null;
        }
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        plainSelect.setSelectItems(null);
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        return plainSelect.toString();
    }

    @Override
    public List<ResourceEntityVo> getResourceEntityList() {
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        return builder.getResourceEntityList();
    }

    @Override
    public BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByCommonCondition(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                Map<String, ResourceInfo> searchConditionMappingMap = new HashMap<>();
                searchConditionMappingMap.put("typeIdList", new ResourceInfo("resource_ipobject","type_id", false));
                searchConditionMappingMap.put("stateIdList", new ResourceInfo("resource_ipobject_state","state_id", false));
                searchConditionMappingMap.put("envIdList", new ResourceInfo("resource_softwareservice_env","env_id", false));
                searchConditionMappingMap.put("appSystemIdList", new ResourceInfo("resource_appmodule_appsystem","app_system_id", false));
                searchConditionMappingMap.put("appModuleIdList", new ResourceInfo("resource_ipobject_appmodule","app_module_id", false));
                searchConditionMappingMap.put("defaultValue", new ResourceInfo("resource_ipobject","id", false));
                searchConditionMappingMap.put("inspectStatusList", new ResourceInfo("resource_ipobject","inspect_status", false));
                searchConditionMappingMap.put("name", new ResourceInfo("resource_ipobject","name", false));
                searchConditionMappingMap.put("ip", new ResourceInfo("resource_ipobject","ip", false));
                searchConditionMappingMap.put("port", new ResourceInfo("resource_softwareservice","port", false));
//        for (Map.Entry<String, ResourceInfo> entry : searchConditionMappingMap.entrySet()) {
//            String key = entry.getKey();
//            JSONArray jsonArray = paramObj.getJSONArray(key);
//            if (CollectionUtils.isNotEmpty(jsonArray)) {
//                ResourceInfo resourceInfo = entry.getValue();
//                if (additionalInformation(resourceInfo)) {
//                    Column column = addJoinTableByResourceInfo(resourceInfo, plainSelect);
//                    addWhere(plainSelect, column, new InExpression(), jsonArray);
//                } else {
//                    unavailableResourceInfoList.add(resourceInfo);
//                }
//            }
//        }

                JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
                if (CollectionUtils.isNotEmpty(defaultValue)) {
                    List<Long> idList = defaultValue.toJavaList(Long.class);
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("defaultValue");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), idList);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }

                JSONArray typeIdList = paramObj.getJSONArray("typeIdList");
                if (CollectionUtils.isNotEmpty(typeIdList)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("typeIdList");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), typeIdList);
                    }else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }

                JSONArray inspectStatusList = paramObj.getJSONArray("inspectStatusList");
                if (CollectionUtils.isNotEmpty(inspectStatusList)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("inspectStatusList");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), inspectStatusList);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }

                JSONArray stateIdList = paramObj.getJSONArray("stateIdList");
                if (CollectionUtils.isNotEmpty(stateIdList)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("stateIdList");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), stateIdList);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }

                JSONArray envIdList = paramObj.getJSONArray("envIdList");
                if (CollectionUtils.isNotEmpty(envIdList)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("envIdList");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), envIdList);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }

                JSONArray appModuleIdList = paramObj.getJSONArray("appModuleIdList");
                JSONArray appSystemIdList = paramObj.getJSONArray("appSystemIdList");
                if (CollectionUtils.isNotEmpty(appModuleIdList) || CollectionUtils.isNotEmpty(appSystemIdList)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("appModuleIdList");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                            resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), appModuleIdList);
                        }
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }
                if (CollectionUtils.isNotEmpty(appSystemIdList)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("appSystemIdList");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), appSystemIdList);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }
                String name = paramObj.getString("name");
                if (StringUtils.isNotBlank(name)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("name");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new EqualsTo(), name);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }
                String ip = paramObj.getString("ip");
                if (StringUtils.isNotBlank(ip)) {
                    ResourceInfo resourceInfo = searchConditionMappingMap.get("ip");
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new EqualsTo(), ip);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }
                String port = paramObj.getString("port");
                ResourceInfo resourceInfo = searchConditionMappingMap.get("port");
                if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                    Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                    if (StringUtils.isNotBlank(port)) {
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new EqualsTo(), port);
                    } else {
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new IsNullExpression());
                    }
                } else {
                    unavailableResourceInfoList.add(resourceInfo);
                }
                    }
                };
        return biConsumer;
    }

    @Override
    public int getCount(String sql) {
        return resourceMapper.getResourceCount(sql);
    }

    @Override
    public List<Long> getIdList(String sql) {
        return resourceMapper.getResourceIdList(sql);
    }

    @Override
    public Long getId(String sql) {
        return resourceMapper.getResourceId(sql);
    }

    @Override
    public List<ResourceVo> getResourceList(String sql) {
        return resourceMapper.getResourceListByIdList(sql);
    }

}
