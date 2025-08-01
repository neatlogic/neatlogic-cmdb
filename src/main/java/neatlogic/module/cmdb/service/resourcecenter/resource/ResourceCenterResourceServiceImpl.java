/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.service.resourcecenter.resource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.config.*;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.CmdbTenantConfig;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceViewFieldMappingException;
import neatlogic.framework.cmdb.utils.ResourceViewGenerateSqlUtil;
import neatlogic.framework.cmdb.utils.ResourceViewGenerateSqlUtilForTiDB;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.dto.DataBaseViewInfoVo;
import neatlogic.framework.fulltextindex.utils.FullTextIndexUtil;
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
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
@Service
public class ResourceCenterResourceServiceImpl implements IResourceCenterResourceService {

    private final Logger logger = LoggerFactory.getLogger(ResourceCenterResourceServiceImpl.class);
    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    @Resource
    ResourceMapper resourceMapper;
    @Resource
    ResourceTagMapper resourceTagMapper;
    @Resource
    ResourceAccountMapper resourceAccountMapper;

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
    public ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj) {
        if(!jsonObj.containsKey("typeId") && !jsonObj.containsKey("typeIdList")){
            List<Long> ciIdList = resourceEntityMapper.getAllResourceTypeCiIdList();
            jsonObj.put("typeIdList", ciIdList);
        }
        return assembleResourceSearchVo(jsonObj, true);
    }

    @Override
    public ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj, boolean isIncludeSon) {
        ResourceSearchVo searchVo = jsonObj.toJavaObject(ResourceSearchVo.class);
        searchVo.setIsHasAuth(AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class));
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            if (!searchVo.getIsHasAuth()) {
                List<CiVo> authedCiList;
                authedCiList = ciMapper.getDownwardCiEntityQueryCiListByLR(ciVo.getLft(), ciVo.getRht(), UserContext.get().getAuthenticationInfoVo(), searchVo.getIsHasAuth());
                if (CollectionUtils.isNotEmpty(authedCiList)) {
                    if (isIncludeSon) {
                        List<CiVo> inCludeSonCiList = ciMapper.getBatchDownwardCiListByCiList(authedCiList);
                        Set<Long> ciIdList = inCludeSonCiList.stream().map(CiVo::getId).collect(Collectors.toSet());
                        searchVo.setAuthedTypeIdList(new ArrayList<>(ciIdList));
                    } else {
                        searchVo.setAuthedTypeIdList(authedCiList.stream().map(CiVo::getId).collect(Collectors.toList()));
                    }
                }
            }
            List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
            searchVo.setTypeIdList(ciIdList);
        } else {
            List<Long> typeIdList = searchVo.getTypeIdList();
            if (CollectionUtils.isNotEmpty(typeIdList)) {
                Set<Long> authedCiIdSet = new HashSet<>();
                Set<Long> ciIdSet = new HashSet<>();
                for (Long ciId : typeIdList) {
                    CiVo ciVo = ciMapper.getCiById(ciId);
                    if (ciVo == null) {
                        throw new CiNotFoundException(ciId);
                    }
                    if (!searchVo.getIsHasAuth()) {
                        List<CiVo> authedCiList;
                        authedCiList = ciMapper.getDownwardCiEntityQueryCiListByLR(ciVo.getLft(), ciVo.getRht(), UserContext.get().getAuthenticationInfoVo(), searchVo.getIsHasAuth());
                        if (CollectionUtils.isNotEmpty(authedCiList)) {
                            if (isIncludeSon) {
                                List<CiVo> inCludeSonCiList = ciMapper.getBatchDownwardCiListByCiList(authedCiList);
                                Set<Long> ciIdList = inCludeSonCiList.stream().map(CiVo::getId).collect(Collectors.toSet());
                                authedCiIdSet.addAll(ciIdList);
                            } else {
                                authedCiIdSet.addAll(authedCiList.stream().map(CiVo::getId).collect(Collectors.toSet()));
                            }
                        }
                    }
                    List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                    ciIdSet.addAll(ciIdList);

                }
                searchVo.setAuthedTypeIdList(new ArrayList<>(authedCiIdSet));
                searchVo.setTypeIdList(new ArrayList<>(ciIdSet));
            }
        }
        //下面逻辑改成通过join对应的表实现
