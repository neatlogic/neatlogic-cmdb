/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.service.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.IResourceBuildSqlCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountComponentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceConditionConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.*;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceViewFieldMappingException;
import neatlogic.framework.cmdb.utils.ResourceViewGenerateSqlUtil;
import neatlogic.framework.cmdb.utils.ResourceViewGenerateSqlUtilForTiDB;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.dto.DataBaseViewInfoVo;
import neatlogic.framework.sqlgenerator.$sql;
import neatlogic.framework.sqlgenerator.ExpressionVo;
import neatlogic.framework.sqlgenerator.JoinVo;
import neatlogic.framework.sqlgenerator.SqlVo;
import neatlogic.framework.store.mysql.DatabaseVendor;
import neatlogic.framework.store.mysql.DatasourceManager;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ResourceBuildSqlServiceImpl implements ResourceBuildSqlService, IResourceBuildSqlCrossoverService {

    private final Logger logger = LoggerFactory.getLogger(ResourceBuildSqlServiceImpl.class);
    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private SchemaMapper schemaMapper;

    @Resource
    private DataBaseViewInfoMapper dataBaseViewInfoMapper;

    @Override
    public String buildResourceView(ResourceEntityVo resourceEntityVo) {
        String viewName = resourceEntityVo.getName();
        String select = null;
        String error = StringUtils.EMPTY;
        try {
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            select = plainSelect.toString();
            String md5 = Md5Util.encryptMD5(select);
            boolean needCreateView = true;
            String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
            if (Objects.equals(tableType, "VIEW")) {
                DataBaseViewInfoVo dataBaseViewInfoVo = dataBaseViewInfoMapper.getDataBaseViewInfoByViewName(viewName);
                if (dataBaseViewInfoVo != null) {
                    // md5相同就不用更新视图了
                    if (Objects.equals(md5, dataBaseViewInfoVo.getMd5())) {
                        try {
                            resourceEntityMapper.getResourceEntityViewDataList(viewName, 0, 1);
                            needCreateView = false;
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
            if (needCreateView) {
                String selectSql = select;
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                    if (Objects.equals(tableType, "BASE TABLE")) {
                        schemaMapper.deleteTable(TenantContext.get().getDataDbName() + "." + viewName);
                    }
                    String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + viewName + " AS " + selectSql;
                    schemaMapper.insertView(sql);
                }).execute();
                if (s.isSucceed()) {
                    DataBaseViewInfoVo dataBaseViewInfoVo = new DataBaseViewInfoVo();
                    dataBaseViewInfoVo.setViewName(viewName);
                    dataBaseViewInfoVo.setMd5(md5);
                    dataBaseViewInfoVo.setLcu(UserContext.get().getUserUuid());
                    dataBaseViewInfoMapper.insertDataBaseViewInfo(dataBaseViewInfoVo);
                } else {
                    error = s.getError();
                }
            }
        } catch (Exception ex) {
            error = ExceptionUtils.getStackTrace(ex);
        } finally {
            if (StringUtils.isNotBlank(error)) {
                String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
                if (!Objects.equals(tableType, "BASE TABLE")) {
                    EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                        schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + viewName);
                        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
                        Table table = new Table();
                        table.setName(viewName);
                        table.setSchemaName(TenantContext.get().getDataDbName());
                        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
                        for (String columnName : fieldNameList) {
                            ColumnDefinition columnDefinition = new ColumnDefinition();
                            columnDefinition.setColumnName(columnName);
                            columnDefinition.setColDataType(new ColDataType("int"));
                            columnDefinitions.add(columnDefinition);
                        }
                        CreateTable createTable = new CreateTable();
                        createTable.setTable(table);
                        createTable.setColumnDefinitions(columnDefinitions);
                        createTable.setIfNotExists(true);
                        schemaMapper.insertView(createTable.toString());
                    }).execute();
                }
                resourceEntityVo.setError(error);
            }
        }
        return select;
    }
    
    @Override
    public String buildGetResourceIdListSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> filterItemFieldNameList = new ArrayList<>();
            ResourceQueryCriteriaVo preConditionQueryCriteriaVo = null;
            ResourceSearchVo preCondition = searchVo.getPreCondition();
            if (preCondition != null) {
                preConditionQueryCriteriaVo = new ResourceQueryCriteriaVo(preCondition);
                filterItemFieldNameList.addAll(getFilterItemFieldNameList(preConditionQueryCriteriaVo));
            }
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            filterItemFieldNameList.addAll(getFilterItemFieldNameList(queryCriteriaVo));
            filterItemFieldNameList.add("id");
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = new SqlVo();
            if (preConditionQueryCriteriaVo != null) {
                getSqlVoForResource(sqlVo, preConditionQueryCriteriaVo, fieldName2ColumnMap);
            }
            getSqlVoForResource(sqlVo, queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            if (CollectionUtils.isNotEmpty(searchVo.getKeywordList()) && searchVo.getNameFieldAttrId() != null && searchVo.getIpFieldAttrId() != null) {
                $sql.addOrderBy(plainSelect, $sql.fun("COUNT", "fw.word").withDistinct(true), "desc");
            }
            Column idColumn = fieldName2ColumnMap.get("id");
            // 分组
            $sql.addGroupBy(plainSelect, idColumn.toString());
            // 排序
            $sql.addOrderBy(plainSelect, idColumn.toString(), "desc");
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceCountSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> filterItemFieldNameList = new ArrayList<>();
            ResourceQueryCriteriaVo preConditionQueryCriteriaVo = null;
            ResourceSearchVo preCondition = searchVo.getPreCondition();
            if (preCondition != null) {
                preConditionQueryCriteriaVo = new ResourceQueryCriteriaVo(preCondition);
                filterItemFieldNameList.addAll(getFilterItemFieldNameList(preConditionQueryCriteriaVo));
            }
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            filterItemFieldNameList.addAll(getFilterItemFieldNameList(queryCriteriaVo));
            filterItemFieldNameList.add("id");
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = new SqlVo();
            if (preConditionQueryCriteriaVo != null) {
                getSqlVoForResource(sqlVo, preConditionQueryCriteriaVo, fieldName2ColumnMap);
            }
            getSqlVoForResource(sqlVo, queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            Column column = fieldName2ColumnMap.get("id");
            $sql.setSelectColumn(plainSelect, $sql.fun("COUNT", column.toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceListSql(List<Long> idList, List<String> selectFieldNameList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(idList)) {
                filterItemFieldNameList.add("id");
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column column = fieldName2ColumnMap.get("id");
            $sql.addWhereExpression(plainSelect, $sql.exp(column.toString(), "in", idList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceListSql(List<Long> idList) {
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
        fieldNameList.remove("env_seq_no");
        fieldNameList.remove("vendor_id");
        fieldNameList.remove("vendor_name");
        fieldNameList.remove("vendor_label");
        fieldNameList.remove("datacenter_id");
        fieldNameList.remove("datacenter_name");
        fieldNameList.remove("fcu");
        fieldNameList.remove("fcd");
        fieldNameList.remove("lcu");
        fieldNameList.remove("lcd");
        return buildGetResourceListSql(idList, fieldNameList);
    }

    @Override
    public String buildGetResourceCountByNameKeywordSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setInspectJobPhaseNodeStatusList(null);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            Column column = fieldName2ColumnMap.get("id");
            $sql.setSelectColumn(plainSelect, $sql.fun("COUNT", column.toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceCountByIpKeywordSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setInspectJobPhaseNodeStatusList(null);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            Column column = fieldName2ColumnMap.get("id");
            $sql.setSelectColumn(plainSelect, $sql.fun("COUNT", column.toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAuthResourceListSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("port");
            selectItemFieldNameList.add("type_id");
            selectItemFieldNameList.add("type_name");
            selectItemFieldNameList.add("type_label");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceListByIpAndPortAndNameWithFilterSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("port");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            String keyword = searchVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                keyword = "%" + keyword + "%";
                Column nameColumn = fieldName2ColumnMap.get("name");
                Column ipColumn = fieldName2ColumnMap.get("ip");
                $sql.addWhereExpression(plainSelect,
                        $sql.exp("(",
                                $sql.exp(nameColumn.toString(), "like", $sql.value(keyword)),
                                "OR",
                                $sql.exp(ipColumn.toString(), "like", $sql.value(keyword)),
                                ")"));
            }
            List<ResourceVo> inputNodeList = searchVo.getInputNodeList();
            if (CollectionUtils.isNotEmpty(inputNodeList)) {
                ExpressionVo orExp = null;
                for (ResourceVo inputNode : inputNodeList) {
                    Column ipColumn = fieldName2ColumnMap.get("ip");
                    ExpressionVo andExp = $sql.exp(ipColumn.toString(), "=", $sql.value(inputNode.getIp()));
                    Column portColumn = fieldName2ColumnMap.get("port");
                    if (inputNode.getPort() != null) {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "=", inputNode.getPort()));
                    } else {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "is null"));
                    }
                    if (StringUtils.isNotBlank(inputNode.getName())) {
                        Column nameColumn = fieldName2ColumnMap.get("name");
                        andExp = $sql.exp(andExp, "and", $sql.exp(nameColumn.toString(), "=", $sql.value(inputNode.getName())));
                    }
                    andExp = $sql.exp("(", andExp, ")");
                    if (orExp == null) {
                        orExp = andExp;
                    } else {
                        orExp = $sql.exp(orExp, "or", andExp);
                    }
                }
                orExp = $sql.exp("(", orExp, ")");
                $sql.addWhereExpression(plainSelect, orExp);
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(1, "=", 0));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceTypeIdListByAuthSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("type_id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.setDistinct(plainSelect, true);
            Column typeIdColumn = fieldName2ColumnMap.get("type_id");
            $sql.setSelectColumn(plainSelect, typeIdColumn.toString());
            String keyword = searchVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                keyword = "%" + keyword + "%";
                Column nameColumn = fieldName2ColumnMap.get("name");
                Column ipColumn = fieldName2ColumnMap.get("ip");
                $sql.addWhereExpression(plainSelect,
                        $sql.exp("(",
                                $sql.exp(nameColumn.toString(), "like", $sql.value(keyword)),
                                "OR",
                                $sql.exp(ipColumn.toString(), "like", $sql.value(keyword)),
                                ")"));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceIdByIpAndPortAndNameSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            if (Objects.equals(searchVo.getIsHasAuth(), false)) {
                ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo();
                queryCriteriaVo.setIsHasAuth(false);
                if (StringUtils.isNotBlank(searchVo.getCmdbGroupType())) {
                    queryCriteriaVo.setCmdbGroupType(searchVo.getCmdbGroupType());
                }
                SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
                $sql.addSql(plainSelect, sqlVo);
            }
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "=", $sql.value(searchVo.getIp())));
            if (StringUtils.isNotBlank(searchVo.getPort())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("port").toString(), "=", $sql.value(searchVo.getPort())));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("port").toString(), "is null"));
            }
            if (StringUtils.isNotBlank(searchVo.getName())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("name").toString(), "=", $sql.value(searchVo.getName())));
            }
            $sql.setLimit(plainSelect, 1);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceIdListByIpAndPortAndNameSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            if (Objects.equals(searchVo.getIsHasAuth(), false)) {
                ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo();
                queryCriteriaVo.setIsHasAuth(false);
                if (StringUtils.isNotBlank(searchVo.getCmdbGroupType())) {
                    queryCriteriaVo.setCmdbGroupType(searchVo.getCmdbGroupType());
                }
                SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
                $sql.addSql(plainSelect, sqlVo);
            }
            List<ResourceVo> inputNodeList = searchVo.getInputNodeList();
            if (CollectionUtils.isNotEmpty(inputNodeList)) {
                ExpressionVo orExp = null;
                for (ResourceVo inputNode : inputNodeList) {
                    Column ipColumn = fieldName2ColumnMap.get("ip");
                    ExpressionVo andExp = $sql.exp(ipColumn.toString(), "=", $sql.value(inputNode.getIp()));
                    Column portColumn = fieldName2ColumnMap.get("port");
                    if (inputNode.getPort() != null) {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "=", inputNode.getPort()));
                    } else {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "is null"));
                    }
                    if (StringUtils.isNotBlank(inputNode.getName())) {
                        Column nameColumn = fieldName2ColumnMap.get("name");
                        andExp = $sql.exp(andExp, "and", $sql.exp(nameColumn.toString(), "=", $sql.value(inputNode.getName())));
                    }
                    andExp = $sql.exp("(", andExp, ")");
                    if (orExp == null) {
                        orExp = andExp;
                    } else {
                        orExp = $sql.exp(orExp, "or", andExp);
                    }
                }
                orExp = $sql.exp("(", orExp, ")");
                $sql.addWhereExpression(plainSelect, orExp);
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(1, "=", 0));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceListByIpAndPortAndNameSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("port");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_name");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_name").toString(), "typeName");
            if (Objects.equals(searchVo.getIsHasAuth(), false)) {
                ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo();
                queryCriteriaVo.setIsHasAuth(false);
                if (StringUtils.isNotBlank(searchVo.getCmdbGroupType())) {
                    queryCriteriaVo.setCmdbGroupType(searchVo.getCmdbGroupType());
                }
                SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
                $sql.addSql(plainSelect, sqlVo);
            }
            List<ResourceVo> inputNodeList = searchVo.getInputNodeList();
            if (CollectionUtils.isNotEmpty(inputNodeList)) {
                ExpressionVo orExp = null;
                for (ResourceVo inputNode : inputNodeList) {
                    Column ipColumn = fieldName2ColumnMap.get("ip");
                    ExpressionVo andExp = $sql.exp(ipColumn.toString(), "=", $sql.value(inputNode.getIp()));
                    Column portColumn = fieldName2ColumnMap.get("port");
                    if (inputNode.getPort() != null) {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "=", inputNode.getPort()));
                    } else {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "is null"));
                    }
                    if (StringUtils.isNotBlank(inputNode.getName())) {
                        Column nameColumn = fieldName2ColumnMap.get("name");
                        andExp = $sql.exp(andExp, "and", $sql.exp(nameColumn.toString(), "=", $sql.value(inputNode.getName())));
                    }
                    andExp = $sql.exp("(", andExp, ")");
                    if (orExp == null) {
                        orExp = andExp;
                    } else {
                        orExp = $sql.exp(orExp, "or", andExp);
                    }
                }
                orExp = $sql.exp("(", orExp, ")");
                $sql.addWhereExpression(plainSelect, orExp);
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(1, "=", 0));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceByIdListSql(List<Long> idList) {
        List<String> fieldNameList = new ArrayList<>();
        fieldNameList.add("id");
        fieldNameList.add("name");
        fieldNameList.add("ip");
        fieldNameList.add("port");
        fieldNameList.add("type_id");
        fieldNameList.add("type_name");
        fieldNameList.add("type_label");
        return buildGetResourceListSql(idList, fieldNameList);
    }

    @Override
    public String buildGetResourceByIdSql(Long id, List<String> selectFieldNameList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column column = fieldName2ColumnMap.get("id");
            $sql.addWhereExpression(plainSelect, $sql.exp(column.toString(), "=", id));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceByIdSql(Long id) {
        List<String> fieldNameList = new ArrayList<>();
        fieldNameList.add("id");
        fieldNameList.add("name");
        fieldNameList.add("ip");
        fieldNameList.add("port");
        fieldNameList.add("type_id");
        fieldNameList.add("type_name");
        fieldNameList.add("type_label");
        return buildGetResourceByIdSql(id, fieldNameList);
    }

    @Override
    public String buildGetResourceIdByResourceIdSql(Long id) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column column = fieldName2ColumnMap.get("id");
            $sql.addWhereExpression(plainSelect, $sql.exp(column.toString(), "=", id));
            $sql.setLimit(plainSelect, 1);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildCheckResourceIdListIsExistsSql(List<Long> idList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column column = fieldName2ColumnMap.get("id");
            $sql.addWhereExpression(plainSelect, $sql.exp(column.toString(), "in", idList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceIdListByAppSystemIdAndModuleIdAndEnvIdSql(ResourceVo resourceVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column appSystemIdColumn = fieldName2ColumnMap.get("app_system_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(appSystemIdColumn.toString(), "=", resourceVo.getAppSystemId()));
            Column appModuleIdColumn = fieldName2ColumnMap.get("app_module_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(appModuleIdColumn.toString(), "=", resourceVo.getAppModuleId()));
            Column envIdColumn = fieldName2ColumnMap.get("env_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(envIdColumn.toString(), "=", resourceVo.getEnvId()));
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.addOrderBy(plainSelect, idColumn.toString(), "desc");
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceListByTypeIdListAndIpListSql(List<Long> typeIdList, List<String> ipList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("name");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("type_id");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column typeIdColumn = fieldName2ColumnMap.get("type_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(typeIdColumn.toString(), "in", typeIdList));
            Column ipColumn = fieldName2ColumnMap.get("ip");
            $sql.addWhereExpression(plainSelect, $sql.exp(ipColumn.toString(), "in", ipList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceByIpAndPortAndNameAndTypeNameSql(String ip, Integer port, String name, String typeName) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("port");
//            selectItemFieldNameList.add("type_name");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_name");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_name").toString(), "typeName");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "=", $sql.value(ip)));
            if (port != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("port").toString(), "=", port));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("port").toString(), "is null"));
            }
            if (StringUtils.isNotBlank(name)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("name").toString(), "=", $sql.value(name)));
            }
            if (StringUtils.isNotBlank(typeName)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("type_name").toString(), "=", $sql.value(typeName)));
            }
            $sql.setLimit(plainSelect, 1);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceByIpAndPortSql(String ip, Integer port) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("ip");
            selectItemFieldNameList.add("port");
//            selectItemFieldNameList.add("type_name");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_name");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_name").toString(), "typeName");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "=", $sql.value(ip)));
            if (port != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("port").toString(), "=", port));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("port").toString(), "is null"));
            }
            $sql.setLimit(plainSelect, 1);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildSearchAccountComponentSql(AccountComponentVo accountComponentVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            selectItemFieldNameList.add("name");
            selectItemFieldNameList.add("ip");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addJoin(plainSelect, $sql.join("join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addJoin(plainSelect, $sql.join("join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            $sql.addJoin(plainSelect, $sql.join("join", "cmdb_resourcecenter_account_protocol", "d").withOn($sql.exp("d.id", "=", "c.protocol_id")));

            $sql.addSelectColumn(plainSelect, "c.id", "accountId");
            $sql.addSelectColumn(plainSelect, "c.name", "accountName");
            $sql.addSelectColumn(plainSelect, "c.account", "account");
            $sql.addSelectColumn(plainSelect, "d.id", "protocolId");
            $sql.addSelectColumn(plainSelect, "d.name", "protocol");
            $sql.addSelectColumn(plainSelect, "d.port", "port");
            String keyword = accountComponentVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                Column nameColumn = fieldName2ColumnMap.get("name");
                Column ipColumn = fieldName2ColumnMap.get("ip");
                $sql.addWhereExpression(plainSelect,
                        $sql.exp("(",
                                $sql.exp(nameColumn.toString(), "like", "'%" + keyword + "%'"),
                                "OR",
                                $sql.exp(ipColumn.toString(), "=", $sql.value(keyword)),
                                ")"
                        )
                );
            }
            $sql.setLimit(plainSelect, accountComponentVo.getStartNum(), accountComponentVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildSearchAccountComponentCountSql(AccountComponentVo accountComponentVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addJoin(plainSelect, $sql.join("join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addJoin(plainSelect, $sql.join("join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            $sql.addJoin(plainSelect, $sql.join("join", "cmdb_resourcecenter_account_protocol", "d").withOn($sql.exp("d.id", "=", "c.protocol_id")));
            $sql.addSelectColumn(plainSelect, $sql.fun("count", "b.resource_id", "b.account_id").withDistinct(true));
            String keyword = accountComponentVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                Column nameColumn = fieldName2ColumnMap.get("name");
                Column ipColumn = fieldName2ColumnMap.get("ip");
                $sql.addWhereExpression(plainSelect,
                        $sql.exp("(",
                                $sql.exp(nameColumn.toString(), "like", "'%" + keyword + "%'"),
                                "OR",
                                $sql.exp(ipColumn.toString(), "=", $sql.value(keyword)),
                                ")"
                        )
                );
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppEnvListByAppSystemIdAndAppModuleIdSql(Long appSystemId, Long appModuleId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("env_name");
            filterItemFieldNameList.add("env_seq_no");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_id").toString(), "id");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_name").toString(), "name");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("env_seq_no").toString(), "seqNo");
            Column appSystemIdColumn = fieldName2ColumnMap.get("app_system_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(appSystemIdColumn.toString(), "=", appSystemId));
            Column appModuleIdColumn = fieldName2ColumnMap.get("app_module_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(appModuleIdColumn.toString(), "=", appModuleId));
            Column envIdColumn = fieldName2ColumnMap.get("env_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(envIdColumn.toString(), "is not null"));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppEnvCountMapByAppSystemIdGroupByAppModuleIdSql(Long appSystemId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_id").toString(), "appModuleId");
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("env_id").toString()).withDistinct(true), "count");
            Column appSystemIdColumn = fieldName2ColumnMap.get("app_system_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(appSystemIdColumn.toString(), "=", appSystemId));
            Column envIdColumn = fieldName2ColumnMap.get("env_id");
            $sql.addWhereExpression(plainSelect, $sql.exp(envIdColumn.toString(), "is not null"));
            Column appModuleIdColumn = fieldName2ColumnMap.get("app_module_id");
            $sql.addGroupBy(plainSelect, appModuleIdColumn.toString());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceCountByDynamicConditionSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> filterItemFieldNameList1 = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.addAll(filterItemFieldNameList1);
            JSONObject conditionConfigObj = new JSONObject();
            conditionConfigObj.put("conditionGroupList", searchVo.getConditionGroupList());
            conditionConfigObj.put("conditionGroupRelList", searchVo.getConditionGroupRelList());
            ResourceConditionConfigVo resourceConditionConfigVo = conditionConfigObj.toJavaObject(ResourceConditionConfigVo.class);
            List<String> filterItemFieldNameList2 = resourceConditionConfigVo.getFilterItemFieldNameList();
            filterItemFieldNameList.addAll(filterItemFieldNameList2);
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            resourceConditionConfigVo.buildConditionSqlVo(sqlVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetResourceIdListByDynamicConditionSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> filterItemFieldNameList1 = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.addAll(filterItemFieldNameList1);
            JSONObject conditionConfigObj = new JSONObject();
            conditionConfigObj.put("conditionGroupList", searchVo.getConditionGroupList());
            conditionConfigObj.put("conditionGroupRelList", searchVo.getConditionGroupRelList());
            ResourceConditionConfigVo resourceConditionConfigVo = conditionConfigObj.toJavaObject(ResourceConditionConfigVo.class);
            List<String> filterItemFieldNameList2 = resourceConditionConfigVo.getFilterItemFieldNameList();
            filterItemFieldNameList.addAll(filterItemFieldNameList2);
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
//            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForResource(queryCriteriaVo, fieldName2ColumnMap);
            resourceConditionConfigVo.buildConditionSqlVo(sqlVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppResourceCountSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(searchVo.getViewName());
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("type_id");
            filterItemFieldNameList.add("inspect_status");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            if (searchVo.getAppSystemId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", searchVo.getAppSystemId()));
            }
            if (searchVo.getAppModuleId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "=", searchVo.getAppModuleId()));
            }
            if (searchVo.getEnvId() != null) {
                if (searchVo.getEnvId() != -2) {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "=", searchVo.getEnvId()));
                } else {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is null"));
                }
            }
            if (searchVo.getTypeId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "=", searchVo.getTypeId()));
            }
            if (CollectionUtils.isNotEmpty(searchVo.getInspectStatusList())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", searchVo.getInspectStatusList()));
            }
            Column column = fieldName2ColumnMap.get("id");
            $sql.setSelectColumn(plainSelect, $sql.fun("COUNT", column.toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppResourceIdListSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(searchVo.getViewName());
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("type_id");
            filterItemFieldNameList.add("inspect_status");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            if (searchVo.getAppSystemId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", searchVo.getAppSystemId()));
            }
            if (searchVo.getAppModuleId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "=", searchVo.getAppModuleId()));
            }
            if (searchVo.getEnvId() != null) {
                if (searchVo.getEnvId() != -2) {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "=", searchVo.getEnvId()));
                } else {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is null"));
                }
            }
            if (searchVo.getTypeId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "=", searchVo.getTypeId()));
            }
            if (CollectionUtils.isNotEmpty(searchVo.getInspectStatusList())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", searchVo.getInspectStatusList()));
            }
            $sql.setDistinct(plainSelect, true);
            Column column = fieldName2ColumnMap.get("id");
            $sql.addOrderBy(plainSelect, column.toString(), "desc");
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppResourceListByIdListSql(ResourceSearchVo searchVo, List<String> selectFieldNameList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(searchVo.getViewName());
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("type_id");
            filterItemFieldNameList.add("inspect_status");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            if (searchVo.getAppSystemId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", searchVo.getAppSystemId()));
            }
            if (searchVo.getAppModuleId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "=", searchVo.getAppModuleId()));
            }
            if (searchVo.getEnvId() != null) {
                if (searchVo.getEnvId() != -2) {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "=", searchVo.getEnvId()));
                } else {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is null"));
                }
            }
            if (searchVo.getTypeId() != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "=", searchVo.getTypeId()));
            }
            if (CollectionUtils.isNotEmpty(searchVo.getInspectStatusList())) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", searchVo.getInspectStatusList()));
            }
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("id").toString(), "in", searchVo.getIdList()));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppResourceListByIdListSql(ResourceSearchVo searchVo) {
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(searchVo.getViewName());
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName(resourceEntityVo.getConfig().getSceneTemplateName());
        return buildGetAppResourceListByIdListSql(searchVo, fieldNameList);
    }

    @Override
    public String buildGetAppEnvListByViewNameAndAppSystemIdAndAppModuleIdAndInspectStatusListSql(String viewName, Long appSystemId, Long appModuleId, List<String> inspectStatusList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(viewName);
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("app_module_name");
            filterItemFieldNameList.add("app_module_abbr_name");
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("env_name");
            filterItemFieldNameList.add("env_seq_no");
            filterItemFieldNameList.add("type_id");
            filterItemFieldNameList.add("type_name");
            filterItemFieldNameList.add("type_label");
            filterItemFieldNameList.add("inspect_status");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addSelectColumn(plainSelect, $sql.fun("IFNULL", fieldName2ColumnMap.get("env_id").toString(), -2), "id");
            $sql.addSelectColumn(plainSelect, $sql.fun("IFNULL", fieldName2ColumnMap.get("env_name").toString(), "'未配置'"), "name");
            $sql.addSelectColumn(plainSelect, $sql.fun("IFNULL", fieldName2ColumnMap.get("env_seq_no").toString(), 9999), "seqNo");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_id").toString(), "moduleId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_name").toString(), "moduleName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("app_module_abbr_name").toString(), "moduleAbbrName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_id").toString(), "typeId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_name").toString(), "typeName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_label").toString(), "typeLabel");
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", appSystemId));
            if (appModuleId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "=", appModuleId));
            } else {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "is not null"));
            }
            if (CollectionUtils.isNotEmpty(inspectStatusList)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", inspectStatusList));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppResourceTypeIdListByViewNameAndAppSystemIdSql(String viewName, Long appSystemId, Long appModuleId, Long envId, List<String> inspectStatusList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(viewName);
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            filterItemFieldNameList.add("env_id");
            filterItemFieldNameList.add("type_id");
            filterItemFieldNameList.add("inspect_status");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_id").toString());
            if (appSystemId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "=", appSystemId));
            }
            if (appModuleId != null) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "=", appModuleId));
            }
            if (envId != null) {
                if (envId != -2) {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "=", envId));
                } else {
                    $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is null"));
                }
            }
            if (CollectionUtils.isNotEmpty(inspectStatusList)) {
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", inspectStatusList));
            }
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetAppSystemIdListByIdSql(String viewName, Long id) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(viewName);
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("app_system_id");
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("app_system_id");
            filterItemFieldNameList.add("app_module_id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("id").toString(), "=", id));
            $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "is not null"));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceListByIdListSql(List<Long> idList, List<String> selectFieldNameList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(idList)) {
                filterItemFieldNameList.add("id");
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_resource_account", "crra").withOn($sql.exp("crra.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_account", "cra").withOn($sql.exp("cra.id", "=", "crra.account_id")));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_resource_tag", "crrt").withOn($sql.exp("crrt.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp(
                    $sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"),
                    "and",
                    $sql.exp("ajpn.resource_id", "=", idColumn.toString())
            )));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_tag", "ct").withOn($sql.exp("ct.id", "=", "crrt.tag_id")));
            $sql.addSelectColumn(plainSelect, "ajpn.id", "inspectJobPhaseNodeId");
            $sql.addSelectColumn(plainSelect, "ajpn.`job_id`", "jobId");
            $sql.addSelectColumn(plainSelect, "ajpn.status", "jobPhaseNodeStatus");
            $sql.addSelectColumn(plainSelect, "ct.name", "tagName");
            $sql.addWhereExpression(plainSelect, $sql.exp(idColumn.toString(), "in", idList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceListByIdListSql(List<Long> idList) {
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
        fieldNameList.remove("env_seq_no");
        fieldNameList.remove("vendor_id");
        fieldNameList.remove("vendor_name");
        fieldNameList.remove("vendor_label");
        fieldNameList.remove("datacenter_id");
        fieldNameList.remove("datacenter_name");
        fieldNameList.remove("fcu");
        fieldNameList.remove("fcd");
        fieldNameList.remove("lcu");
        fieldNameList.remove("lcd");
        return buildGetInspectResourceListByIdListSql(idList, fieldNameList);
    }

    @Override
    public String buildGetInspectResourceCountSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceCountByIpKeywordSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceCountByNameKeywordSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`name` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceIdListSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.addGroupBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            if (Objects.equals(queryCriteriaVo.getIsNameFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("name").toString()));
            } else if (Objects.equals(queryCriteriaVo.getIsIpFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("ip").toString()));
            }
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectResourceListByIdListAndJobIdSql(List<Long> idList, Long jobId) {
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
        fieldNameList.remove("env_seq_no");
        fieldNameList.remove("vendor_id");
        fieldNameList.remove("vendor_name");
        fieldNameList.remove("vendor_label");
        fieldNameList.remove("datacenter_id");
        fieldNameList.remove("datacenter_name");
        fieldNameList.remove("fcu");
        fieldNameList.remove("fcd");
        fieldNameList.remove("lcu");
        fieldNameList.remove("lcd");

        fieldNameList.remove("monitor_status");
        fieldNameList.remove("monitor_time");
        fieldNameList.remove("inspect_status");
        fieldNameList.remove("inspect_time");
        fieldNameList.remove("maintenance_window");
        fieldNameList.remove("description");
        fieldNameList.remove("app_module_id");
        fieldNameList.remove("app_module_name");
        fieldNameList.remove("app_module_abbr_name");
        fieldNameList.remove("app_system_id");
        fieldNameList.remove("app_system_name");
        fieldNameList.remove("app_system_abbr_name");
        fieldNameList.remove("state_label");
        fieldNameList.remove("env_id");
        fieldNameList.remove("env_name");
        return buildGetInspectResourceListByIdListAndJobIdSql(idList, jobId, fieldNameList);
    }

    @Override
    public String buildGetInspectResourceListByIdListAndJobIdSql(List<Long> idList, Long jobId, List<String> selectFieldNameList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(selectFieldNameList)) {
                selectItemFieldNameList.addAll(selectFieldNameList);
            }
            List<String> filterItemFieldNameList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(idList)) {
                filterItemFieldNameList.add("id");
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            Column idColumn = fieldName2ColumnMap.get("id");
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_resource_account", "crra").withOn($sql.exp("crra.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_account", "cra").withOn($sql.exp("cra.id", "=", "crra.account_id")));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_resourcecenter_resource_tag", "crrt").withOn($sql.exp("crrt.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", idColumn.toString())));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_cientity_inspect", "cci").withOn($sql.exp(
                    $sql.exp("cci.ci_entity_id", "=", idColumn.toString()),
                    "and",
                    $sql.exp("cci.job_id", "=", "ajpn.job_id")
            )));
            $sql.addJoin(plainSelect, $sql.join("left join", "cmdb_tag", "ct").withOn($sql.exp("ct.id", "=", "crrt.tag_id")));
            $sql.addSelectColumn(plainSelect, "cci.inspect_status");
            $sql.addSelectColumn(plainSelect, "cci.inspect_time");
            $sql.addSelectColumn(plainSelect, "ajpn.id", "inspectJobPhaseNodeId");
            $sql.addSelectColumn(plainSelect, "ajpn.`job_id`", "jobId");
            $sql.addSelectColumn(plainSelect, "ajpn.status", "jobPhaseNodeStatus");
            $sql.addSelectColumn(plainSelect, "ct.id", "tagId");
            $sql.addSelectColumn(plainSelect, "ct.name", "tagName");
            $sql.addSelectColumn(plainSelect, "ct.description", "tagDescription");
            $sql.addWhereExpression(plainSelect, $sql.exp(idColumn.toString(), "in", idList));
            $sql.addWhereExpression(plainSelect, $sql.exp("ajpn.job_id", "=", jobId));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceCountSql(ResourceSearchVo searchVo, Long jobId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceCountByIpKeywordSql(ResourceSearchVo searchVo, Long jobId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceCountByNameKeywordSql(ResourceSearchVo searchVo, Long jobId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND a.`name` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword));
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, $sql.fun("count", fieldName2ColumnMap.get("id").toString()).withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectAutoexecJobNodeResourceIdListSql(ResourceSearchVo searchVo, Long jobId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setJobId(jobId);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspectConfigFile(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.addGroupBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            if (Objects.equals(queryCriteriaVo.getIsNameFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("name").toString()));
            } else if (Objects.equals(queryCriteriaVo.getIsIpFieldSort(), 1)) {
                $sql.addOrderBy(plainSelect, $sql.fun("length", fieldName2ColumnMap.get("ip").toString()));
            }
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFileResourceIdListSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
                filterItemFieldNameList.add(queryCriteriaVo.getSearchField());
            }
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            /*
            <if test="keyword != null and keyword != ''">
                    AND (a.`name` LIKE CONCAT('%', #{keyword}, '%') OR a.`ip` LIKE CONCAT('%', #{keyword}, '%'))
                </if>
             */
            if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
                String keyword = "'%" + queryCriteriaVo.getKeyword() + "%'";
                $sql.addWhereExpression(plainSelect, $sql.exp("(",
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword),
                        ")")
                );
            }
            SqlVo sqlVo = getSqlVoForInspect(queryCriteriaVo, fieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            $sql.addJoin(plainSelect, $sql.join("left join", "inspect_config_file_last_change_time", "g").withOn($sql.exp("g.`resource_id`", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.addGroupBy(plainSelect, fieldName2ColumnMap.get("id").toString());

            $sql.addOrderBy(plainSelect, $sql.fun("max","g.`last_change_time`"), "desc");
            $sql.addOrderBy(plainSelect, fieldName2ColumnMap.get("id").toString());
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathCountSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            /*
            <if test="keyword != null and keyword != ''">
                AND (a.`path` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`name` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`ip` LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
             */
            if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                String keyword = "'%" + searchVo.getKeyword() + "%'";
                ExpressionVo orExp = $sql.exp(
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword)
                );
                orExp = $sql.exp(orExp, "or", $sql.exp("b.path", "like", keyword));
                $sql.addWhereExpression(plainSelect, $sql.exp("(", orExp, ")")
                );
            }
            /*
            <if test="timeRange != null">
                <if test="timeRange.size() > 0">
                    AND a.`inspect_time` &gt;= STR_TO_DATE(#{timeRange[0]}, '%Y-%m-%d %H:%i:%s')
                </if>
                <if test="timeRange.size() > 1">
                    AND a.`inspect_time` &lt;= STR_TO_DATE(#{timeRange[1]}, '%Y-%m-%d %H:%i:%s')
                </if>
            </if>
             */
            List<String> timeRange = searchVo.getTimeRange();
            if (CollectionUtils.isNotEmpty(timeRange)) {
                $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", ">=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(0)), "'%Y-%m-%d %H:%i:%s'")));
                if (timeRange.size() > 1) {
                    $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", "<=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(1)), "'%Y-%m-%d %H:%i:%s'")));
                }
            }
            $sql.addSelectColumn(plainSelect, $sql.fun("count", "b.id").withDistinct(true));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathIdListSql(ResourceSearchVo searchVo) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            /*
            <if test="keyword != null and keyword != ''">
                AND (a.`path` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`name` LIKE CONCAT('%', #{keyword}, '%')
                OR b.`ip` LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
             */
            if (StringUtils.isNotBlank(searchVo.getKeyword())) {
                String keyword = "'%" + searchVo.getKeyword() + "%'";
                ExpressionVo orExp = $sql.exp(
                        $sql.exp(fieldName2ColumnMap.get("name").toString(), "like", keyword),
                        "or", $sql.exp(fieldName2ColumnMap.get("ip").toString(), "like", keyword)
                );
                orExp = $sql.exp(orExp, "or", $sql.exp("b.path", "like", keyword));
                $sql.addWhereExpression(plainSelect, $sql.exp("(", orExp, ")")
                );
            }
            /*
            <if test="timeRange != null">
                <if test="timeRange.size() > 0">
                    AND a.`inspect_time` &gt;= STR_TO_DATE(#{timeRange[0]}, '%Y-%m-%d %H:%i:%s')
                </if>
                <if test="timeRange.size() > 1">
                    AND a.`inspect_time` &lt;= STR_TO_DATE(#{timeRange[1]}, '%Y-%m-%d %H:%i:%s')
                </if>
            </if>
             */
            List<String> timeRange = searchVo.getTimeRange();
            if (CollectionUtils.isNotEmpty(timeRange)) {
                $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", ">=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(0)), "'%Y-%m-%d %H:%i:%s'")));
                if (timeRange.size() > 1) {
                    $sql.addWhereExpression(plainSelect, $sql.exp("b.inspect_time", "<=", $sql.fun("STR_TO_DATE", $sql.value(timeRange.get(1)), "'%Y-%m-%d %H:%i:%s'")));
                }
            }
            $sql.addGroupBy(plainSelect, "b.id");
            $sql.addOrderBy(plainSelect, $sql.fun("max", "b.inspect_time"), "desc");
            $sql.addSelectColumn(plainSelect, "b.id");
            $sql.setLimit(plainSelect, searchVo.getStartNum(), searchVo.getPageSize());
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathListSql(List<Long> idList) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_label");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addSelectColumn(plainSelect, "b.id", "id");
            $sql.addSelectColumn(plainSelect, "b.resource_id", "resourceId");
            $sql.addSelectColumn(plainSelect, "b.path", "path");
            $sql.addSelectColumn(plainSelect, "b.inspect_time", "inspectTime");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("name").toString(), "resourceName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("ip").toString(), "resourceIP");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("port").toString(), "resourcePort");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_label").toString(), "resourceTypeLabel");
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addWhereExpression(plainSelect, $sql.exp("b.id", "in", idList));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String buildGetInspectConfigFilePathListByJobIdSql(Long jobId) {
        try {
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            List<String> filterItemFieldNameList = new ArrayList<>();
            filterItemFieldNameList.add("id");
            filterItemFieldNameList.add("name");
            filterItemFieldNameList.add("ip");
            filterItemFieldNameList.add("port");
            filterItemFieldNameList.add("type_label");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> fieldName2ColumnMap = new HashMap<>();
            PlainSelect plainSelect = getPlainSelect(config, fieldName2ColumnMap);
            $sql.setDistinct(plainSelect, true);
            $sql.addSelectColumn(plainSelect, "b.id", "id");
            $sql.addSelectColumn(plainSelect, "b.resource_id", "resourceId");
            $sql.addSelectColumn(plainSelect, "b.path", "path");
            $sql.addSelectColumn(plainSelect, "b.md5", "md5");
            $sql.addSelectColumn(plainSelect, "b.inspect_time", "inspectTime");
            $sql.addSelectColumn(plainSelect, "b.file_id", "fileId");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("name").toString(), "resourceName");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("ip").toString(), "resourceIP");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("port").toString(), "resourcePort");
            $sql.addSelectColumn(plainSelect, fieldName2ColumnMap.get("type_label").toString(), "resourceTypeLabel");
            $sql.addSelectColumn(plainSelect, "c.id", "versionId");
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_path", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            $sql.addJoin(plainSelect, $sql.join("join", "inspect_config_file_version", "c").withOn($sql.exp(
                    $sql.exp("c.`path_id`", "=", "b.`id`"),
                    "and",
                    $sql.exp("c.`file_id`", "=", "b.`file_id`")
            )));
            $sql.addWhereExpression(plainSelect, $sql.exp("c.job_id", "=", jobId));
            return plainSelect.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private ResourceEntityConfigVo getResourceEntityConfigVo(ResourceEntityVo resourceEntityVo) {
        String viewName = resourceEntityVo.getName();
        ResourceEntityConfigVo originalConfig = resourceEntityVo.getConfig();
        List<ResourceEntityRelLinkVo> relLinkList = getRelLinkListByRelNode(originalConfig.getRelNode());
        originalConfig.setRelLinkList(relLinkList);
        List<ResourceEntityLeftJoinVo> leftJoinList = getLeftJoinList(originalConfig);
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
        if (CollectionUtils.isEmpty(fieldNameList)) {
            String sceneTemplateName = originalConfig.getSceneTemplateName();
            if (StringUtils.isNotBlank(sceneTemplateName)) {
                fieldNameList = ResourceEntityFactory.getFieldNameListByViewName(sceneTemplateName);
            }
        }
        List<String> selectItemFieldNameList = new ArrayList<>(fieldNameList);
        ResourceEntityConfigVo config = fieldMappingCheckValidityAndFillIdData(viewName, fieldNameList, originalConfig);
        config.setLeftJoinList(leftJoinList);
        config.setSelectItemFieldNameList(selectItemFieldNameList);
        config.setFilterItemFieldNameList(new ArrayList<>());
        return config;
    }
    /**
     * 对字段映射配置信息进行有效性检查及填充缺省数据
     */
    private ResourceEntityConfigVo fieldMappingCheckValidityAndFillIdData(String viewName, List<String> fieldNameList, ResourceEntityConfigVo config) {
        ResourceEntityConfigVo newConfig = new ResourceEntityConfigVo();
        String mainCi = config.getMainCi();
        if (StringUtils.isBlank(mainCi)) {
            throw new ResourceViewFieldMappingException(viewName);
        }
        List<ResourceEntityFieldMappingVo> fieldMappingList = config.getFieldMappingList();
        if (CollectionUtils.isEmpty(fieldMappingList)) {
            throw new ResourceViewFieldMappingException(viewName, fieldNameList);
        }
        Set<String> ciNameSet = new HashSet<>();
        ciNameSet.add(mainCi);
        for (ResourceEntityFieldMappingVo fieldMappingVo : fieldMappingList) {
            String fromCi = fieldMappingVo.getFromCi();
            if (StringUtils.isNotBlank(fromCi)) {
                ciNameSet.add(fromCi);
            }
            String toCi = fieldMappingVo.getToCi();
            if (StringUtils.isNotBlank(toCi)) {
                ciNameSet.add(toCi);
            }
        }
        Map<Long, List<AttrVo>> ciId2AttrListMap = new HashMap<>();
        Map<String, CiVo> name2CiMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(ciNameSet)) {
            List<CiVo> ciList = ciMapper.getCiListByNameList(new ArrayList<>(ciNameSet));
            for (CiVo ciVo : ciList) {
                name2CiMap.put(ciVo.getName(), ciVo);
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                ciId2AttrListMap.put(ciVo.getId(), attrList);
            }
        }
        CiVo mainCiVo = name2CiMap.get(mainCi);
        if (mainCiVo == null) {
            throw new ResourceViewFieldMappingException(viewName, mainCi);
        }
        newConfig.setMainCi(mainCi);
        newConfig.setMainCiVo(mainCiVo);
        Map<String, GlobalAttrVo> name2GlobalAttrMap = new HashMap<>();
        List<ResourceEntityFieldMappingVo> resultList = new ArrayList<>();
        for (ResourceEntityFieldMappingVo fieldMappingVo : fieldMappingList) {
            String field = fieldMappingVo.getField();
            if (!fieldNameList.remove(field)) {
                continue;
            }
            String type = fieldMappingVo.getType();
            ResourceEntityFieldMappingVo newFieldMappingVo = new ResourceEntityFieldMappingVo();
            newFieldMappingVo.setField(field);
            newFieldMappingVo.setType(type);
            if (Objects.equals(type, "const")) {
                String fromCi = fieldMappingVo.getFromCi();
                if (StringUtils.isBlank(fromCi)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                CiVo fromCiVo = name2CiMap.get(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String fromAttr = fieldMappingVo.getFromAttr();
                if (StringUtils.isBlank(fromAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                if (!defaultAttrList.contains(fromAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                newFieldMappingVo.setFromCi(fromCi);
                newFieldMappingVo.setFromCiId(fromCiVo.getId());
                newFieldMappingVo.setFromAttr(fromAttr);
            } else if (Objects.equals(type, "attr")) {
                String fromCi = fieldMappingVo.getFromCi();
                if (StringUtils.isBlank(fromCi)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                CiVo fromCiVo = name2CiMap.get(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String fromAttr = fieldMappingVo.getFromAttr();
                if (StringUtils.isBlank(fromAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                AttrVo fromAttrVo = getAttrVo(fromCiVo, fromAttr, ciId2AttrListMap);
                if (fromAttrVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                newFieldMappingVo.setFromCi(fromCi);
                newFieldMappingVo.setFromCiId(fromCiVo.getId());
                newFieldMappingVo.setFromAttr(fromAttr);
                newFieldMappingVo.setFromAttrId(fromAttrVo.getId());
                newFieldMappingVo.setFromAttrCiId(fromAttrVo.getCiId());
                if (fromAttrVo.getTargetCiId() != null) {
                    String toCi = fieldMappingVo.getToCi();
                    if (StringUtils.isBlank(toCi)) {
                        throw new ResourceViewFieldMappingException(viewName, field, "toCi", toCi);
                    }
                    CiVo toCiVo = name2CiMap.get(toCi);
                    if (toCiVo == null) {
                        throw new ResourceViewFieldMappingException(viewName, field, "toCi", toCi);
                    }
                    if (!Objects.equals(toCiVo.getId(), fromAttrVo.getTargetCiId())) {
                        throw new ResourceViewFieldMappingException(viewName, field, "toCi", toCi);
                    }
                    String toAttr = fieldMappingVo.getToAttr();
                    if (StringUtils.isBlank(toAttr)) {
                        throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                    }
                    newFieldMappingVo.setToCi(toCi);
                    newFieldMappingVo.setToCiId(toCiVo.getId());
                    newFieldMappingVo.setToCiIsVirtual(toCiVo.getIsVirtual());
                    newFieldMappingVo.setToAttr(toAttr);
                    if (Objects.equals(toCiVo.getIsVirtual(), 1)) {
                        newFieldMappingVo.setToAttrCiId(toCiVo.getId());
                        newFieldMappingVo.setToAttrCiName(toCiVo.getName());
                    }
                    if (!defaultAttrList.contains(toAttr)) {
                        AttrVo toAttrVo = getAttrVo(toCiVo, toAttr, ciId2AttrListMap);
                        if (toAttrVo == null) {
                            throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                        }
                        newFieldMappingVo.setToAttrId(toAttrVo.getId());
                        if (Objects.equals(toCiVo.getIsVirtual(), 0)) {
                            newFieldMappingVo.setToAttrCiId(toAttrVo.getCiId());
                            newFieldMappingVo.setToAttrCiName(toAttrVo.getCiName());
                        }
                        if (toAttrVo.getTargetCiId() != null) {
                            throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                        }
                    }
                }
            } else if (Objects.equals(type, "rel")) {
                String fromCi = fieldMappingVo.getFromCi();
                if (StringUtils.isBlank(fromCi)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                CiVo fromCiVo = name2CiMap.get(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String toCi = fieldMappingVo.getToCi();
                if (StringUtils.isBlank(toCi)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "toCi", toCi);
                }
                CiVo toCiVo = name2CiMap.get(toCi);
                if (toCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "toCi", toCi);
                }
                newFieldMappingVo.setFromCi(fromCi);
                newFieldMappingVo.setFromCiId(fromCiVo.getId());
                newFieldMappingVo.setToCi(toCi);
                newFieldMappingVo.setToCiId(toCiVo.getId());
                newFieldMappingVo.setToCiIsVirtual(toCiVo.getIsVirtual());
                String direction = fieldMappingVo.getDirection();
                newFieldMappingVo.setDirection(direction);
                if (Objects.equals(direction, "from")) {
                    String fromAttr = fieldMappingVo.getFromAttr();
                    if (StringUtils.isBlank(fromAttr)) {
                        throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                    }
                    newFieldMappingVo.setFromAttr(fromAttr);
                    if (!defaultAttrList.contains(fromAttr)) {
                        AttrVo fromAttrVo = getAttrVo(fromCiVo, fromAttr, ciId2AttrListMap);
                        if (fromAttrVo == null) {
                            throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                        }
                        if (fromAttrVo.getTargetCiId() != null) {
                            throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                        }
                        newFieldMappingVo.setFromAttrId(fromAttrVo.getId());
                        newFieldMappingVo.setFromAttrCiId(fromAttrVo.getCiId());
                    }
                } else {
                    String toAttr = fieldMappingVo.getToAttr();
                    if (StringUtils.isBlank(toAttr)) {
                        newFieldMappingVo.setToAttr("_id");
                    } else {
                        newFieldMappingVo.setToAttr(toAttr);
                        if (!defaultAttrList.contains(toAttr)) {
                            AttrVo toAttrVo = getAttrVo(toCiVo, toAttr, ciId2AttrListMap);
                            if (toAttrVo == null) {
                                throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                            }
                            if (toAttrVo.getTargetCiId() != null) {
                                throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                            }
                            newFieldMappingVo.setToAttrId(toAttrVo.getId());
                            newFieldMappingVo.setToAttrCiId(toAttrVo.getCiId());
                            newFieldMappingVo.setToAttrCiName(toAttrVo.getCiName());
                        }
                    }
                }
            } else if (Objects.equals(type, "newRel")) {
                String uuid = fieldMappingVo.getUuid();
                String ciName = fieldMappingVo.getCiName();
                String attr = fieldMappingVo.getAttr();
                if (StringUtils.isBlank(attr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "attr", attr);
                }
                List<ResourceEntityRelLinkVo> relLinkList = config.getRelLinkList();
                if (CollectionUtils.isNotEmpty(relLinkList)) {
                    for (ResourceEntityRelLinkVo relLinkVo : relLinkList) {
                        if (Objects.equals(relLinkVo.getRightUuid(), uuid)) {
                            CiVo rightCiVo = name2CiMap.get(ciName);
                            if (rightCiVo == null) {
                                throw new ResourceViewFieldMappingException(viewName, field, "ciName", ciName);
                            }
                            newFieldMappingVo.setType("rel");
                            newFieldMappingVo.setDirection(relLinkVo.getDirection());
                            if (Objects.equals(relLinkVo.getDirection(), RelDirectionType.FROM.getValue())) {
                                CiVo fromCiVo = rightCiVo;
                                newFieldMappingVo.setFromCi(fromCiVo.getName());
                                newFieldMappingVo.setFromCiId(fromCiVo.getId());
                                String fromAttr = attr;
                                newFieldMappingVo.setFromAttr(fromAttr);
                                if (!defaultAttrList.contains(fromAttr)) {
                                    AttrVo fromAttrVo = getAttrVo(fromCiVo, fromAttr, ciId2AttrListMap);
                                    if (fromAttrVo == null) {
                                        throw new ResourceViewFieldMappingException(viewName, field, "attr", attr);
                                    }
                                    if (fromAttrVo.getTargetCiId() != null) {
                                        throw new ResourceViewFieldMappingException(viewName, field, "attr", attr);
                                    }
                                    newFieldMappingVo.setFromAttrId(fromAttrVo.getId());
                                    newFieldMappingVo.setFromAttrCiId(fromAttrVo.getCiId());
                                }
                                newFieldMappingVo.setFromCiAlias(relLinkVo.getRightCiAlias());

                                String toCi = relLinkVo.getLeftCi();
                                CiVo toCiVo = name2CiMap.get(toCi);
                                newFieldMappingVo.setToCi(toCiVo.getName());
                                newFieldMappingVo.setToCiId(toCiVo.getId());
                                newFieldMappingVo.setToCiIsVirtual(toCiVo.getIsVirtual());
                                newFieldMappingVo.setToCiAlias(relLinkVo.getLeftCiAlias());
                            } else if (Objects.equals(relLinkVo.getDirection(), RelDirectionType.TO.getValue())) {
                                CiVo toCiVo = rightCiVo;
                                newFieldMappingVo.setToCi(toCiVo.getName());
                                newFieldMappingVo.setToCiId(toCiVo.getId());
                                newFieldMappingVo.setToCiIsVirtual(toCiVo.getIsVirtual());
                                newFieldMappingVo.setToCiAlias(relLinkVo.getRightCiAlias());
                                String toAttr = attr;
                                if (StringUtils.isBlank(toAttr)) {
                                    newFieldMappingVo.setToAttr("_id");
                                } else {
                                    newFieldMappingVo.setToAttr(toAttr);
                                    if (!defaultAttrList.contains(toAttr)) {
                                        AttrVo toAttrVo = getAttrVo(toCiVo, toAttr, ciId2AttrListMap);
                                        if (toAttrVo == null) {
                                            throw new ResourceViewFieldMappingException(viewName, field, "attr", attr);
                                        }
                                        if (toAttrVo.getTargetCiId() != null) {
                                            throw new ResourceViewFieldMappingException(viewName, field, "attr", attr);
                                        }
                                        newFieldMappingVo.setToAttrId(toAttrVo.getId());
                                        newFieldMappingVo.setToAttrCiId(toAttrVo.getCiId());
                                        newFieldMappingVo.setToAttrCiName(toAttrVo.getCiName());
                                    }
                                }
                                String fromCi = relLinkVo.getLeftCi();
                                CiVo fromCiVo = name2CiMap.get(fromCi);
                                newFieldMappingVo.setFromCi(fromCiVo.getName());
                                newFieldMappingVo.setFromCiId(fromCiVo.getId());
                                newFieldMappingVo.setFromCiAlias(relLinkVo.getLeftCiAlias());
                            }
                            break;
                        }
                    }
                }
            } else if (Objects.equals(type, "globalAttr")) {
                String fromCi = fieldMappingVo.getFromCi();
                if (StringUtils.isBlank(fromCi)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                CiVo fromCiVo = name2CiMap.get(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String fromAttr = fieldMappingVo.getFromAttr();
                if (StringUtils.isBlank(fromAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                GlobalAttrVo globalAttrVo = name2GlobalAttrMap.get(fromAttr);
                if (globalAttrVo == null) {
                    globalAttrVo = globalAttrMapper.getGlobalAttrByName(fromAttr);
                    if (globalAttrVo == null) {
                        throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                    }
                    name2GlobalAttrMap.put(fromAttr, globalAttrVo);
                }
                String toAttr = fieldMappingVo.getToAttr();
                if (StringUtils.isBlank(toAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                }
                if (!Objects.equals(toAttr, "id") && !Objects.equals(toAttr, "value") && !Objects.equals(toAttr, "sort")) {
                    throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                }
                newFieldMappingVo.setFromCi(fromCi);
                newFieldMappingVo.setFromCiId(fromCiVo.getId());
                newFieldMappingVo.setFromAttr(fromAttr);
                newFieldMappingVo.setToAttr(toAttr);
            } else if (Objects.equals(type, "empty")) {
                // ignore
            } else {
                throw new ResourceViewFieldMappingException(viewName, field, "type", type);
            }
            resultList.add(newFieldMappingVo);
        }
        newConfig.setFieldMappingList(resultList);
        return newConfig;
    }

    private List<ResourceEntityRelLinkVo> getRelLinkListByRelNode(ResourceEntityRelNodeVo relNode) {
        List<ResourceEntityRelLinkVo> relLinkList = new ArrayList<>();
        if (relNode != null) {
            Map<String, Map<ResourceEntityRelNodeVo, String>> map = new HashMap<>();
            List<ResourceEntityRelNodeVo> children = relNode.getChildren();
            if (CollectionUtils.isNotEmpty(children)) {
                for (ResourceEntityRelNodeVo child : children) {
                    addRelLinkListByRelNode(relLinkList, relNode, child, map);
                }
            }
        }
        return relLinkList;
    }

    private void addRelLinkListByRelNode(List<ResourceEntityRelLinkVo> relLinkList, ResourceEntityRelNodeVo leftNode, ResourceEntityRelNodeVo rightNode, Map<String, Map<ResourceEntityRelNodeVo, String>> map) {
        {
            ResourceEntityRelLinkVo relLinkVo = new ResourceEntityRelLinkVo();
            {
                Map<ResourceEntityRelNodeVo, String> relNodeAliasMap = map.computeIfAbsent(leftNode.getCiName(), key -> new HashMap<>());
                int size = relNodeAliasMap.size();
                String alias = relNodeAliasMap.get(leftNode);
                if (alias == null) {
                    if (size == 0) {
                        alias = StringUtils.EMPTY;
                    } else {
                        alias = "alias_" + (size + 1);
                    }
                    relNodeAliasMap.put(leftNode, alias);
                }
                relLinkVo.setLeftCi(leftNode.getCiName());
                relLinkVo.setLeftCiAlias(alias);
            }
            {
                Map<ResourceEntityRelNodeVo, String> relNodeAliasMap = map.computeIfAbsent(rightNode.getCiName(), key -> new HashMap<>());
                int size = relNodeAliasMap.size();
                String alias = relNodeAliasMap.get(rightNode);
                if (alias == null) {
                    if (size == 0) {
                        alias = StringUtils.EMPTY;
                    } else {
                        alias = "_alias_" + (size + 1);
                    }
                    relNodeAliasMap.put(rightNode, alias);
                }
                relLinkVo.setRightCi(rightNode.getCiName());
                relLinkVo.setRightCiAlias(alias);
                relLinkVo.setRightUuid(rightNode.getUuid());
            }
            relLinkVo.setDirection(rightNode.getDirection());
            relLinkList.add(relLinkVo);
        }
        List<ResourceEntityRelNodeVo> children = rightNode.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            for (ResourceEntityRelNodeVo child : children) {
                addRelLinkListByRelNode(relLinkList, rightNode, child, map);
            }
        }
    }

    private List<ResourceEntityLeftJoinVo> getLeftJoinList(ResourceEntityConfigVo config) {
        List<ResourceEntityLeftJoinVo> resultList = new ArrayList<>();
        List<ResourceEntityRelLinkVo> relLinkList = config.getRelLinkList();
        if (CollectionUtils.isNotEmpty(relLinkList)) {
            Set<String> ciNameSet = new HashSet<>();
            for (ResourceEntityRelLinkVo linkVo : relLinkList) {
                String leftCi = linkVo.getLeftCi();
                String rightCi = linkVo.getRightCi();
                if (StringUtils.isNotBlank(leftCi)) {
                    ciNameSet.add(leftCi);
                }
                if (StringUtils.isNotBlank(rightCi)) {
                    ciNameSet.add(rightCi);
                }
            }
            Map<String, CiVo> name2CiMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(ciNameSet)) {
                List<CiVo> ciList = ciMapper.getCiListByNameList(new ArrayList<>(ciNameSet));
                for (CiVo ciVo : ciList) {
                    name2CiMap.put(ciVo.getName(), ciVo);
                }
            }
            for (ResourceEntityRelLinkVo linkVo : relLinkList) {
                String leftCi = linkVo.getLeftCi();
                String rightCi = linkVo.getRightCi();
                CiVo leftCiVo = name2CiMap.get(leftCi);
                if (leftCiVo == null) {
                    throw new CiNotFoundException(leftCi);
                }
                CiVo rightCiVo = name2CiMap.get(rightCi);
                if (rightCiVo == null) {
                    throw new CiNotFoundException(rightCi);
                }
                String direction = linkVo.getDirection();
                if (Objects.equals(direction, RelDirectionType.FROM.getValue())) {
                    ResourceEntityLeftJoinVo leftJoinVo = new ResourceEntityLeftJoinVo();
                    leftJoinVo.setDirection(direction);
                    leftJoinVo.setFromCi(rightCiVo.getName());
                    leftJoinVo.setFromCiId(rightCiVo.getId());
                    leftJoinVo.setFromCiAlias(linkVo.getRightCiAlias());
                    leftJoinVo.setToCi(leftCiVo.getName());
                    leftJoinVo.setToCiId(leftCiVo.getId());
                    leftJoinVo.setToCiAlias(linkVo.getLeftCiAlias());
                    resultList.add(leftJoinVo);
                } else if (Objects.equals(direction, RelDirectionType.TO.getValue())) {
                    ResourceEntityLeftJoinVo leftJoinVo = new ResourceEntityLeftJoinVo();
                    leftJoinVo.setDirection(direction);
                    leftJoinVo.setFromCi(leftCiVo.getName());
                    leftJoinVo.setFromCiId(leftCiVo.getId());
                    leftJoinVo.setFromCiAlias(linkVo.getLeftCiAlias());
                    leftJoinVo.setToCi(rightCiVo.getName());
                    leftJoinVo.setToCiId(rightCiVo.getId());
                    leftJoinVo.setToCiAlias(linkVo.getRightCiAlias());
                    resultList.add(leftJoinVo);
                }
            }
        }
        return resultList;
    }

    private AttrVo getAttrVo(CiVo ciVo, String attrName, Map<Long, List<AttrVo>> ciId2AttrListMap) {
        List<AttrVo> attrList = ciId2AttrListMap.get(ciVo.getId());
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (AttrVo attr : attrList) {
                if (Objects.equals(attr.getName(), attrName)) {
                    return attr;
                }
            }
        }
        return null;
    }

    /**
     * 根据queryCriteriaVo查询条件收集组装动态sql时，需要返回的条件列
     */
    private List<String> getFilterItemFieldNameList(ResourceQueryCriteriaVo queryCriteriaVo) {
        Set<String> filterItemFieldNameSet = new HashSet<>();
        if (StringUtils.isNotBlank(queryCriteriaVo.getKeyword())) {
            filterItemFieldNameSet.add("name");
            filterItemFieldNameSet.add("ip");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getKeywordList())) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
            filterItemFieldNameSet.add("id");
        }
        if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTypeIdList())) {
            filterItemFieldNameSet.add("type_id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getStateIdList())) {
            filterItemFieldNameSet.add("state_id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getVendorIdList())) {
            filterItemFieldNameSet.add("vendor_id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getEnvIdList())) {
            filterItemFieldNameSet.add("env_id");
        }
        if (Objects.equals(queryCriteriaVo.getExistNoEnv(), true)) {
            filterItemFieldNameSet.add("env_id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppSystemIdList())) {
            filterItemFieldNameSet.add("app_system_id");
            filterItemFieldNameSet.add("app_module_id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppModuleIdList())) {
            filterItemFieldNameSet.add("app_module_id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getDefaultValue())) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getIdList())) {
            filterItemFieldNameSet.add("id");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectStatusList())) {
            filterItemFieldNameSet.add("inspect_status");
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInputNodeList())) {
            filterItemFieldNameSet.add("name");
            filterItemFieldNameSet.add("ip");
            filterItemFieldNameSet.add("port");
        }
        if (queryCriteriaVo.getConditionConfig() != null) {
            filterItemFieldNameSet.addAll(queryCriteriaVo.getConditionConfig().getFilterItemFieldNameList());
        }
        return new ArrayList<>(filterItemFieldNameSet);
    }

    private SqlVo getSqlVoForResource(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> fieldName2ColumnMap) {
        SqlVo sqlVo = new SqlVo();
        getSqlVoForResource(sqlVo, queryCriteriaVo, fieldName2ColumnMap);
        return sqlVo;
    }

    private void getSqlVoForResource(SqlVo sqlVo, ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> fieldName2ColumnMap) {
//        SqlVo sqlVo = new SqlVo();
        List<JoinVo> joinList = new ArrayList<>();
        List<ExpressionVo> whereExpressionList = new ArrayList<>();
        /*
        <if test="keywordList != null and keywordList.size() > 0">
            JOIN fulltextindex_field_cmdb ffc ON ffc.target_id = a.id AND ffc.target_field IN (#{nameFieldAttrId}, #{ipFieldAttrId})
            JOIN fulltextindex_word fw ON ffc.word_id = fw.id
            AND (fw.word IN
            <foreach collection="keywordList" item="item" open="(" close=")" separator=",">
                #{item}
            </foreach>
            )
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getKeywordList()) && (queryCriteriaVo.getNameFieldAttrId() != null || queryCriteriaVo.getIpFieldAttrId() != null)) {
//            System.out.println("a");
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("ffc.target_id", "=", fieldName2ColumnMap.get("id").toString()),
                        "and",
                        $sql.exp("ffc.target_field", "in", Arrays.asList(queryCriteriaVo.getNameFieldAttrId(), queryCriteriaVo.getIpFieldAttrId())));
                joinList.add($sql.join("join", "fulltextindex_field_cmdb", "ffc").withOn(expressionVo));
            }
            {
//                ExpressionVo expressionVo = $sql.exp(
//                        $sql.exp("fw.id", "=", "ffc.word_id"),
//                        "and",
//                        $sql.exp("fw.word", "in", queryCriteriaVo.getKeywordList()));
                joinList.add($sql.join("join", "fulltextindex_word", "fw").withOn($sql.exp("fw.id", "=", "ffc.word_id")));
                whereExpressionList.add($sql.exp("fw.word", "in", queryCriteriaVo.getKeywordList()));
            }
        }
        /*
        <if test="batchSearchList != null and batchSearchList.size() > 0 and searchField != null and searchField != ''">
            JOIN fulltextindex_field_cmdb ffc2 ON ffc2.target_id = a.id
            <choose>
                <when test="searchField == 'name'">
                    AND ffc2.target_field = #{nameFieldAttrId}
                </when>
                <otherwise>
                    AND ffc2.target_field = #{ipFieldAttrId}
                </otherwise>
            </choose>
            JOIN fulltextindex_word fw2 ON ffc2.word_id = fw2.id
            AND (fw2.word IN
            <foreach collection="batchSearchList" item="item" open="(" close=")" separator=",">
                #{item}
            </foreach>
            )
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
            {
                Long fieldAttrId = null;
                if (Objects.equals(queryCriteriaVo.getSearchField(), "name")) {
//                    System.out.println("b");
                    fieldAttrId = queryCriteriaVo.getNameFieldAttrId();
                } else {
//                    System.out.println("c");
                    fieldAttrId = queryCriteriaVo.getIpFieldAttrId();
                }
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("ffc2.target_id", "=", fieldName2ColumnMap.get("id").toString()),
                        "and",
                        $sql.exp("ffc2.target_field", "=", fieldAttrId));
                joinList.add($sql.join("join", "fulltextindex_field_cmdb", "ffc2").withOn(expressionVo));
            }
            {
//                ExpressionVo expressionVo = $sql.exp(
//                        $sql.exp("fw2.id", "=", "ffc2.word_id"),
//                        "and",
//                        $sql.exp("fw2.word", "in", queryCriteriaVo.getBatchSearchList()));
                joinList.add($sql.join("join", "fulltextindex_word", "fw2").withOn($sql.exp("fw2.id", "=", "ffc2.word_id")));
                whereExpressionList.add($sql.exp("fw2.word", "in", queryCriteriaVo.getBatchSearchList()));
            }
        }
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_account` b ON b.`resource_id` = a.`id`
            LEFT JOIN `cmdb_resourcecenter_account` c ON c.`id` = b.`account_id`
        </if>

        <if test="protocolIdList != null and protocolIdList.size() > 0">
            AND c.`protocol_id` IN
            <foreach collection="protocolIdList" item="protocolId" open="(" separator="," close=")">
                #{protocolId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
//            System.out.println("d");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            whereExpressionList.add($sql.exp("c.protocol_id", "in", queryCriteriaVo.getProtocolIdList()));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_tag` d ON d.`resource_id` = a.`id`
        </if>

        <if test="tagIdList != null and tagIdList.size() > 0">
            AND d.`tag_id` IN
            <foreach collection="tagIdList" item="tagId" open="(" separator="," close=")">
                #{tagId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
//            System.out.println("e");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_tag", "d").withOn($sql.exp("d.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("d.tag_id", "in", queryCriteriaVo.getTagIdList()));
        }
        /*
        <if test="inspectJobPhaseNodeStatusList !=null and inspectJobPhaseNodeStatusList.size() > 0">
            left join autoexec_job_resource_inspect ajri on ajri.resource_id=a.id
            left join autoexec_job_phase_node ajpn on ajpn.job_phase_id =ajri.phase_id AND ajpn.resource_id = a.id
        </if>

        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
//            System.out.println("f");
            joinList.add($sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            ExpressionVo expressionVo = $sql.exp($sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"), "and", $sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString()));
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn(expressionVo));
            whereExpressionList.add($sql.exp("ajpn.status", "in", queryCriteriaVo.getInspectJobPhaseNodeStatusList()));
        }
        /*
        <if test="isHasAuth == false">
            LEFT JOIN cmdb_cientity_group ccg ON ccg.cientity_id = a.id
            LEFT JOIN cmdb_group_auth cga ON ccg.group_id = cga.group_id
             <choose>
                <when test="cmdbGroupType == 'autoexec'">
                    LEFT JOIN cmdb_group cg ON cga.group_id = cg.id AND cg.type in ('autoexec')
                </when>
                <otherwise>
                    LEFT JOIN cmdb_group cg ON cga.group_id = cg.id AND cg.type in ('readonly','maintain','autoexec')
                </otherwise>
            </choose>
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
            joinList.add($sql.join("left join", "cmdb_cientity_group", "ccg").withOn($sql.exp("ccg.cientity_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_group_auth", "cga").withOn($sql.exp("cga.group_id", "=", "ccg.group_id")));

            List<String> strList = new ArrayList<>();
            if (Objects.equals(queryCriteriaVo.getCmdbGroupType(), "autoexec")) {
//                System.out.println("g");
                strList.add("autoexec");
            } else {
//                System.out.println("h");
                strList.add("autoexec");
                strList.add("readonly");
                strList.add("maintain");
            }
            ExpressionVo expressionVo = $sql.exp(
                    $sql.exp("cg.id", "=", "cga.group_id"),
                    "and",
                    $sql.exp("cg.type", "in", strList)
            );
            joinList.add($sql.join("left join", "cmdb_group", "cg").withOn(expressionVo));
        }
        /*
         <if test="typeIdList != null and typeIdList.size() > 0">
            <if test="isHasAuth == true">
                AND a.`type_id` IN
                <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                    #{typeId}
                </foreach>
            </if>
            <if test="isHasAuth == false">
                AND (
                <choose>
                    <when test="authedTypeIdList != null and authedTypeIdList.size() >0">
                        a.`type_id` IN
                        <foreach collection="authedTypeIdList" item="authedTypeId" open="(" separator="," close=")">
                            #{authedTypeId}
                        </foreach>
                    </when>
                    <otherwise>
                        1 = 0
                    </otherwise>
                </choose>
                or (
                cg.id is not null and
                a.`type_id` IN
                <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                    #{typeId}
                </foreach>
                and
                ((cga.auth_type = 'common' AND cga.auth_uuid = 'alluser')
                <if test="authenticationInfo != null">
                    OR cga.auth_uuid IN (
                    #{authenticationInfo.userUuid}
                    <if test="authenticationInfo.teamUuidList != null and authenticationInfo.teamUuidList.size() > 0">
                        <foreach collection="authenticationInfo.teamUuidList" item="item" open="," separator=",">
                            #{item}
                        </foreach>
                    </if>
                    <if test="authenticationInfo.roleUuidList != null and authenticationInfo.roleUuidList.size() > 0">
                        <foreach collection="authenticationInfo.roleUuidList" item="item" open="," separator=",">
                            #{item}
                        </foreach>
                    </if>
                )
                </if>
                )
                )
                )
            </if>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTypeIdList())) {
            if (Objects.equals(queryCriteriaVo.getIsHasAuth(), true)) {
//                System.out.println("i");
                whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
            } else if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
                ExpressionVo orLeftExpressionVo = null;
                if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthedTypeIdList())) {
//                    System.out.println("j");
                    orLeftExpressionVo = $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getAuthedTypeIdList());
                } else {
//                    System.out.println("k");
                    orLeftExpressionVo = $sql.exp(1, "=", 0);
                }
                ExpressionVo orRightExpressionVo = $sql.exp($sql.exp("cg.id", "is not null"), "and", $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));

                ExpressionVo orLeftExpressionVo2 = $sql.exp("(", $sql.exp("cga.auth_type", "=", "'common'"), "and", $sql.exp("cga.auth_uuid", "=", "'alluser'"), ")");
                ExpressionVo orRightExpressionVo2 = null;
                if (queryCriteriaVo.getAuthenticationInfo() != null) {
//                    System.out.println("l");
                    List<String> uuidList = new ArrayList<>();
                    if (StringUtils.isNotBlank(queryCriteriaVo.getAuthenticationInfo().getUserUuid())) {
                        uuidList.add(queryCriteriaVo.getAuthenticationInfo().getUserUuid());
                    }
                    if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthenticationInfo().getTeamUuidList())) {
                        uuidList.addAll(queryCriteriaVo.getAuthenticationInfo().getTeamUuidList());
                    }
                    if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthenticationInfo().getRoleUuidList())) {
                        uuidList.addAll(queryCriteriaVo.getAuthenticationInfo().getRoleUuidList());
                    }
                    if (CollectionUtils.isNotEmpty(uuidList)) {
//                        System.out.println("m");
                        orRightExpressionVo2 = $sql.exp("cga.auth_uuid", "in", uuidList);
                    }
                }
                if (orRightExpressionVo2 != null) {
//                    System.out.println("n");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", $sql.exp("(", orLeftExpressionVo2, "or", orRightExpressionVo2, ")"));
                } else {
//                    System.out.println("o");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", orLeftExpressionVo2);
                }
                ExpressionVo orExpressionVo = $sql.exp("(", orLeftExpressionVo, "or", orRightExpressionVo, ")");
                whereExpressionList.add(orExpressionVo);
            }
        }
        /*
        <if test="stateIdList != null and stateIdList.size() > 0">
            AND a.`state_id` IN
            <foreach collection="stateIdList" item="stateId" open="(" separator="," close=")">
                #{stateId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getStateIdList())) {
//            System.out.println("p");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
        }
        /*
        <if test="vendorIdList != null and vendorIdList.size() > 0">
            AND a.`vendor_id` IN
            <foreach collection="vendorIdList" item="vendorId" open="(" separator="," close=")">
                #{vendorId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getVendorIdList())) {
//            System.out.println("q");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
        }
        /*
        <if test="envIdList != null and envIdList.size() > 0">
            AND a.`env_id` IN
            <foreach collection="envIdList" item="envId" open="(" separator="," close=")">
                #{envId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getEnvIdList())) {
//            System.out.println("r");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="isExistNoEnv">
            AND a.`env_id` is null
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getExistNoEnv(), true)) {
//            System.out.println("s");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("env_id").toString(), "is null"));
        }
        /*
        <if test="appSystemIdList != null and appSystemIdList.size() > 0">
            AND a.`app_system_id` IN
            <foreach collection="appSystemIdList" item="appSystemId" open="(" separator="," close=")">
                #{appSystemId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppSystemIdList())) {
//            System.out.println("t");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
        }
        /*
        <if test="appModuleIdList != null and appModuleIdList.size() > 0">
            AND a.`app_module_id` IN
            <foreach collection="appModuleIdList" item="appModuleId" open="(" separator="," close=")">
                #{appModuleId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppModuleIdList())) {
//            System.out.println("u");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
        }
        /*
        <if test="defaultValue != null and defaultValue.size() > 0">
            AND a.`id` IN
            <foreach collection="defaultValue" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getDefaultValue())) {
//            System.out.println("v");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("id").toString(), "in", queryCriteriaVo.getDefaultValue()));
        }
        /*
        <if test="idList != null and idList.size() > 0">
            AND a.`id` IN
            <foreach collection="idList" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getIdList())) {
//            System.out.println("w");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("id").toString(), "in", queryCriteriaVo.getIdList()));
        }
        /*
        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectStatusList())) {
//            System.out.println("x");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", queryCriteriaVo.getInspectStatusList()));
        }
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInputNodeList())) {
            ExpressionVo orExp = null;
            for (ResourceVo inputNode : queryCriteriaVo.getInputNodeList()) {
                Column ipColumn = fieldName2ColumnMap.get("ip");
                ExpressionVo andExp = $sql.exp(ipColumn.toString(), "=", $sql.value(inputNode.getIp()));
                Column portColumn = fieldName2ColumnMap.get("port");
                if (inputNode.getPort() != null) {
                    andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "=", inputNode.getPort()));
                } else {
                    andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "is null"));
                }
                if (StringUtils.isNotBlank(inputNode.getName())) {
                    Column nameColumn = fieldName2ColumnMap.get("name");
                    andExp = $sql.exp(andExp, "and", $sql.exp(nameColumn.toString(), "=", $sql.value(inputNode.getName())));
                }
                andExp = $sql.exp("(", andExp, ")");
                if (orExp == null) {
                    orExp = andExp;
                } else {
                    orExp = $sql.exp(orExp, "or", andExp);
                }
            }
            orExp = $sql.exp("(", orExp, ")");
            whereExpressionList.add(orExp);
        }
        sqlVo.withAddJoinList(joinList);
        sqlVo.withAddWhereExpressionList(whereExpressionList);
        if (queryCriteriaVo.getConditionConfig() != null) {
            queryCriteriaVo.getConditionConfig().buildConditionSqlVo(sqlVo, fieldName2ColumnMap);
        }
//        return sqlVo;
    }

    private SqlVo getSqlVoForInspect(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> fieldName2ColumnMap) {
        SqlVo sqlVo = new SqlVo();
        List<JoinVo> joinList = new ArrayList<>();
        List<ExpressionVo> whereExpressionList = new ArrayList<>();
        /*
        <if test="batchSearchList != null and batchSearchList.size() > 0 and searchField != null and searchField != ''">
            AND
            <if test="searchField == 'name'">
                <foreach collection="batchSearchList" item="item" open="(" separator=" OR " close=")">
                    a.`name` LIKE #{item}
                </foreach>
            </if>
            <if test="searchField == 'ip'">
                <foreach collection="batchSearchList" item="item" open="(" separator=" OR " close=")">
                    a.`ip` LIKE #{item}
                </foreach>
            </if>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getBatchSearchList()) && StringUtils.isNotBlank(queryCriteriaVo.getSearchField())) {
            String columnName = null;
            if (Objects.equals(queryCriteriaVo.getSearchField(), "name")) {
//                System.out.println("b");
                columnName = fieldName2ColumnMap.get("name").toString();
            } else {
//                System.out.println("c");
                columnName = fieldName2ColumnMap.get("ip").toString();
            }
            ExpressionVo orExp = null;
            for (String item : queryCriteriaVo.getBatchSearchList()) {
                if (orExp != null) {
                    orExp = $sql.exp(orExp, "or", $sql.exp(columnName, "like", $sql.value(item)));
                } else {
                    orExp = $sql.exp(columnName, "like", $sql.value(item));
                }
            }
            whereExpressionList.add($sql.exp("(", orExp, ")"));
        }
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_account` b ON b.`resource_id` = a.`id`
            LEFT JOIN `cmdb_resourcecenter_account` c ON c.`id` = b.`account_id`
        </if>

        <if test="protocolIdList != null and protocolIdList.size() > 0">
            AND c.`protocol_id` IN
            <foreach collection="protocolIdList" item="protocolId" open="(" separator="," close=")">
                #{protocolId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
//            System.out.println("d");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            whereExpressionList.add($sql.exp("c.protocol_id", "in", queryCriteriaVo.getProtocolIdList()));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_tag` d ON d.`resource_id` = a.`id`
        </if>

        <if test="tagIdList != null and tagIdList.size() > 0">
            AND d.`tag_id` IN
            <foreach collection="tagIdList" item="tagId" open="(" separator="," close=")">
                #{tagId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
//            System.out.println("e");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_tag", "d").withOn($sql.exp("d.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("d.tag_id", "in", queryCriteriaVo.getTagIdList()));
        }
        /*
        <if test="inspectJobPhaseNodeStatusList !=null and inspectJobPhaseNodeStatusList.size() > 0">
            left join autoexec_job_resource_inspect ajri on ajri.resource_id=a.id
            left join autoexec_job_phase_node ajpn on ajpn.job_phase_id =ajri.phase_id AND ajpn.resource_id = a.id
        </if>

        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
//            System.out.println("f");
            joinList.add($sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            ExpressionVo expressionVo = $sql.exp($sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"), "and", $sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString()));
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn(expressionVo));
            whereExpressionList.add($sql.exp("ajpn.status", "in", queryCriteriaVo.getInspectJobPhaseNodeStatusList()));
        }
        /*
        <if test="isHasAuth == false">
            LEFT JOIN cmdb_cientity_group ccg ON ccg.cientity_id = a.id
            LEFT JOIN cmdb_group_auth cga ON ccg.group_id = cga.group_id
             <choose>
                <when test="cmdbGroupType == 'autoexec'">
                    LEFT JOIN cmdb_group cg ON cga.group_id = cg.id AND cg.type in ('autoexec')
                </when>
                <otherwise>
                    LEFT JOIN cmdb_group cg ON cga.group_id = cg.id AND cg.type in ('readonly','maintain','autoexec')
                </otherwise>
            </choose>
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
            joinList.add($sql.join("left join", "cmdb_cientity_group", "ccg").withOn($sql.exp("ccg.cientity_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_group_auth", "cga").withOn($sql.exp("cga.group_id", "=", "ccg.group_id")));

            List<String> strList = new ArrayList<>();
            if (Objects.equals(queryCriteriaVo.getCmdbGroupType(), "autoexec")) {
//                System.out.println("g");
                strList.add("autoexec");
            } else {
//                System.out.println("h");
                strList.add("autoexec");
                strList.add("readonly");
                strList.add("maintain");
            }
            ExpressionVo expressionVo = $sql.exp(
                    $sql.exp("cg.id", "=", "cga.group_id"),
                    "and",
                    $sql.exp("cg.type", "in", strList)
            );
            joinList.add($sql.join("left join", "cmdb_group", "cg").withOn(expressionVo));
        }
        /*
         <if test="typeIdList != null and typeIdList.size() > 0">
            <if test="isHasAuth == true">
                AND a.`type_id` IN
                <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                    #{typeId}
                </foreach>
            </if>
            <if test="isHasAuth == false">
                AND (
                <choose>
                    <when test="authedTypeIdList != null and authedTypeIdList.size() >0">
                        a.`type_id` IN
                        <foreach collection="authedTypeIdList" item="authedTypeId" open="(" separator="," close=")">
                            #{authedTypeId}
                        </foreach>
                    </when>
                    <otherwise>
                        1 = 0
                    </otherwise>
                </choose>
                or (
                cg.id is not null and
                a.`type_id` IN
                <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                    #{typeId}
                </foreach>
                and
                ((cga.auth_type = 'common' AND cga.auth_uuid = 'alluser')
                <if test="authenticationInfo != null">
                    OR cga.auth_uuid IN (
                    #{authenticationInfo.userUuid}
                    <if test="authenticationInfo.teamUuidList != null and authenticationInfo.teamUuidList.size() > 0">
                        <foreach collection="authenticationInfo.teamUuidList" item="item" open="," separator=",">
                            #{item}
                        </foreach>
                    </if>
                    <if test="authenticationInfo.roleUuidList != null and authenticationInfo.roleUuidList.size() > 0">
                        <foreach collection="authenticationInfo.roleUuidList" item="item" open="," separator=",">
                            #{item}
                        </foreach>
                    </if>
                )
                </if>
                )
                )
                )
            </if>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTypeIdList())) {
            if (Objects.equals(queryCriteriaVo.getIsHasAuth(), true)) {
//                System.out.println("i");
                whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
            } else if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
                ExpressionVo orLeftExpressionVo = null;
                if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthedTypeIdList())) {
//                    System.out.println("j");
                    orLeftExpressionVo = $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getAuthedTypeIdList());
                } else {
//                    System.out.println("k");
                    orLeftExpressionVo = $sql.exp(1, "=", 0);
                }
                ExpressionVo orRightExpressionVo = $sql.exp($sql.exp("cg.id", "is not null"), "and", $sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));

                ExpressionVo orLeftExpressionVo2 = $sql.exp("(", $sql.exp("cga.auth_type", "=", "'common'"), "and", $sql.exp("cga.auth_uuid", "=", "'alluser'"), ")");
                ExpressionVo orRightExpressionVo2 = null;
                if (queryCriteriaVo.getAuthenticationInfo() != null) {
//                    System.out.println("l");
                    List<String> uuidList = new ArrayList<>();
                    if (StringUtils.isNotBlank(queryCriteriaVo.getAuthenticationInfo().getUserUuid())) {
                        uuidList.add(queryCriteriaVo.getAuthenticationInfo().getUserUuid());
                    }
                    if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthenticationInfo().getTeamUuidList())) {
                        uuidList.addAll(queryCriteriaVo.getAuthenticationInfo().getTeamUuidList());
                    }
                    if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthenticationInfo().getRoleUuidList())) {
                        uuidList.addAll(queryCriteriaVo.getAuthenticationInfo().getRoleUuidList());
                    }
                    if (CollectionUtils.isNotEmpty(uuidList)) {
//                        System.out.println("m");
                        orRightExpressionVo2 = $sql.exp("cga.auth_uuid", "in", uuidList);
                    }
                }
                if (orRightExpressionVo2 != null) {
//                    System.out.println("n");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", $sql.exp("(", orLeftExpressionVo2, "or", orRightExpressionVo2, ")"));
                } else {
//                    System.out.println("o");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", orLeftExpressionVo2);
                }
                ExpressionVo orExpressionVo = $sql.exp("(", orLeftExpressionVo, "or", orRightExpressionVo, ")");
                whereExpressionList.add(orExpressionVo);
            }
        }
        /*
        <if test="stateIdList != null and stateIdList.size() > 0">
            AND a.`state_id` IN
            <foreach collection="stateIdList" item="stateId" open="(" separator="," close=")">
                #{stateId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getStateIdList())) {
//            System.out.println("p");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
        }
        /*
        <if test="vendorIdList != null and vendorIdList.size() > 0">
            AND a.`vendor_id` IN
            <foreach collection="vendorIdList" item="vendorId" open="(" separator="," close=")">
                #{vendorId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getVendorIdList())) {
//            System.out.println("q");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
        }
        /*
        <if test="envIdList != null and envIdList.size() > 0">
            AND a.`env_id` IN
            <foreach collection="envIdList" item="envId" open="(" separator="," close=")">
                #{envId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getEnvIdList())) {
//            System.out.println("r");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="appSystemIdList != null and appSystemIdList.size() > 0">
            AND a.`app_system_id` IN
            <foreach collection="appSystemIdList" item="appSystemId" open="(" separator="," close=")">
                #{appSystemId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppSystemIdList())) {
//            System.out.println("t");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
        }
        /*
        <if test="appModuleIdList != null and appModuleIdList.size() > 0">
            AND a.`app_module_id` IN
            <foreach collection="appModuleIdList" item="appModuleId" open="(" separator="," close=")">
                #{appModuleId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppModuleIdList())) {
//            System.out.println("u");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
        }
        /*
        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectStatusList())) {
//            System.out.println("x");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("inspect_status").toString(), "in", queryCriteriaVo.getInspectStatusList()));
        }
        sqlVo.withJoinList(joinList);
        sqlVo.withWhereExpressionList(whereExpressionList);
        return sqlVo;
    }

    private SqlVo getSqlVoForInspectConfigFile(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> fieldName2ColumnMap) {
        SqlVo sqlVo = new SqlVo();
        List<JoinVo> joinList = new ArrayList<>();
        List<ExpressionVo> whereExpressionList = new ArrayList<>();
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_account` b ON b.`resource_id` = a.`id`
            LEFT JOIN `cmdb_resourcecenter_account` c ON c.`id` = b.`account_id`
        </if>

        <if test="protocolIdList != null and protocolIdList.size() > 0">
            AND c.`protocol_id` IN
            <foreach collection="protocolIdList" item="protocolId" open="(" separator="," close=")">
                #{protocolId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
//            System.out.println("d");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
            whereExpressionList.add($sql.exp("c.protocol_id", "in", queryCriteriaVo.getProtocolIdList()));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_tag` d ON d.`resource_id` = a.`id`
        </if>

        <if test="tagIdList != null and tagIdList.size() > 0">
            AND d.`tag_id` IN
            <foreach collection="tagIdList" item="tagId" open="(" separator="," close=")">
                #{tagId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
//            System.out.println("e");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_tag", "d").withOn($sql.exp("d.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("d.tag_id", "in", queryCriteriaVo.getTagIdList()));
        }
        /*
        <if test="inspectJobPhaseNodeStatusList !=null and inspectJobPhaseNodeStatusList.size() > 0">
            left join autoexec_job_resource_inspect ajri on ajri.resource_id=a.id
            left join autoexec_job_phase_node ajpn on ajpn.job_phase_id =ajri.phase_id AND ajpn.resource_id = a.id
        </if>

        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
//            System.out.println("f");
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("ajpn.status", "in", queryCriteriaVo.getInspectJobPhaseNodeStatusList()));
        }
        if (queryCriteriaVo.getJobId() != null) {
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            whereExpressionList.add($sql.exp("ajpn.job_id", "=", queryCriteriaVo.getJobId()));
        }
        /*
         <if test="typeIdList != null and typeIdList.size() > 0">
            AND a.`type_id` IN
            <foreach collection="typeIdList" item="typeId" open="(" separator="," close=")">
                #{typeId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTypeIdList())) {
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
        }
        /*
        <if test="stateIdList != null and stateIdList.size() > 0">
            AND a.`state_id` IN
            <foreach collection="stateIdList" item="stateId" open="(" separator="," close=")">
                #{stateId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getStateIdList())) {
//            System.out.println("p");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
        }
        /*
        <if test="vendorIdList != null and vendorIdList.size() > 0">
            AND a.`vendor_id` IN
            <foreach collection="vendorIdList" item="vendorId" open="(" separator="," close=")">
                #{vendorId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getVendorIdList())) {
//            System.out.println("q");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
        }
        /*
        <if test="envIdList != null and envIdList.size() > 0">
            AND a.`env_id` IN
            <foreach collection="envIdList" item="envId" open="(" separator="," close=")">
                #{envId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getEnvIdList())) {
//            System.out.println("r");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="appSystemIdList != null and appSystemIdList.size() > 0">
            AND a.`app_system_id` IN
            <foreach collection="appSystemIdList" item="appSystemId" open="(" separator="," close=")">
                #{appSystemId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppSystemIdList())) {
//            System.out.println("t");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
        }
        /*
        <if test="appModuleIdList != null and appModuleIdList.size() > 0">
            AND a.`app_module_id` IN
            <foreach collection="appModuleIdList" item="appModuleId" open="(" separator="," close=")">
                #{appModuleId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAppModuleIdList())) {
//            System.out.println("u");
            whereExpressionList.add($sql.exp(fieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
        }
        /*
        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectStatusList())) {
//            System.out.println("x");
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn($sql.exp("ajpn.resource_id", "=", fieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_cientity_inspect", "cci").withOn($sql.exp(
                    $sql.exp("cci.ci_entity_id", "=", fieldName2ColumnMap.get("id").toString()),
                    "and",
                    $sql.exp("cci.job_id", "=", "ajpn.job_id")
            )));
            whereExpressionList.add($sql.exp("cci.inspect_status", "in", queryCriteriaVo.getInspectStatusList()));
        }
        sqlVo.withJoinList(joinList);
        sqlVo.withWhereExpressionList(whereExpressionList);
        return sqlVo;
    }

    private PlainSelect getPlainSelect(ResourceEntityConfigVo config, Map<String, Column> fieldName2ColumnMap) {
        PlainSelect plainSelect = null;
        if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
            ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
            plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
            fieldName2ColumnMap.putAll(resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap());
        } else {
            ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
            plainSelect = resourceViewGenerateSqlUtil.getSql();
            fieldName2ColumnMap.putAll(resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap());
        }
        return plainSelect;
    }
}
