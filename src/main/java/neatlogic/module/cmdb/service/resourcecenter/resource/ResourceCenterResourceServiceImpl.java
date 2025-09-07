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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityFieldMappingVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.CmdbTenantConfig;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.fulltextindex.utils.FullTextIndexUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
public class ResourceCenterResourceServiceImpl implements IResourceCenterResourceService, IResourceCenterResourceCrossoverService {

    private final Logger logger = LoggerFactory.getLogger(ResourceCenterResourceServiceImpl.class);
    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    private final String MYBATIS_MODE = "mybatis";
    private final String JSQLPARSER_MODE = "jsqlparser";
    private final String COMPARISON_ENABLED = "1";
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

    @Resource
    private ResourceBuildSqlService resourceBuildSqlService;

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
    public int getResourceCount(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        int oldRowNum = 0;
        int newRowNum = 0;
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = resourceBuildSqlService.buildGetResourceCountSql(searchVo);
            newRowNum = resourceMapper.getCountBySql(sql);
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldRowNum = resourceMapper.getResourceCount(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            if (oldRowNum != newRowNum) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("newRowNum", newRowNum);
                errorObj.put("oldRowNum", oldRowNum);
                logger.error("资产清单新旧SQL获取rowNum结果不一致：{}", errorObj);
            }
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newRowNum;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldRowNum;
        }
        return 0;
    }

    @Override
    public List<Long> getResourceIdList(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<Long> newIdList = new ArrayList<>();
        List<Long> oldIdList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = resourceBuildSqlService.buildGetResourceIdListSql(searchVo);
            newIdList = resourceMapper.getIdListBySql(sql);
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldIdList = resourceMapper.getResourceIdList(searchVo);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            if (!Objects.equals(oldIdList, newIdList)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("newIdList", newIdList);
                errorObj.put("oldIdList", oldIdList);
                logger.error("资产清单新旧SQL获取idList结果不一致：{}", errorObj);
            }
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newIdList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldIdList;
        }
        return new ArrayList<>();
    }

    @Override
    public List<ResourceVo> getResourceListByIdList(List<Long> idList) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
        List<ResourceVo> newResourceList = new ArrayList<>();
        List<ResourceVo> oldResourceList = new ArrayList<>();
        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            String sql = resourceBuildSqlService.buildGetResourceListSql(idList);
            newResourceList = resourceMapper.getResourceListBySql(sql);
        }
        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
            oldResourceList = resourceMapper.getResourceListByIdList(idList);
        }
        if (Objects.equals(enable, COMPARISON_ENABLED)) {
            checkResourceListIsEquals(newResourceList, oldResourceList);
        }
        if (Objects.equals(mode, JSQLPARSER_MODE)) {
            return newResourceList;
        } else if (Objects.equals(mode, MYBATIS_MODE)) {
            return oldResourceList;
        }
        return new ArrayList<>();
    }

//    public Object example() {
//        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
//        String mode = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_SQL_MODE);
//        if (Objects.equals(mode, JSQLPARSER_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
//
//        }
//        if (Objects.equals(mode, MYBATIS_MODE) || Objects.equals(enable, COMPARISON_ENABLED)) {
//
//        }
//        if (Objects.equals(enable, COMPARISON_ENABLED)) {
//
//        }
//        if (Objects.equals(mode, JSQLPARSER_MODE)) {
//
//        } else if (Objects.equals(mode, MYBATIS_MODE)) {
//
//        }
//        return null;
//    }

    private boolean checkResourceListIsEquals(List<ResourceVo> resourceList, List<ResourceVo> oldResourceList) {
        if (oldResourceList.size() != resourceList.size()) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("resourceList.size()", resourceList.size());
            errorObj.put("oldResourceList.size()", oldResourceList.size());
            logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
            return false;
        }
        boolean flag = true;
        resourceList.sort(Comparator.comparing(ResourceVo::getId));
        oldResourceList.sort(Comparator.comparing(ResourceVo::getId));
        for (int i = 0; i < resourceList.size(); i++) {
            ResourceVo resourceVo = resourceList.get(i);
            ResourceVo oldResourceVo = oldResourceList.get(i);
            String resourceString = JSONObject.toJSONString(resourceVo);
            String oldResourceString = JSONObject.toJSONString(oldResourceVo);
            if (!Objects.equals(resourceString, oldResourceString)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("index", i);
                errorObj.put("resourceVo", resourceVo);
                errorObj.put("oldResourceVo", oldResourceVo);
                logger.error("资产清单新旧SQL获取tbodyList结果不一致：{}", errorObj);
                flag = false;
            }
        }
        return flag;
    }

    private boolean noFilterCondition(ResourceSearchVo searchVo) {
        if (StringUtils.isNotBlank(searchVo.getKeyword())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getBatchSearchList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getStateIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getVendorIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getEnvIdList())) {
            return false;
        }
        if (searchVo.getExistNoEnv()) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getAppSystemIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getAppModuleIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getDefaultValue())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getInspectStatusList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getProtocolIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getInspectJobPhaseNodeStatusList())) {
            return false;
        }
        return true;
    }
}