//        List<Long> resourceIdList = null;
//        if (CollectionUtils.isNotEmpty(searchVo.getProtocolIdList())) {
//            List<Long> idList = resourceCenterMapper.getResourceIdListByProtocolIdList(searchVo);
//            if (resourceIdList == null) {
//                resourceIdList = idList;
//            } else {
//                resourceIdList.retainAll(idList);
//            }
//        }
//        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
//            List<Long> idList = resourceCenterMapper.getResourceIdListByTagIdList(searchVo);
//            if (resourceIdList == null) {
//                resourceIdList = idList;
//            } else {
//                resourceIdList.retainAll(idList);
//            }
//        }
//        searchVo.setIdList(resourceIdList);
        searchVo.setIsHasAuth(AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class) || Objects.equals("0", ConfigManager.getConfig(CmdbTenantConfig.IS_RESOURCECENTER_AUTH)));
        return searchVo;
    }

    @Override
    public void assembleResourceSearchVo(ResourceSearchVo searchVo, boolean isIncludeSon) {
        boolean isHasAuth = AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class);
        Long typeId = searchVo.getTypeId();
        List<Long> typeIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(searchVo.getTypeIdList())) {
            typeIdList.addAll(searchVo.getTypeIdList());
        }
        if (typeId != null && !typeIdList.contains(typeId)) {
            typeIdList.add(typeId);
        }
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            Set<Long> authedCiIdSet = new HashSet<>();
            Set<Long> ciIdSet = new HashSet<>();
            for (Long ciId : typeIdList) {
                CiVo ciVo = ciMapper.getCiById(ciId);
                if (ciVo == null) {
                    throw new CiNotFoundException(ciId);
                }
                if (!isHasAuth) {
                    List<CiVo> authedCiList;
                    authedCiList = ciMapper.getDownwardCiEntityQueryCiListByLR(ciVo.getLft(), ciVo.getRht(), UserContext.get().getAuthenticationInfoVo(), searchVo.getIsHasAuth());
                    if (CollectionUtils.isNotEmpty(authedCiList)) {
                        if (isIncludeSon) {
                            List<CiVo> inCludeSonCiList = ciMapper.getBatchDownwardCiListByCiList(authedCiList);
                            Set<Long> ciIdList = inCludeSonCiList.stream().map(CiVo::getId).collect(Collectors.toSet());
                            authedCiIdSet.addAll(ciIdList);
                        } else {
                            authedCiIdSet.addAll(authedCiList.stream().map(CiVo::getId).collect(Collectors.toSet()));
                        }
                    }
                }
                List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                ciIdSet.addAll(ciIdList);

            }
            searchVo.setAuthedTypeIdList(new ArrayList<>(authedCiIdSet));
            searchVo.setTypeIdList(new ArrayList<>(ciIdSet));
        }
        searchVo.setIsHasAuth(AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class) || Objects.equals("0", ConfigManager.getConfig(CmdbTenantConfig.IS_RESOURCECENTER_AUTH)));
    }

    @Override
    public void handleBatchSearchList(ResourceSearchVo searchVo) {
        List<String> batchSearchList = searchVo.getBatchSearchList();
        if (CollectionUtils.isNotEmpty(batchSearchList)) {
            List<String> keywordList = new ArrayList<>();
            if (Objects.equals(searchVo.getSearchField(), "name")) {
//                List<String> list = new ArrayList<>();
                for (String keyword : batchSearchList) {
//                    list.add("%" + keyword + "%");
                    keywordList.addAll(FullTextIndexUtil.sliceKeyword(keyword));
                }
//                searchVo.setBatchSearchList(list);
            } else if (Objects.equals(searchVo.getSearchField(), "ip")) {
//                List<String> list = new ArrayList<>();
                for (String keyword : batchSearchList) {
                    if (keyword.endsWith("*")) {
                        keyword = keyword.substring(0, keyword.length() - 1);
                        if (keyword.endsWith(".")) {
                            keyword = keyword.substring(0, keyword.length() - 1);
                        }
                    }
//                    list.add(keyword);
                    keywordList.addAll(FullTextIndexUtil.sliceKeyword(keyword));
                }
//                searchVo.setBatchSearchList(list);
            }
//            searchVo.setKeyword(String.join(" ", keywordList));
            searchVo.setBatchSearchList(keywordList);
        }
    }

    @Override
    public void setIpFieldAttrIdAndNameFieldAttrId(ResourceSearchVo searchVo) {
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
        if (resourceEntityVo != null) {
            ResourceEntityConfigVo config = resourceEntityVo.getConfig();
            if (config != null) {
                List<ResourceEntityFieldMappingVo> mappingList = config.getFieldMappingList();
                if (CollectionUtils.isNotEmpty(mappingList)) {
                    Long nameAttrId = null;
                    Long ipAttrId = null;
                    for (ResourceEntityFieldMappingVo mappingVo : mappingList) {
                        if (Objects.equals(mappingVo.getField(), "name")) {
                            CiVo ciVo = ciMapper.getCiByName(mappingVo.getFromCi());
                            if (ciVo != null) {
                                AttrVo attr = attrMapper.getAttrByCiIdAndName(ciVo.getId(), mappingVo.getFromAttr());
                                if (attr != null) {
                                    nameAttrId = attr.getId();
                                }
                            }
                        } else if (Objects.equals(mappingVo.getField(), "ip")) {
                            CiVo ciVo = ciMapper.getCiByName(mappingVo.getFromCi());
                            if (ciVo != null) {
                                AttrVo attr = attrMapper.getAttrByCiIdAndName(ciVo.getId(), mappingVo.getFromAttr());
                                if (attr != null) {
                                    ipAttrId = attr.getId();
                                }
                            }
                        }
                        if (nameAttrId != null && ipAttrId != null) {
                            break;
                        }
                    }
                    searchVo.setIpFieldAttrId(ipAttrId);
                    searchVo.setNameFieldAttrId(nameAttrId);
                }
            }
        }
    }

    @Override
    public void setIsIpFieldSortAndIsNameFieldSort(ResourceSearchVo searchVo) {
        if (StringUtils.isNotBlank(searchVo.getKeyword())) {
            int ipKeywordCount = resourceMapper.getResourceCountByIpKeyword(searchVo);
            if (ipKeywordCount > 0) {
                searchVo.setIsIpFieldSort(1);
            } else {
                int nameKeywordCount = resourceMapper.getResourceCountByNameKeyword(searchVo);
                if (nameKeywordCount > 0) {
                    searchVo.setIsNameFieldSort(1);
                }
            }
        }
    }

    @Override
    public List<Long> getDownwardCiIdListByCiIdList(List<Long> idList) {
        Set<Long> ciIdSet = new HashSet<>();
        for (Long ciId : idList) {
            CiVo ciVo = ciMapper.getCiById(ciId);
            if (ciVo == null) {
                throw new CiNotFoundException(ciId);
            }
            List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
            ciIdSet.addAll(ciIdList);
        }
        return new ArrayList<>(ciIdSet);
    }

    @Override
    public Map<Long, List<AccountVo>> getResourceAccountByResourceIdList(List<Long> idList) {
        Map<Long, List<AccountVo>> resourceAccountVoMap = new HashMap<>();
        List<ResourceAccountVo> resourceAccountVoList = resourceAccountMapper.getResourceAccountListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            Set<Long> accountIdSet = resourceAccountVoList.stream().map(ResourceAccountVo::getAccountId).collect(Collectors.toSet());
            List<AccountVo> accountList = resourceAccountMapper.getAccountListByIdList(new ArrayList<>(accountIdSet));
            Map<Long, AccountVo> accountMap = accountList.stream().collect(Collectors.toMap(AccountVo::getId, e -> e));
            for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
                AccountVo accountVo = accountMap.get(resourceAccountVo.getAccountId());
                if (accountVo != null) {
                    resourceAccountVoMap.computeIfAbsent(resourceAccountVo.getResourceId(), k -> new ArrayList<>()).add(accountVo);
                }
            }
        }
        return resourceAccountVoMap;
    }

    @Override
    public Map<Long, List<TagVo>> getResourceTagByResourceIdList(List<Long> idList) {
        Map<Long, List<TagVo>> resourceTagVoMap = new HashMap<>();
        List<ResourceTagVo> resourceTagVoList = resourceTagMapper.getResourceTagListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            Set<Long> tagIdSet = resourceTagVoList.stream().map(ResourceTagVo::getTagId).collect(Collectors.toSet());
            List<TagVo> tagList = resourceTagMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
            Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(TagVo::getId, e -> e));
            for (ResourceTagVo resourceTagVo : resourceTagVoList) {
                TagVo tagVo = tagMap.get(resourceTagVo.getTagId());
                if (tagVo != null) {
                    resourceTagVoMap.computeIfAbsent(resourceTagVo.getResourceId(), k -> new ArrayList<>()).add(tagVo);
                }
            }
        }
        return resourceTagVoMap;
    }

