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
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
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

    /**
     * 根据查询条件组装查询资源总个数的PlainSelect对象
     * @param mainResourceId
     * @param biConsumerList
     * @return
     */
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

    /**
     * 根据查询条件组装查询资源总个数的sql语句
     * @param mainResourceId
     * @param biConsumerList
     * @return
     */
    @Override
    public String getResourceCountSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList) {
        PlainSelect plainSelect = getResourceCountPlainSelect(mainResourceId, biConsumerList);
        if (plainSelect == null) {
            return null;
        }
        return plainSelect.toString();
    }

    /**
     * 根据查询条件组装查询当前页id列表的sql语句
     * @param mainResourceId
     * @param biConsumerList
     * @param startNum
     * @param pageSize
     * @return
     */
    @Override
    public String getResourceIdListSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList, int startNum, int pageSize) {
        PlainSelect plainSelect = getResourceCountPlainSelect(mainResourceId, biConsumerList);
        if (plainSelect == null) {
            return null;
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

    /**
     * 根据查询条件组装查询当前页id列表的sql语句
     * @param plainSelect
     * @return
     */
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

    /**
     * 根据查询条件组装查询当前页id列表的sql语句
     * @param plainSelect
     * @param startNum
     * @param pageSize
     * @return
     */
    @Override
    public String getResourceIdListSql(PlainSelect plainSelect, int startNum, int pageSize) {
        getResourceIdListSql(plainSelect);
        plainSelect.withLimit(new Limit().withOffset(new LongValue(startNum)).withRowCount(new LongValue(pageSize)));
        return plainSelect.toString();
    }

    /**
     * 根据查询条件组装查询只返回一个id的sql语句
     * @param mainResourceId
     * @param biConsumerList
     * @return
     */
    @Override
    public String getResourceIdSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList) {
        return getResourceIdListSql(mainResourceId, biConsumerList, 0, 1);
    }

    /**
     * 根据需要查询的列，生成对应的sql语句
     * @param plainSelect
     * @param theadList
     * @param unavailableResourceInfoList
     * @return
     */
    @Override
    public String getResourceListSql(PlainSelect plainSelect, List<ResourceInfo> theadList, List<ResourceInfo> unavailableResourceInfoList) {
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

    /**
     * 根据需要查询的列，生成对应的sql语句
     * @param mainResourceId
     * @param theadList
     * @param idList
     * @param unavailableResourceInfoList
     * @return
     */
    @Override
    public String getResourceListByIdListSql(String mainResourceId, List<ResourceInfo> theadList, List<Long> idList, List<ResourceInfo> unavailableResourceInfoList) {
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

    /**
     * 根据需要查询的列和查询条件，生成对应的sql语句执行，返回ResourceVo列表
     * @param mainResourceId
     * @param theadList
     * @param biConsumerList
     * @param basePageVo
     * @param unavailableResourceInfoList
     * @return
     */
    @Override
    public List<ResourceVo> getResourceList(
            String mainResourceId,
            List<ResourceInfo> theadList,
            List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList,
            BasePageVo basePageVo,
            List<ResourceInfo> unavailableResourceInfoList
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
        sql = getResourceListByIdListSql(mainResourceId, theadList, idList, unavailableResourceInfoList);
        if (StringUtils.isBlank(sql)) {
            return resourceList;
        }
        resourceList = getResourceList(sql);
        return resourceList;
    }

    /**
     * 根据需要查询的列和查询条件，生成对应的sql语句
     * @param mainResourceId
     * @param theadList
     * @param biConsumerList
     * @param unavailableResourceInfoList
     * @return
     */
    @Override
    public String getResourceListSql(
            String mainResourceId,
            List<ResourceInfo> theadList,
            List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList,
            List<ResourceInfo> unavailableResourceInfoList) {
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

    /**
     * 获取数据初始化配置信息中的视图列表信息
     * @return
     */
    @Override
    public List<ResourceEntityVo> getResourceEntityList() {
        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        return builder.getResourceEntityList();
    }

    /**
     * 查询个数
     * @param sql
     * @return
     */
    @Override
    public int getCount(String sql) {
        return resourceMapper.getResourceCount(sql);
    }

    /**
     * 查询id列表
     * @param sql
     * @return
     */
    @Override
    public List<Long> getIdList(String sql) {
        return resourceMapper.getResourceIdList(sql);
    }

    /**
     * 查询id
     * @param sql
     * @return
     */
    @Override
    public Long getId(String sql) {
        return resourceMapper.getResourceId(sql);
    }

    /**
     * 查询资源列表
     * @param sql
     * @return
     */
    @Override
    public List<ResourceVo> getResourceList(String sql) {
        return resourceMapper.getResourceListByIdList(sql);
    }

}
