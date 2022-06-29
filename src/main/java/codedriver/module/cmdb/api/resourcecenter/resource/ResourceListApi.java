/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceListApiCrossoverService;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigNotFoundException;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
import codedriver.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import codedriver.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询资源中心数据列表接口
 *
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceListApi extends PrivateApiComponentBase implements IResourceListApiCrossoverService {

    private String mainResourceId = "resource_ipobject";

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/list";
    }

    @Override
    public String getName() {
        return "查询资源中心数据列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "protocolIdList", type = ApiParamType.JSONARRAY, desc = "协议id列表"),
            @Param(name = "stateIdList", type = ApiParamType.JSONARRAY, desc = "状态id列表"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "资源类型id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, desc = "标签id列表"),
            @Param(name = "inspectStatusList", type = ApiParamType.JSONARRAY, desc = "巡检状态列表"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的资源ID列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表"),
            @Param(name = "errorList", explode = ResourceEntityVo[].class, desc = "数据初始化配置异常信息列表"),
            @Param(name = "unavailableResourceInfoList", explode = ResourceInfo[].class, desc = "数据初始化配置异常信息列表")
    })
    @Description(desc = "查询资源中心数据列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ResourceVo> resourceVoList = new ArrayList<>();
        ResourceSearchVo searchVo;
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            searchVo = new ResourceSearchVo();
            searchVo.setDefaultValue(defaultValue);
        } else {
            searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj);
        }

        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
        if (configVo == null) {
            throw new ResourceCenterConfigNotFoundException();
        }
        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
        List<ResourceEntityVo> resourceEntityList = builder.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);

        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        PlainSelect filterPlainSelect = getPlainSelectBySearchCondition(searchVo, resourceSearchGenerateSqlUtil, unavailableResourceInfoList);
        if (filterPlainSelect == null) {
            return TableResultUtil.getResult(resourceVoList, searchVo);
        }
        String sql = getResourceCountSql(filterPlainSelect);
        int rowNum = resourceCenterMapper.getResourceCountNew(sql);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            sql = getResourceIdListSql(filterPlainSelect, searchVo);
            List<Long> idList = resourceCenterMapper.getResourceIdListNew(sql);
            if (CollectionUtils.isNotEmpty(idList)) {
                sql = getResourceListByIdListSql(idList, resourceSearchGenerateSqlUtil, unavailableResourceInfoList);
                if (StringUtils.isNotBlank(sql)) {
                    resourceVoList = resourceCenterMapper.getResourceListByIdListNew(sql);
                    if (CollectionUtils.isNotEmpty(resourceVoList)) {
                        resourceCenterResourceService.addResourceAccount(idList, resourceVoList);
                        resourceCenterResourceService.addResourceTag(idList, resourceVoList);
                    }
                }
            }
        }
        JSONObject resultObj = TableResultUtil.getResult(resourceVoList, searchVo);
        List<ResourceEntityVo> errorList = new ArrayList<>();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            if (StringUtils.isNotBlank(resourceEntityVo.getError())) {
                errorList.add(resourceEntityVo);
            }
        }
        if (CollectionUtils.isNotEmpty(errorList)) {
            resultObj.put("errorList", errorList);
        }
        if (CollectionUtils.isNotEmpty(unavailableResourceInfoList)) {
            resultObj.put("unavailableResourceInfoList", unavailableResourceInfoList);
        }
        return resultObj;
    }

    /**
     * 拼装查询总数sql语句
     * @param filterPlainSelect
     * @return
     */
    private String getResourceCountSql(PlainSelect filterPlainSelect) {
        Table fromTable = (Table)filterPlainSelect.getFromItem();
        filterPlainSelect.setSelectItems(Arrays.asList(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(new Column(fromTable, "id")))))));
        return filterPlainSelect.toString();
    }

    /**
     * 拼装查询当前页id列表sql语句
     * @param filterPlainSelect
     * @param searchVo
     * @return
     */
    private String getResourceIdListSql(PlainSelect filterPlainSelect, ResourceSearchVo searchVo) {
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

    /**
     * 根据查询过滤条件，生成对应的sql语句
     * @param searchVo
     * @param resourceSearchGenerateSqlUtil
     * @return
     */
    private PlainSelect getPlainSelectBySearchCondition(ResourceSearchVo searchVo, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
        Table mainTable = (Table) plainSelect.getFromItem();

        Map<String, ResourceInfo> searchConditionMappingMap = new HashMap<>();
        searchConditionMappingMap.put("typeIdList", new ResourceInfo("resource_ipobject","type_id", false));
        searchConditionMappingMap.put("stateIdList", new ResourceInfo("resource_ipobject_state","state_id", false));
        searchConditionMappingMap.put("envIdList", new ResourceInfo("resource_softwareservice_env","env_id", false));
        searchConditionMappingMap.put("appSystemIdList", new ResourceInfo("resource_appmodule_appsystem","app_system_id", false));
        searchConditionMappingMap.put("appModuleIdList", new ResourceInfo("resource_ipobject_appmodule","app_module_id", false));
        searchConditionMappingMap.put("defaultValue", new ResourceInfo("resource_ipobject","id", false));
        searchConditionMappingMap.put("inspectStatusList", new ResourceInfo("resource_ipobject","inspect_status", false));
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            ResourceInfo resourceInfo = searchConditionMappingMap.get("defaultValue");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
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
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> typeIdList = searchVo.getTypeIdList();
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("typeIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
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
            }else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<String> inspectStatusList = searchVo.getInspectStatusList();
        if (CollectionUtils.isNotEmpty(inspectStatusList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("inspectStatusList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
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
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> stateIdList = searchVo.getStateIdList();
        if (CollectionUtils.isNotEmpty(stateIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("stateIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
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
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> envIdList = searchVo.getEnvIdList();
        if (CollectionUtils.isNotEmpty(envIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("envIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
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
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> appModuleIdList = searchVo.getAppModuleIdList();
        List<Long> appSystemIdList = searchVo.getAppSystemIdList();
        if (CollectionUtils.isNotEmpty(appModuleIdList) || CollectionUtils.isNotEmpty(appSystemIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("appModuleIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                if (CollectionUtils.isNotEmpty(appModuleIdList)) {
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
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            ResourceInfo resourceInfo = searchConditionMappingMap.get("appSystemIdList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
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
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        List<Long> protocolIdList = searchVo.getProtocolIdList();
        if (CollectionUtils.isNotEmpty(protocolIdList)) {
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
        List<Long> tagIdList = searchVo.getTagIdList();
        if (CollectionUtils.isNotEmpty(tagIdList)) {
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
        String keyword = searchVo.getKeyword();
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
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(multiOrExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, multiOrExpression));
            }
        }
        return plainSelect;
    }

    /**
     * 根据需要查询的列，生成对应的sql语句
     * @param idList
     * @param resourceSearchGenerateSqlUtil
     * @return
     */
    public String getResourceListByIdListSql(List<Long> idList, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
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
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
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
}