//    @Override
//    public void addResourceAccount(List<Long> idList, List<ResourceVo> resourceVoList) {
//        Map<Long, List<AccountVo>> resourceAccountVoMap = new HashMap<>();
//        List<ResourceAccountVo> resourceAccountVoList = resourceAccountMapper.getResourceAccountListByResourceIdList(idList);
//        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
//            Set<Long> accountIdSet = resourceAccountVoList.stream().map(ResourceAccountVo::getAccountId).collect(Collectors.toSet());
//            List<AccountVo> accountList = resourceAccountMapper.getAccountListByIdList(new ArrayList<>(accountIdSet));
//            Map<Long, AccountVo> accountMap = accountList.stream().collect(Collectors.toMap(AccountVo::getId, e -> e));
//            for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
//                resourceAccountVoMap.computeIfAbsent(resourceAccountVo.getResourceId(), k -> new ArrayList<>()).add(accountMap.get(resourceAccountVo.getAccountId()));
//            }
//        }
//        for (ResourceVo resourceVo : resourceVoList) {
//            List<AccountVo> accountVoList = resourceAccountVoMap.get(resourceVo.getId());
//            if (CollectionUtils.isNotEmpty(accountVoList)) {
//                resourceVo.setAccountList(accountVoList);
//            }
//        }
//    }

//    @Override
//    public void addResourceTag(List<Long> idList, List<ResourceVo> resourceVoList) {
//        Map<Long, List<TagVo>> resourceTagVoMap = new HashMap<>();
//        List<ResourceTagVo> resourceTagVoList = resourceTagMapper.getResourceTagListByResourceIdList(idList);
//        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
//            Set<Long> tagIdSet = resourceTagVoList.stream().map(ResourceTagVo::getTagId).collect(Collectors.toSet());
//            List<TagVo> tagList = resourceTagMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
//            Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(TagVo::getId, e -> e));
//            for (ResourceTagVo resourceTagVo : resourceTagVoList) {
//                resourceTagVoMap.computeIfAbsent(resourceTagVo.getResourceId(), k -> new ArrayList<>()).add(tagMap.get(resourceTagVo.getTagId()));
//            }
//        }
//
//        for (ResourceVo resourceVo : resourceVoList) {
//            List<TagVo> tagVoList = resourceTagVoMap.get(resourceVo.getId());
//            if (CollectionUtils.isNotEmpty(tagVoList)) {
//                resourceVo.setTagList(tagVoList.stream().map(TagVo::getName).collect(Collectors.toList()));
//            }
//        }
//    }

    /**
     * 获取对应模块的应用清单列表
     * 其中清单列表有 系统 应用实例 应用实例集群 DB实例 DB实例集群 访问入口
     *
     * @param searchVo
     * @return
     */
//    @Deprecated
//    @Override
//    public JSONArray getAppModuleResourceList(ResourceSearchVo searchVo) {
//        JSONArray tableList = new JSONArray();
//        List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(AppModuleResourceType.getNameList());
//        List<Long> resourceTypeIdList = new ArrayList<>();
//        Long appSystemId = searchVo.getAppSystemId();
//        Long appModuleId = searchVo.getAppModuleId();
//        Long typeId = searchVo.getTypeId();
//        if (typeId != null) {
//            CiVo ciVo = ciMapper.getCiById(typeId);
//            if (ciVo == null) {
//                throw new CiNotFoundException(typeId);
//            }
//            resourceTypeIdList.add(typeId);
//        } else if (CollectionUtils.isNotEmpty(searchVo.getTypeIdList())) {
//            resourceTypeIdList.addAll(searchVo.getTypeIdList());
//            searchVo.setTypeIdList(null);
//        } else if (appModuleId != null) {
//            CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(appModuleId);
//            if (ciEntityVo == null) {
//                throw new AppModuleNotFoundException(appModuleId);
//            }
//            Set<Long> resourceTypeIdSet = resourceTempMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
//            resourceTypeIdList.addAll(resourceTypeIdSet);
//            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
//                resourceTypeIdSet = resourceTempMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
//                resourceTypeIdList.addAll(resourceTypeIdSet);
//            }
//        } else if (appSystemId != null) {
//            CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(appSystemId);
//            if (ciEntityVo == null) {
//                throw new AppSystemNotFoundException(appSystemId);
//            }
//            Set<Long> resourceTypeIdSet = resourceTempMapper.getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
//            resourceTypeIdList.addAll(resourceTypeIdSet);
//            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
//                resourceTypeIdSet = resourceTempMapper.getOsResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
//                resourceTypeIdList.addAll(resourceTypeIdSet);
//            }
//        }
//
//        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
//            List<CiVo> ciList = ciMapper.getAllCi(resourceTypeIdList);
//            for (CiVo ciVo : ciList) {
//                ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName());
//                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
//                if (StringUtils.isBlank(resourceTypeName)) {
//                    continue;
//                }
//                String actionKey = AppModuleResourceType.getAction(resourceTypeName);
//                if (StringUtils.isBlank(actionKey)) {
//                    continue;
//                }
//                searchVo.setTypeId(ciVo.getId());
//                List<ResourceVo> returnList = searchMap.get(actionKey).execute(searchVo);
//                if (CollectionUtils.isNotEmpty(returnList)) {
//                    JSONObject tableObj = TableResultUtil.getResult(returnList, searchVo);
//                    tableObj.put("type", resourceTypeVo);
//                    tableList.add(tableObj);
//                }
//            }
//        }
//        return tableList;
//    }

//    @PostConstruct
//    @Deprecated
//    public void searchDispatcherInit() {
//        searchMap.put("ipObject", (searchVo) -> {
//            int rowNum = resourceTempMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceTempMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceMapper.getResourceListByIdList(idList);
//                }
//            }
//            return new ArrayList<>();
//        });
//
//        searchMap.put("OS", (searchVo) -> {
//            int rowNum = resourceTempMapper.getOsResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceTempMapper.getOsResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceTempMapper.getOsResourceListByIdList(idList);
//                }
//            }
//            return new ArrayList<>();
//        });
//
//        searchMap.put("APPIns", (searchVo) -> {
//            int rowNum = resourceTempMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceTempMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceMapper.getResourceListByIdList(idList);
//                }
//            }
//            return new ArrayList<>();
//        });
//
//        searchMap.put("DBIns", (searchVo) -> {
//            int rowNum = resourceTempMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceTempMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceTempMapper.getDbInstanceResourceListByIdList(idList);
//                }
//            }
//            return new ArrayList<>();
//        });
//
//    }
//    @Deprecated
//    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo) {
//        for (CiVo ciVo : resourceCiVoList) {
//            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
//                return ciVo.getName();
//            }
//        }
//        return null;
//    }

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    public void addTagAndAccountInformation(List<ResourceVo> resourceList) {
        List<Long> idList = resourceList.stream().map(ResourceVo::getId).collect(Collectors.toList());
        Map<Long, List<AccountVo>> accountMap = getResourceAccountByResourceIdList(idList);
        Map<Long, List<TagVo>> tagMap = getResourceTagByResourceIdList(idList);
        for (ResourceVo resourceVo : resourceList) {
            Long id = resourceVo.getId();
            List<AccountVo> accountList = accountMap.get(id);
            if (CollectionUtils.isNotEmpty(accountList)) {
                resourceVo.setAccountList(accountList);
            }
            List<TagVo> tagList = tagMap.get(id);
            if (CollectionUtils.isNotEmpty(tagList)) {
                resourceVo.setTagList(tagList.stream().map(TagVo::getName).collect(Collectors.toList()));
            }
        }
    }

    @Override
    public void addTagInformation(List<ResourceVo> resourceList) {
        List<Long> idList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, List<TagVo>> tagMap = getResourceTagByResourceIdList(idList);
        for (ResourceVo resourceVo : resourceList) {
            List<TagVo> tagList = tagMap.get(resourceVo.getId());
            if (CollectionUtils.isNotEmpty(tagList)) {
                resourceVo.setTagList(tagList.stream().map(TagVo::getName).collect(Collectors.toList()));
            }
        }
    }

    @Override
    public void addAccountInformation(List<ResourceVo> resourceList) {
        List<Long> idList = resourceList.stream().map(ResourceVo::getId).collect(Collectors.toList());
        Map<Long, List<AccountVo>> accountMap = getResourceAccountByResourceIdList(idList);
        for (ResourceVo resourceVo : resourceList) {
            List<AccountVo> accountList = accountMap.get(resourceVo.getId());
            if (CollectionUtils.isNotEmpty(accountList)) {
                resourceVo.setAccountList(accountList);
            }
        }
    }

    @Override
    public String buildResourceView(ResourceEntityVo resourceEntityVo) {
        String viewName = resourceEntityVo.getName();
        String select = null;
        String error = StringUtils.EMPTY;
        try {
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                select = resourceViewGenerateSqlUtilForTiDB.getSql().toString();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                select = resourceViewGenerateSqlUtil.getSql().toString();
            }
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
            PlainSelect plainSelect = null;
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }

            SqlVo sqlVo = getSqlVo(queryCriteriaVo, filterItemFieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
//            List<JoinVo> joinList = getJoinList(queryCriteriaVo, filterItemFieldName2ColumnMap);
//            $sql.addJoinList(plainSelect, joinList);
//            List<ExpressionVo> whereExpressionList = getWhereExpressionList(queryCriteriaVo, filterItemFieldName2ColumnMap);
//            $sql.addWhereExpressionList(plainSelect, whereExpressionList);
            if (CollectionUtils.isNotEmpty(searchVo.getKeywordList()) && searchVo.getNameFieldAttrId() != null && searchVo.getIpFieldAttrId() != null) {
                $sql.addOrderBy(plainSelect, $sql.fun("COUNT", "fw.word").withDistinct(true), "desc");
            }
            Column idColumn = filterItemFieldName2ColumnMap.get("id");
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
            PlainSelect plainSelect = null;
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }
            SqlVo sqlVo = getSqlVo(queryCriteriaVo, filterItemFieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
//            List<JoinVo> joinList = getJoinList(queryCriteriaVo, filterItemFieldName2ColumnMap);
//            $sql.addJoinList(plainSelect, joinList);
//            List<ExpressionVo> whereExpressionList = getWhereExpressionList(queryCriteriaVo, filterItemFieldName2ColumnMap);
//            $sql.addWhereExpressionList(plainSelect, whereExpressionList);
            Column column = filterItemFieldName2ColumnMap.get("id");
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
            PlainSelect plainSelect = null;
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
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }
            Column column = filterItemFieldName2ColumnMap.get("id");
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
            PlainSelect plainSelect = null;
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setInspectJobPhaseNodeStatusList(null);
            queryCriteriaVo.setBatchSearchList(null);
            queryCriteriaVo.setIpFieldAttrId(null);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }
            SqlVo sqlVo = getSqlVo(queryCriteriaVo, filterItemFieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            Column column = filterItemFieldName2ColumnMap.get("id");
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
            PlainSelect plainSelect = null;
            ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
            ResourceEntityConfigVo config = getResourceEntityConfigVo(resourceEntityVo);
            ResourceQueryCriteriaVo queryCriteriaVo = new ResourceQueryCriteriaVo(searchVo);
            queryCriteriaVo.setInspectJobPhaseNodeStatusList(null);
            queryCriteriaVo.setBatchSearchList(null);
            queryCriteriaVo.setNameFieldAttrId(null);
            List<String> selectItemFieldNameList = new ArrayList<>();
            selectItemFieldNameList.add("id");
            List<String> filterItemFieldNameList = getFilterItemFieldNameList(queryCriteriaVo);
            filterItemFieldNameList.add("id");
            config.setSelectItemFieldNameList(selectItemFieldNameList);
            config.setFilterItemFieldNameList(filterItemFieldNameList);
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }
            SqlVo sqlVo = getSqlVo(queryCriteriaVo, filterItemFieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            Column column = filterItemFieldName2ColumnMap.get("id");
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
            PlainSelect plainSelect = null;
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
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }
            SqlVo sqlVo = getSqlVo(queryCriteriaVo, filterItemFieldName2ColumnMap);
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
            PlainSelect plainSelect = null;
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
            Map<String, Column> filterItemFieldName2ColumnMap = new HashMap<>();
            if (Objects.equals(DatasourceManager.getDatabaseId(), DatabaseVendor.TIDB.getDatabaseId())) {
                ResourceViewGenerateSqlUtilForTiDB resourceViewGenerateSqlUtilForTiDB = new ResourceViewGenerateSqlUtilForTiDB(config);
                plainSelect = resourceViewGenerateSqlUtilForTiDB.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtilForTiDB.getFilterItemFieldName2ColumnMap();
            } else {
                ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
                plainSelect = resourceViewGenerateSqlUtil.getSql();
                filterItemFieldName2ColumnMap = resourceViewGenerateSqlUtil.getFilterItemFieldName2ColumnMap();
            }
            SqlVo sqlVo = getSqlVo(queryCriteriaVo, filterItemFieldName2ColumnMap);
            $sql.addSql(plainSelect, sqlVo);
            String keyword = searchVo.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                keyword = "%" + keyword + "%";
                Column nameColumn = filterItemFieldName2ColumnMap.get("name");
                Column ipColumn = filterItemFieldName2ColumnMap.get("ip");
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
                    Column ipColumn = filterItemFieldName2ColumnMap.get("ip");
                    ExpressionVo andExp = $sql.exp(ipColumn.toString(), "=", $sql.value(inputNode.getIp()));
                    Column portColumn = filterItemFieldName2ColumnMap.get("port");
                    if (inputNode.getPort() != null) {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "=", inputNode.getPort()));
                    } else {
                        andExp = $sql.exp(andExp, "and", $sql.exp(portColumn.toString(), "is null"));
                    }
                    if (StringUtils.isNotBlank(inputNode.getName())) {
                        Column nameColumn = filterItemFieldName2ColumnMap.get("name");
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
     *
     * @param viewName
     * @param config
     * @return
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
        CiVo mainCiVo = ciMapper.getCiByName(mainCi);
        if (mainCiVo == null) {
            throw new ResourceViewFieldMappingException(viewName, mainCi);
        }
        newConfig.setMainCi(mainCi);
        newConfig.setMainCiVo(mainCiVo);
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
                CiVo fromCiVo = ciMapper.getCiByName(fromCi);
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
                CiVo fromCiVo = ciMapper.getCiByName(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String fromAttr = fieldMappingVo.getFromAttr();
                if (StringUtils.isBlank(fromAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                AttrVo fromAttrVo = getAttrVo(fromCiVo, fromAttr);
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
                    CiVo toCiVo = ciMapper.getCiByName(toCi);
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
                        AttrVo toAttrVo = getAttrVo(toCiVo, toAttr);
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
                CiVo fromCiVo = ciMapper.getCiByName(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String toCi = fieldMappingVo.getToCi();
                if (StringUtils.isBlank(toCi)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "toCi", toCi);
                }
                CiVo toCiVo = ciMapper.getCiByName(toCi);
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
                        AttrVo fromAttrVo = getAttrVo(fromCiVo, fromAttr);
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
                            AttrVo toAttrVo = getAttrVo(toCiVo, toAttr);
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
                            CiVo rightCiVo = ciMapper.getCiByName(ciName);
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
                                    AttrVo fromAttrVo = getAttrVo(fromCiVo, fromAttr);
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
                                CiVo toCiVo = ciMapper.getCiByName(toCi);
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
                                        AttrVo toAttrVo = getAttrVo(toCiVo, toAttr);
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
                                CiVo fromCiVo = ciMapper.getCiByName(fromCi);
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
                CiVo fromCiVo = ciMapper.getCiByName(fromCi);
                if (fromCiVo == null) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromCi", fromCi);
                }
                String fromAttr = fieldMappingVo.getFromAttr();
                if (StringUtils.isBlank(fromAttr)) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
                }
                GlobalAttrVo globalAttrVo = new GlobalAttrVo();
                globalAttrVo.setName(fromAttr);
                if (globalAttrMapper.checkGlobalAttrNameIsUsed(globalAttrVo) == 0) {
                    throw new ResourceViewFieldMappingException(viewName, field, "fromAttr", fromAttr);
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
            for (ResourceEntityRelLinkVo linkVo : relLinkList) {
                String leftCi = linkVo.getLeftCi();
                String rightCi = linkVo.getRightCi();
                CiVo leftCiVo = ciMapper.getCiByName(leftCi);
                if (leftCiVo == null) {
                    throw new CiNotFoundException(leftCi);
                }
                CiVo rightCiVo = ciMapper.getCiByName(rightCi);
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

    private AttrVo getAttrVo(CiVo ciVo, String attrName) {
        List<CiVo> upwardCiList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        for (CiVo ci : upwardCiList) {
            AttrVo attr = attrMapper.getDeclaredAttrByCiIdAndName(ci.getId(), attrName);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    /**
     * 根据queryCriteriaVo查询条件收集组装动态sql时，需要返回的条件列
     * @param queryCriteriaVo
     * @return
     */
    private List<String> getFilterItemFieldNameList(ResourceQueryCriteriaVo queryCriteriaVo) {
        Set<String> filterItemFieldNameSet = new HashSet<>();
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
        return new ArrayList<>(filterItemFieldNameSet);
    }

    /**
     * 根据queryCriteriaVo查询条件补充业务逻辑过滤条件，包括join表和where条件
     * @param queryCriteriaVo
     * @param plainSelect
     * @param filterItemFieldName2ColumnMap
     * @return
     */
    private PlainSelect supplementBusinessLogicByResourceSearchVo(ResourceQueryCriteriaVo queryCriteriaVo, PlainSelect plainSelect, Map<String, Column> filterItemFieldName2ColumnMap) {
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
            Table ffcTable = new Table("fulltextindex_field_cmdb").withAlias(new Alias("ffc").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(ffcTable, "target_id"), filterItemFieldName2ColumnMap.get("id"));
                ExpressionList values = new ExpressionList();
                if (queryCriteriaVo.getNameFieldAttrId() != null) {
                    values.addExpressions(new LongValue(queryCriteriaVo.getNameFieldAttrId()));
                }
                if (queryCriteriaVo.getIpFieldAttrId() != null) {
                    values.addExpressions(new LongValue(queryCriteriaVo.getIpFieldAttrId()));
                }
                InExpression inExpression = new InExpression(new Column(ffcTable, "target_field"), values);
                Join join = new Join().withRightItem(ffcTable).addOnExpression(new AndExpression(equalsTo, inExpression));
                plainSelect.addJoins(join);
            }
            Table fwTable = new Table("fulltextindex_word").withAlias(new Alias("fw").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(fwTable, "id"), new Column(ffcTable,"word_id"));
                ExpressionList values = new ExpressionList();
                for (String keyword : queryCriteriaVo.getKeywordList()) {
                    values.addExpressions(new StringValue(keyword));
                }
                InExpression inExpression = new InExpression(new Column(fwTable, "word"), values);
                Join join = new Join().withRightItem(fwTable).addOnExpression(new AndExpression(equalsTo, inExpression));
                plainSelect.addJoins(join);
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
            Table ffc2Table = new Table("fulltextindex_field_cmdb").withAlias(new Alias("ffc2").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(ffc2Table, "target_id"), filterItemFieldName2ColumnMap.get("id"));
                LongValue longValue = null;
                if (Objects.equals(queryCriteriaVo.getSearchField(), "name")) {
                    longValue = new LongValue(queryCriteriaVo.getNameFieldAttrId());
                } else {
                    longValue = new LongValue(queryCriteriaVo.getIpFieldAttrId());
                }
                EqualsTo equalsTo2 = new EqualsTo(new Column(ffc2Table, "target_field"), longValue);
                Join join = new Join().withRightItem(ffc2Table).addOnExpression(new AndExpression(equalsTo, equalsTo2));
                plainSelect.addJoins(join);
            }
            Table fw2Table = new Table("fulltextindex_word").withAlias(new Alias("fw2").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(fw2Table, "id"), new Column(ffc2Table,"word_id"));
                ExpressionList values = new ExpressionList();
                for (String keyword : queryCriteriaVo.getBatchSearchList()) {
                    values.addExpressions(new StringValue(keyword));
                }
                InExpression inExpression = new InExpression(new Column(fw2Table, "word"), values);
                Join join = new Join().withRightItem(fw2Table).addOnExpression(new AndExpression(equalsTo, inExpression));
                plainSelect.addJoins(join);
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
            Table bTable = new Table("cmdb_resourcecenter_resource_account").withAlias(new Alias("b").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(bTable, "resource_id"), filterItemFieldName2ColumnMap.get("id"));
                Join join = new Join().withLeft(true).withRightItem(bTable).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Table cTable = new Table("cmdb_resourcecenter_account").withAlias(new Alias("c").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(cTable, "id"), new Column(bTable,"account_id"));
                Join join = new Join().withLeft(true).withRightItem(cTable).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Column column = new Column(cTable,"protocol_id");
            ExpressionList values = new ExpressionList();
            for (Long protocolId : queryCriteriaVo.getProtocolIdList()) {
                values.addExpressions(new LongValue(protocolId));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Table dTable = new Table("cmdb_resourcecenter_resource_tag").withAlias(new Alias("d").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(dTable, "resource_id"), filterItemFieldName2ColumnMap.get("id"));
                Join join = new Join().withLeft(true).withRightItem(dTable).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Column column = new Column(dTable, "tag_id");
            ExpressionList values = new ExpressionList();
            for (Long tagId : queryCriteriaVo.getTagIdList()) {
                values.addExpressions(new LongValue(tagId));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Table ajriTable = new Table("autoexec_job_resource_inspect").withAlias(new Alias("ajri").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(ajriTable, "resource_id"), filterItemFieldName2ColumnMap.get("id"));
                Join join = new Join().withLeft(true).withRightItem(ajriTable).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Table ajpnTable = new Table("autoexec_job_phase_node").withAlias(new Alias("ajpn").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(ajpnTable, "job_phase_id"), new Column(ajriTable,"phase_id"));
                EqualsTo equalsTo2 = new EqualsTo(new Column(ajpnTable, "resource_id"), filterItemFieldName2ColumnMap.get("id"));
                Join join = new Join().withLeft(true).withRightItem(ajpnTable).addOnExpression(new AndExpression(equalsTo, equalsTo2));
                plainSelect.addJoins(join);
            }
            Column column = new Column(ajpnTable, "status");
            ExpressionList values = new ExpressionList();
            for (String status : queryCriteriaVo.getInspectJobPhaseNodeStatusList()) {
                values.addExpressions(new StringValue(status));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Table ccgTable = new Table("cmdb_cientity_group").withAlias(new Alias("ccg").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(ccgTable, "cientity_id"), filterItemFieldName2ColumnMap.get("id"));
                Join join = new Join().withLeft(true).withRightItem(ccgTable).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Table cgaTable = new Table("cmdb_group_auth").withAlias(new Alias("cga").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(cgaTable, "group_id"), new Column(ccgTable,"group_id"));
                Join join = new Join().withLeft(true).withRightItem(cgaTable).addOnExpression(equalsTo);
                plainSelect.addJoins(join);
            }
            Table cgTable = new Table("cmdb_group").withAlias(new Alias("cg").withUseAs(false));
            {
                EqualsTo equalsTo = new EqualsTo(new Column(cgTable, "id"), new Column(cgaTable, "group_id"));
                ExpressionList values = new ExpressionList();
                if (Objects.equals(queryCriteriaVo.getCmdbGroupType(), "autoexec")) {
                    values.addExpressions(new StringValue("autoexec"));
                } else {
                    values.addExpressions(new StringValue("autoexec"));
                    values.addExpressions(new StringValue("readonly"));
                    values.addExpressions(new StringValue("maintain"));
                }
                InExpression inExpression = new InExpression(new Column(cgTable, "type"), values);
                Join join = new Join().withLeft(true).withRightItem(cgTable).addOnExpression(new AndExpression(equalsTo, inExpression));
                plainSelect.addJoins(join);
            }
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
                Column column = filterItemFieldName2ColumnMap.get("type_id");
                ExpressionList values = new ExpressionList();
                for (Long typeId : queryCriteriaVo.getTypeIdList()) {
                    values.addExpressions(new LongValue(typeId));
                }
                InExpression inExpression = new InExpression(column, values);
                Expression where = plainSelect.getWhere();
                if (where != null) {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                } else {
                    plainSelect.setWhere(inExpression);
                }
            } else if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
                Expression orLeftExpression = null;
                if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthedTypeIdList())) {
                    Column column = filterItemFieldName2ColumnMap.get("type_id");
                    ExpressionList values = new ExpressionList();
                    for (Long authedTypeId : queryCriteriaVo.getAuthedTypeIdList()) {
                        values.addExpressions(new LongValue(authedTypeId));
                    }
                    orLeftExpression = new InExpression(column, values);
                } else {
                    orLeftExpression = new EqualsTo(new LongValue(1), new LongValue(0));
                }
                Expression orRightExpression = null;
                IsNullExpression IsNullExpression = new IsNullExpression().withLeftExpression(new Column("cg.id")).withNot(true);
                Column column = filterItemFieldName2ColumnMap.get("type_id");
                ExpressionList values = new ExpressionList();
                for (Long typeId : queryCriteriaVo.getTypeIdList()) {
                    values.addExpressions(new LongValue(typeId));
                }
                InExpression inExpression = new InExpression(column, values);
                orRightExpression = new AndExpression(IsNullExpression, inExpression);
                Expression orLeftExpression2 = new AndExpression(new EqualsTo(new Column("cga.auth_type"), new StringValue("common")), new EqualsTo(new Column("cga.auth_uuid"), new StringValue("alluser")));
                orLeftExpression2 = new Parenthesis(orLeftExpression2);
                Expression orRightExpression2 = null;
                if (queryCriteriaVo.getAuthenticationInfo() != null) {
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
                        Column column2 = new Column("cga.auth_uuid");
                        ExpressionList values2 = new ExpressionList();
                        for (String uuid : uuidList) {
                            values2.addExpressions(new StringValue(uuid));
                        }
                        orRightExpression2 = new InExpression(column2, values2);
                    }
                }
                if (orRightExpression2 != null) {
                    orRightExpression = new AndExpression(orRightExpression, new Parenthesis(new OrExpression(orLeftExpression2, orRightExpression2)));
                } else {
                    orRightExpression = new AndExpression(orRightExpression, orLeftExpression2);
                }
                OrExpression orExpression = new OrExpression(orLeftExpression, orRightExpression);
                Expression where = plainSelect.getWhere();
                if (where != null) {
                    plainSelect.setWhere(new AndExpression(where, new Parenthesis(orExpression)));
                } else {
                    plainSelect.setWhere(orExpression);
                }
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
            Column column = filterItemFieldName2ColumnMap.get("state_id");
            ExpressionList values = new ExpressionList();
            for (Long stateId : queryCriteriaVo.getStateIdList()) {
                values.addExpressions(new LongValue(stateId));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("vendor_id");
            ExpressionList values = new ExpressionList();
            for (Long vendorId : queryCriteriaVo.getVendorIdList()) {
                values.addExpressions(new LongValue(vendorId));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("env_id");
            ExpressionList values = new ExpressionList();
            for (Long envId : queryCriteriaVo.getEnvIdList()) {
                values.addExpressions(new LongValue(envId));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
        }
        /*
        <if test="isExistNoEnv">
            AND a.`env_id` is null
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getExistNoEnv(), true)) {
            Column column = filterItemFieldName2ColumnMap.get("env_id");

            IsNullExpression isNullExpression = new IsNullExpression();
            isNullExpression.withLeftExpression(column);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, isNullExpression));
            } else {
                plainSelect.setWhere(isNullExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("app_system_id");
            ExpressionList values = new ExpressionList();
            for (Long appSystemIdList : queryCriteriaVo.getAppSystemIdList()) {
                values.addExpressions(new LongValue(appSystemIdList));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("app_module_id");
            ExpressionList values = new ExpressionList();
            for (Long appModuleIdList : queryCriteriaVo.getAppModuleIdList()) {
                values.addExpressions(new LongValue(appModuleIdList));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("id");
            ExpressionList values = new ExpressionList();
            JSONArray defaultValue = queryCriteriaVo.getDefaultValue();
            List<Long> idList = defaultValue.toJavaList(Long.class);
            for (Long id : idList) {
                values.addExpressions(new LongValue(id));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("id");
            ExpressionList values = new ExpressionList();
            for (Long id : queryCriteriaVo.getIdList()) {
                values.addExpressions(new LongValue(id));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
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
            Column column = filterItemFieldName2ColumnMap.get("inspect_status");
            ExpressionList values = new ExpressionList();
            for (String inspectStatus : queryCriteriaVo.getInspectStatusList()) {
                values.addExpressions(new StringValue(inspectStatus));
            }
            InExpression inExpression = new InExpression(column, values);
            Expression where = plainSelect.getWhere();
            if (where != null) {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            } else {
                plainSelect.setWhere(inExpression);
            }
        }
        return plainSelect;
    }

    private SqlVo getSqlVo(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> filterItemFieldName2ColumnMap) {
        SqlVo sqlVo = new SqlVo();
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
            System.out.println("a");
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("ffc.target_id", "=", filterItemFieldName2ColumnMap.get("id").toString()),
                        "and",
                        $sql.exp("ffc.target_field", "in", Arrays.asList(queryCriteriaVo.getNameFieldAttrId(), queryCriteriaVo.getIpFieldAttrId())));
                joinList.add($sql.join("join", "fulltextindex_field_cmdb", "ffc").withOn(expressionVo));
            }
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("fw.id", "=", "ffc.word_id"),
                        "and",
                        $sql.exp("fw.word", "in", queryCriteriaVo.getKeywordList()));
                joinList.add($sql.join("join", "fulltextindex_word", "fw").withOn(expressionVo));
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
                    System.out.println("b");
                    fieldAttrId = queryCriteriaVo.getNameFieldAttrId();
                } else {
                    System.out.println("c");
                    fieldAttrId = queryCriteriaVo.getIpFieldAttrId();
                }
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("ffc2.target_id", "=", filterItemFieldName2ColumnMap.get("id").toString()),
                        "and",
                        $sql.exp("ffc2.target_field", "=", fieldAttrId));
                joinList.add($sql.join("join", "fulltextindex_field_cmdb", "ffc2").withOn(expressionVo));
            }
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("fw2.id", "=", "ffc2.word_id"),
                        "and",
                        $sql.exp("fw2.word", "in", queryCriteriaVo.getBatchSearchList()));
                joinList.add($sql.join("join", "fulltextindex_word", "fw2").withOn(expressionVo));
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
            System.out.println("d");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
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
            System.out.println("e");
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_tag", "d").withOn($sql.exp("d.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
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
            System.out.println("f");
            joinList.add($sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
            ExpressionVo expressionVo = $sql.exp($sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"), "and", $sql.exp("ajpn.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString()));
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
            joinList.add($sql.join("left join", "cmdb_cientity_group", "ccg").withOn($sql.exp("ccg.cientity_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_group_auth", "cga").withOn($sql.exp("cga.group_id", "=", "ccg.group_id")));

            List<String> strList = new ArrayList<>();
            if (Objects.equals(queryCriteriaVo.getCmdbGroupType(), "autoexec")) {
                System.out.println("g");
                strList.add("autoexec");
            } else {
                System.out.println("h");
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
                System.out.println("i");
                whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
            } else if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
                ExpressionVo orLeftExpressionVo = null;
                if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthedTypeIdList())) {
                    System.out.println("j");
                    orLeftExpressionVo = $sql.exp(filterItemFieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getAuthedTypeIdList());
                } else {
                    System.out.println("k");
                    orLeftExpressionVo = $sql.exp(1, "=", 0);
                }
                ExpressionVo orRightExpressionVo = $sql.exp($sql.exp("cg.id", "is not null"), "and", $sql.exp(filterItemFieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));

                ExpressionVo orLeftExpressionVo2 = $sql.exp("(", $sql.exp("cga.auth_type", "=", "'common'"), "and", $sql.exp("cga.auth_uuid", "=", "'alluser'"), ")");
                ExpressionVo orRightExpressionVo2 = null;
                if (queryCriteriaVo.getAuthenticationInfo() != null) {
                    System.out.println("l");
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
                        System.out.println("m");
                        orRightExpressionVo2 = $sql.exp("cga.auth_uuid", "in", uuidList);
                    }
                }
                if (orRightExpressionVo2 != null) {
                    System.out.println("n");
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", $sql.exp("(", orLeftExpressionVo2, "or", orRightExpressionVo2, ")"));
                } else {
                    System.out.println("o");
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
            System.out.println("p");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
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
            System.out.println("q");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
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
            System.out.println("r");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="isExistNoEnv">
            AND a.`env_id` is null
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getExistNoEnv(), true)) {
            System.out.println("s");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("env_id").toString(), "is null"));
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
            System.out.println("t");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
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
            System.out.println("u");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
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
            System.out.println("v");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("id").toString(), "in", queryCriteriaVo.getDefaultValue()));
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
            System.out.println("w");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("id").toString(), "in", queryCriteriaVo.getIdList()));
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
            System.out.println("x");
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("inspect_status").toString(), "in", queryCriteriaVo.getInspectStatusList()));
        }
        sqlVo.withJoinList(joinList);
        sqlVo.withWhereExpressionList(whereExpressionList);
        return sqlVo;
    }

    private List<JoinVo> getJoinList(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> filterItemFieldName2ColumnMap) {
        List<JoinVo> joinList = new ArrayList<>();
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
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("ffc.target_id", "=", filterItemFieldName2ColumnMap.get("id").toString()),
                        "and",
                        $sql.exp("ffc.target_field", "in", Arrays.asList(queryCriteriaVo.getNameFieldAttrId(), queryCriteriaVo.getIpFieldAttrId())));
                joinList.add($sql.join("join", "fulltextindex_field_cmdb", "ffc").withOn(expressionVo));
            }
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("fw.id", "=", "ffc.word_id"),
                        "and",
                        $sql.exp("fw.word", "in", queryCriteriaVo.getKeywordList()));
                joinList.add($sql.join("join", "fulltextindex_word", "fw").withOn(expressionVo));
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
                    fieldAttrId = queryCriteriaVo.getNameFieldAttrId();
                } else {
                    fieldAttrId = queryCriteriaVo.getIpFieldAttrId();
                }
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("ffc2.target_id", "=", filterItemFieldName2ColumnMap.get("id").toString()),
                        "and",
                        $sql.exp("ffc2.target_field", "=", fieldAttrId));
                joinList.add($sql.join("join", "fulltextindex_field_cmdb", "ffc2").withOn(expressionVo));
            }
            {
                ExpressionVo expressionVo = $sql.exp(
                        $sql.exp("fw2.id", "=", "ffc2.word_id"),
                        "and",
                        $sql.exp("fw2.word", "in", queryCriteriaVo.getBatchSearchList()));
                joinList.add($sql.join("join", "fulltextindex_word", "fw2").withOn(expressionVo));
            }
        }
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_account` b ON b.`resource_id` = a.`id`
            LEFT JOIN `cmdb_resourcecenter_account` c ON c.`id` = b.`account_id`
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_account", "b").withOn($sql.exp("b.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_resourcecenter_account", "c").withOn($sql.exp("c.id", "=", "b.account_id")));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            LEFT JOIN `cmdb_resourcecenter_resource_tag` d ON d.`resource_id` = a.`id`
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
            joinList.add($sql.join("left join", "cmdb_resourcecenter_resource_tag", "d").withOn($sql.exp("d.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
        }
        /*
        <if test="inspectJobPhaseNodeStatusList !=null and inspectJobPhaseNodeStatusList.size() > 0">
            left join autoexec_job_resource_inspect ajri on ajri.resource_id=a.id
            left join autoexec_job_phase_node ajpn on ajpn.job_phase_id =ajri.phase_id AND ajpn.resource_id = a.id
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
            joinList.add($sql.join("left join", "autoexec_job_resource_inspect", "ajri").withOn($sql.exp("ajri.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
            ExpressionVo expressionVo = $sql.exp($sql.exp("ajpn.job_phase_id", "=", "ajri.phase_id"), "and", $sql.exp("ajpn.resource_id", "=", filterItemFieldName2ColumnMap.get("id").toString()));
            joinList.add($sql.join("left join", "autoexec_job_phase_node", "ajpn").withOn(expressionVo));
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
            joinList.add($sql.join("left join", "cmdb_cientity_group", "ccg").withOn($sql.exp("ccg.cientity_id", "=", filterItemFieldName2ColumnMap.get("id").toString())));
            joinList.add($sql.join("left join", "cmdb_group_auth", "cga").withOn($sql.exp("cga.group_id", "=", "ccg.group_id")));

            List<String> strList = new ArrayList<>();
            if (Objects.equals(queryCriteriaVo.getCmdbGroupType(), "autoexec")) {
                strList.add("autoexec");
            } else {
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
        return joinList;
    }

    private List<ExpressionVo> getWhereExpressionList(ResourceQueryCriteriaVo queryCriteriaVo, Map<String, Column> filterItemFieldName2ColumnMap) {
        List<ExpressionVo> whereExpressionList = new ArrayList<>();
        /*
        <if test="protocolIdList != null and protocolIdList.size() > 0">
            AND c.`protocol_id` IN
            <foreach collection="protocolIdList" item="protocolId" open="(" separator="," close=")">
                #{protocolId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getProtocolIdList())) {
            whereExpressionList.add($sql.exp("c.protocol_id", "in", queryCriteriaVo.getProtocolIdList()));
        }
        /*
        <if test="tagIdList != null and tagIdList.size() > 0">
            AND d.`tag_id` IN
            <foreach collection="tagIdList" item="tagId" open="(" separator="," close=")">
                #{tagId}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getTagIdList())) {
            whereExpressionList.add($sql.exp("d.tag_id", "in", queryCriteriaVo.getTagIdList()));
        }
        /*
        <if test="inspectStatusList != null and inspectStatusList.size() > 0">
            AND a.`inspect_status` IN
            <foreach collection="inspectStatusList" item="inspectStatus" open="(" separator="," close=")">
                #{inspectStatus}
            </foreach>
        </if>
         */
        if (CollectionUtils.isNotEmpty(queryCriteriaVo.getInspectJobPhaseNodeStatusList())) {
            whereExpressionList.add($sql.exp("ajpn.status", "in", queryCriteriaVo.getInspectJobPhaseNodeStatusList()));
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
                whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));
            } else if (Objects.equals(queryCriteriaVo.getIsHasAuth(), false)) {
                ExpressionVo orLeftExpressionVo = null;
                if (CollectionUtils.isNotEmpty(queryCriteriaVo.getAuthedTypeIdList())) {
                    orLeftExpressionVo = $sql.exp(filterItemFieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getAuthedTypeIdList());
                } else {
                    orLeftExpressionVo = $sql.exp(1, "=", 0);
                }
                ExpressionVo orRightExpressionVo = $sql.exp($sql.exp("cg.id", "is not null"), "and", $sql.exp(filterItemFieldName2ColumnMap.get("type_id").toString(), "in", queryCriteriaVo.getTypeIdList()));

                ExpressionVo orLeftExpressionVo2 = $sql.exp("(", $sql.exp("cga.auth_type", "=", "'common'"), "and", $sql.exp("cga.auth_uuid", "=", "'alluser'"), ")");
                ExpressionVo orRightExpressionVo2 = null;
                if (queryCriteriaVo.getAuthenticationInfo() != null) {
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
                        orRightExpressionVo2 = $sql.exp("cga.auth_uuid", "in", uuidList);
                    }
                }
                if (orRightExpressionVo2 != null) {
                    orRightExpressionVo = $sql.exp(orRightExpressionVo, "and", $sql.exp("(", orLeftExpressionVo2, "or", orRightExpressionVo2, ")"));
                } else {
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("state_id").toString(), "in", queryCriteriaVo.getStateIdList()));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("vendor_id").toString(), "in", queryCriteriaVo.getVendorIdList()));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("env_id").toString(), "in", queryCriteriaVo.getEnvIdList()));
        }
        /*
        <if test="isExistNoEnv">
            AND a.`env_id` is null
        </if>
         */
        if (Objects.equals(queryCriteriaVo.getExistNoEnv(), true)) {
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("env_id").toString(), "is null"));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("app_system_id").toString(), "in", queryCriteriaVo.getAppSystemIdList()));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("app_module_id").toString(), "in", queryCriteriaVo.getAppModuleIdList()));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("id").toString(), "in", queryCriteriaVo.getDefaultValue()));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("id").toString(), "in", queryCriteriaVo.getIdList()));
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
            whereExpressionList.add($sql.exp(filterItemFieldName2ColumnMap.get("inspect_status").toString(), "in", queryCriteriaVo.getInspectStatusList()));
        }
        return whereExpressionList;
    }
}
