/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.service.resourcecenter.resource;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityFieldMappingVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AppModuleResourceType;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.cmdb.enums.resourcecenter.ViewType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceViewFieldMappingException;
import neatlogic.framework.cmdb.utils.ResourceViewGenerateSqlUtil;
import neatlogic.framework.dao.mapper.DataBaseViewInfoMapper;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.dto.DataBaseViewInfoVo;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.Md5Util;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import neatlogic.module.cmdb.utils.ResourceEntityViewBuilder;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
@Service
public class ResourceCenterResourceServiceImpl implements IResourceCenterResourceService {
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
    private CiEntityMapper ciEntityMapper;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private SchemaMapper schemaMapper;

    @Resource
    private DataBaseViewInfoMapper dataBaseViewInfoMapper;

    public static final Map<String, Action<ResourceSearchVo>> searchMap = new HashMap<>();

    @FunctionalInterface
    public interface Action<T> {
        List<ResourceVo> execute(T t);
    }

    @Override
    public ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj) {
        ResourceSearchVo searchVo = jsonObj.toJavaObject(ResourceSearchVo.class);
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
            searchVo.setTypeIdList(ciIdList);
        } else {
            List<Long> typeIdList = searchVo.getTypeIdList();
            if (CollectionUtils.isNotEmpty(typeIdList)) {
                Set<Long> ciIdSet = new HashSet<>();
                for (Long ciId : typeIdList) {
                    CiVo ciVo = ciMapper.getCiById(ciId);
                    if (ciVo == null) {
                        throw new CiNotFoundException(ciId);
                    }
                    List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                    ciIdSet.addAll(ciIdList);
                }
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
        return searchVo;
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

    @Override
    public void addResourceAccount(List<Long> idList, List<ResourceVo> resourceVoList) {
        Map<Long, List<AccountVo>> resourceAccountVoMap = new HashMap<>();
        List<ResourceAccountVo> resourceAccountVoList = resourceAccountMapper.getResourceAccountListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            Set<Long> accountIdSet = resourceAccountVoList.stream().map(ResourceAccountVo::getAccountId).collect(Collectors.toSet());
            List<AccountVo> accountList = resourceAccountMapper.getAccountListByIdList(new ArrayList<>(accountIdSet));
            Map<Long, AccountVo> accountMap = accountList.stream().collect(Collectors.toMap(AccountVo::getId, e -> e));
            for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
                resourceAccountVoMap.computeIfAbsent(resourceAccountVo.getResourceId(), k -> new ArrayList<>()).add(accountMap.get(resourceAccountVo.getAccountId()));
            }
        }
        for (ResourceVo resourceVo : resourceVoList) {
            List<AccountVo> accountVoList = resourceAccountVoMap.get(resourceVo.getId());
            if (CollectionUtils.isNotEmpty(accountVoList)) {
                resourceVo.setAccountList(accountVoList);
            }
        }
    }

    @Override
    public void addResourceTag(List<Long> idList, List<ResourceVo> resourceVoList) {
        Map<Long, List<TagVo>> resourceTagVoMap = new HashMap<>();
        List<ResourceTagVo> resourceTagVoList = resourceTagMapper.getResourceTagListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            Set<Long> tagIdSet = resourceTagVoList.stream().map(ResourceTagVo::getTagId).collect(Collectors.toSet());
            List<TagVo> tagList = resourceTagMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
            Map<Long, TagVo> tagMap = tagList.stream().collect(Collectors.toMap(TagVo::getId, e -> e));
            for (ResourceTagVo resourceTagVo : resourceTagVoList) {
                resourceTagVoMap.computeIfAbsent(resourceTagVo.getResourceId(), k -> new ArrayList<>()).add(tagMap.get(resourceTagVo.getTagId()));
            }
        }

        for (ResourceVo resourceVo : resourceVoList) {
            List<TagVo> tagVoList = resourceTagVoMap.get(resourceVo.getId());
            if (CollectionUtils.isNotEmpty(tagVoList)) {
                resourceVo.setTagList(tagVoList.stream().map(TagVo::getName).collect(Collectors.toList()));
            }
        }
    }

    /**
     * 获取对应模块的应用清单列表
     * 其中清单列表有 系统 应用实例 应用实例集群 DB实例 DB实例集群 访问入口
     *
     * @param searchVo
     * @return
     */
    @Override
    public JSONArray getAppModuleResourceList(ResourceSearchVo searchVo) {
        JSONArray tableList = new JSONArray();
        List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(AppModuleResourceType.getNameList());
        List<Long> resourceTypeIdList = new ArrayList<>();
        Long appSystemId = searchVo.getAppSystemId();
        Long appModuleId = searchVo.getAppModuleId();
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            resourceTypeIdList.add(typeId);
        } else if (appModuleId != null) {
            CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(appModuleId);
            if (ciEntityVo == null) {
                throw new AppModuleNotFoundException(appModuleId);
            }
            Set<Long> resourceTypeIdSet = resourceMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
            resourceTypeIdList.addAll(resourceTypeIdSet);
            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                resourceTypeIdSet = resourceMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                resourceTypeIdList.addAll(resourceTypeIdSet);
            }
        } else if (appSystemId != null) {
            CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(appSystemId);
            if (ciEntityVo == null) {
                throw new AppSystemNotFoundException(appSystemId);
            }
            Set<Long> resourceTypeIdSet = resourceMapper.getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
            resourceTypeIdList.addAll(resourceTypeIdSet);
            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                resourceTypeIdSet = resourceMapper.getOsResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
                resourceTypeIdList.addAll(resourceTypeIdSet);
            }
        }

        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
            List<CiVo> ciList = ciMapper.getAllCi(resourceTypeIdList);
            for (CiVo ciVo : ciList) {
                ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName());
                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                if (StringUtils.isBlank(resourceTypeName)) {
                    continue;
                }
                String actionKey = AppModuleResourceType.getAction(resourceTypeName);
                if (StringUtils.isBlank(actionKey)) {
                    continue;
                }
                searchVo.setTypeId(ciVo.getId());
                List<ResourceVo> returnList = searchMap.get(actionKey).execute(searchVo);
                if (CollectionUtils.isNotEmpty(returnList)) {
                    JSONObject tableObj = TableResultUtil.getResult(returnList, searchVo);
                    tableObj.put("type", resourceTypeVo);
                    tableList.add(tableObj);
                }
            }
        }
        return tableList;
    }

    @PostConstruct
    public void searchDispatcherInit() {
        searchMap.put("ipObject", (searchVo) -> {
            int rowNum = resourceMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceMapper.getResourceListByIdList(idList);
                }
            }
            return new ArrayList<>();
        });

        searchMap.put("OS", (searchVo) -> {
            int rowNum = resourceMapper.getOsResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceMapper.getOsResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceMapper.getOsResourceListByIdList(idList);
                }
            }
            return new ArrayList<>();
        });

//        searchMap.put("StorageDevice", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getStorageResourceCount(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getStorageResourceIdList(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getStorageResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });
//
//        searchMap.put("NetworkDevice", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getNetDevResourceCount(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getNetDevResourceIdList(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getNetDevResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });

        searchMap.put("APPIns", (searchVo) -> {
            int rowNum = resourceMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceMapper.getAppInstanceResourceListByIdList(idList);
                }
            }
            return new ArrayList<>();
        });

        searchMap.put("DBIns", (searchVo) -> {
            int rowNum = resourceMapper.getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceMapper.getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceMapper.getDbInstanceResourceListByIdList(idList);
                }
            }
            return new ArrayList<>();
        });

    }

    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo) {
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }

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
    public List<ResourceVo> getAppModuleList(ResourceSearchVo searchVo) {
        int count = resourceMapper.searchAppModuleCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> idList = resourceMapper.searchAppModuleIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceMapper.searchAppModule(idList);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public Collection<AppEnvVo> getAppEnvList(ResourceSearchVo searchVo) {
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(searchVo.getAppSystemId());
        if (ciEntityVo == null) {
            throw new AppSystemNotFoundException(searchVo.getAppSystemId());
        }
        Set<Long> resourceTypeIdSet = resourceMapper.getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
        List<Long> resourceTypeIdList = new ArrayList<>(resourceTypeIdSet);
        if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
            resourceTypeIdSet = resourceMapper.getOsResourceTypeIdListByAppSystemIdAndEnvId(searchVo);
            resourceTypeIdList.addAll(resourceTypeIdSet);
        }

        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
            Map<Long, AppEnvVo> returnEnvMap = new HashMap<>();
            Map<Long, Set<Long>> envIdModuleIdSetMap = new HashMap<>();
            Map<Long, List<AppModuleVo>> envIdModuleListMap = new HashMap<>();
            Map<Long, Set<Long>> envModuleIdCiIdSetMap = new HashMap<>();
            Map<Long, List<CiVo>> envModuleIdCiListMap = new HashMap<>();
            List<CiVo> ciList = ciMapper.getAllCi(resourceTypeIdList);
            List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(AppModuleResourceType.getNameList());

            for (CiVo ciVo : ciList) {
                List<AppEnvVo> appEnvList = new ArrayList<>();
                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                if (StringUtils.isBlank(resourceTypeName)) {
                    continue;
                }
                String actionKey = AppModuleResourceType.getAction(resourceTypeName);
                if (StringUtils.isBlank(actionKey)) {
                    continue;
                }
                searchVo.setTypeId(ciVo.getId());
                if (actionKey.equals("OS")) {
                    appEnvList.addAll(resourceMapper.getOsEnvListByAppSystemIdAndTypeId(searchVo));
                } else {
                    appEnvList.addAll(resourceMapper.getIpObjectEnvListByAppSystemIdAndTypeId(searchVo));
                }

                /*数据处理
                1、returnEnvMap           环境List
                2、envIdModuleIdSetMap    环境id对应的 模块id列表
                3、envModuleIdCiIdSetMap  环境id+模块id对应的 模型id列表
                 */
                if (CollectionUtils.isNotEmpty(appEnvList)) {
                    for (AppEnvVo envVo : appEnvList) {
                        //未配置情况，环境id设为-2，因为id为-1的一般都是代表 所有 的意思
                        if (envVo.getId() == null) {
                            envVo.setId(-2L);
                            envVo.setName("未配置");
                            envVo.setSeqNo(9999);
                        }
                        Long envId = envVo.getId();
                        returnEnvMap.put(envId, envVo);
                        List<AppModuleVo> appModuleList = envVo.getAppModuleList();
                        Set<Long> appModuleIdSet = appModuleList.stream().map(AppModuleVo::getId).collect(Collectors.toSet());
                        if (envIdModuleIdSetMap.containsKey(envId)) {
                            for (AppModuleVo moduleVo : appModuleList) {
                                if (envIdModuleIdSetMap.get(envId).contains(moduleVo.getId())) {
                                    List<CiVo> ciVoList = moduleVo.getCiList();
                                    for (CiVo ci : ciVoList) {
                                        if (CollectionUtils.isNotEmpty(envModuleIdCiIdSetMap.get(envId + moduleVo.getId())) && !envModuleIdCiIdSetMap.get(envId + moduleVo.getId()).contains(ci.getId())) {
                                            envModuleIdCiIdSetMap.get(envId + moduleVo.getId()).add(ci.getId());
                                            envModuleIdCiListMap.get(envId + moduleVo.getId()).add(ci);
                                        }
                                    }
                                } else {
                                    envIdModuleIdSetMap.get(envId).add(moduleVo.getId());
                                    envIdModuleListMap.get(envId).add(moduleVo);
                                    envModuleIdCiIdSetMap.put(envId + moduleVo.getId(), moduleVo.getCiList().stream().map(CiVo::getId).collect(Collectors.toSet()));
                                    envModuleIdCiListMap.put(envId + moduleVo.getId(), moduleVo.getCiList());
                                }
                            }
                            envIdModuleIdSetMap.get(envId).addAll(appModuleIdSet);
                        } else {
                            envIdModuleIdSetMap.put(envId, appModuleIdSet);
                            envIdModuleListMap.put(envId, appModuleList);
                            for (AppModuleVo moduleVo : appModuleList) {
                                envModuleIdCiIdSetMap.put(envId + moduleVo.getId(), moduleVo.getCiList().stream().map(CiVo::getId).collect(Collectors.toSet()));
                                envModuleIdCiListMap.put(envId + moduleVo.getId(), moduleVo.getCiList());
                            }
                        }
                    }
                }
            }
            for (Map.Entry<Long, AppEnvVo> entry : returnEnvMap.entrySet()) {
                List<AppModuleVo> appModuleVoList = envIdModuleListMap.get(entry.getKey());
                for (AppModuleVo appModuleVo : appModuleVoList) {
                    List<CiVo> ciVoList = envModuleIdCiListMap.get(entry.getKey() + appModuleVo.getId());
                    appModuleVo.setCiList(ciVoList);
                }
                entry.getValue().setAppModuleList(appModuleVoList);
            }
            return returnEnvMap.values().stream().sorted(Comparator.comparing(AppEnvVo::getSeqNo)).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<ResourceEntityVo> rebuildResourceEntity() {
        List<String> viewNameList = ResourceEntityFactory.getViewNameList();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityListByNameList(viewNameList);
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
//            resourceEntityVo.setError("");
//            resourceEntityVo.setStatus(Status.PENDING.getValue());
//            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
//            String xml = resourceEntityMapper.getResourceEntityXmlByName(resourceEntityVo.getName());
//            if (StringUtils.isNotBlank(xml)) {
//                resourceEntityVo.setXml(xml);
//                ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(resourceEntityVo);
//                builder.buildView();
//            }
            String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
            resourceEntityVo.setConfigStr(config);
            String error = buildResourceView(resourceEntityVo.getName(), resourceEntityVo.getConfig());
            resourceEntityVo.setError(error);
            if (StringUtils.isNotBlank(error)) {
                resourceEntityVo.setStatus(Status.ERROR.getValue());
            } else {
                resourceEntityVo.setStatus(Status.READY.getValue());
            }
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
        }
        List<SceneEntityVo> sceneEntityList = ResourceEntityFactory.getSceneEntityList();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            String viewName = sceneEntityVo.getName();
            String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
            if (tableType == null) {
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
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                    schemaMapper.insertView(createTable.toString());
                }).execute();
            }
        }
        resourceEntityList = resourceEntityMapper.getResourceEntityListByNameList(viewNameList);
        Map<String, ResourceEntityVo> resourceEntityVoMap = resourceEntityList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
        List<ResourceEntityVo> resultList = new ArrayList<>();
        for (SceneEntityVo sceneEntityVo : sceneEntityList) {
            ResourceEntityVo resourceEntityVo = resourceEntityVoMap.get(sceneEntityVo.getName());
            if (resourceEntityVo == null) {
                resourceEntityVo = new ResourceEntityVo();
                resourceEntityVo.setName(sceneEntityVo.getName());
                resourceEntityVo.setLabel(sceneEntityVo.getLabel());
                resourceEntityVo.setStatus(Status.PENDING.getValue());
                resourceEntityVo.setType(ViewType.SCENE.getValue());
            }
            resultList.add(resourceEntityVo);
        }
        return resultList;
    }

    @Override
    public String buildResourceView(String viewName, ResourceEntityConfigVo originalConfig) {
        String error = StringUtils.EMPTY;
        ResourceEntityConfigVo config = null;
        try {
            config = fieldMappingCheckValidityAndFillDefaultData(viewName, originalConfig);
        } catch (ResourceViewFieldMappingException e) {
            return e.getMessage();
        }
        ResourceViewGenerateSqlUtil resourceViewGenerateSqlUtil = new ResourceViewGenerateSqlUtil(config);
        String selectSql = resourceViewGenerateSqlUtil.getSql();
        String md5 = Md5Util.encryptMD5(selectSql);
        String tableType = schemaMapper.checkTableOrViewIsExists(TenantContext.get().getDataDbName(), viewName);
        if (tableType != null) {
            if (Objects.equals(tableType, "SYSTEM VIEW")) {
                return StringUtils.EMPTY;
            } else if (Objects.equals(tableType, "VIEW")) {
                DataBaseViewInfoVo dataBaseViewInfoVo = dataBaseViewInfoMapper.getDataBaseViewInfoByViewName(viewName);
                if (dataBaseViewInfoVo != null) {
                    // md5相同就不用更新视图了
                    if (Objects.equals(md5, dataBaseViewInfoVo.getMd5())) {
                        return StringUtils.EMPTY;
                    }
                }
            }
        }
        try {
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
                return s.getError();
            }
        } catch (Exception ex) {
            error = ex.getMessage();
            if (Objects.equals(tableType, "VIEW")) {
                schemaMapper.deleteView(TenantContext.get().getDataDbName() + "." + viewName);
            }
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
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                schemaMapper.insertView(createTable.toString());
            }).execute();
        } finally {
            return error;
//            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
        }
    }
    private final static List<String> defaultAttrList = Arrays.asList("_id", "_uuid", "_name", "_fcu", "_fcd", "_lcu", "_lcd", "_inspectStatus", "_inspectTime", "_monitorStatus", "_monitorTime", "_typeId", "_typeName", "_typeLabel");

    @Override
    public ResourceEntityConfigVo fieldMappingCheckValidityAndFillDefaultData(String viewName, ResourceEntityConfigVo config) {
        ResourceEntityConfigVo newConfig = new ResourceEntityConfigVo();
        String mainCi = config.getMainCi();
        if (StringUtils.isBlank(mainCi)) {
            throw new ResourceViewFieldMappingException(viewName);
        }
        List<String> fieldNameList = ResourceEntityFactory.getFieldNameListByViewName(viewName);
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
                AttrVo fromAttrVo = attrMapper.getAttrByCiIdAndName(fromCiVo.getId(), fromAttr);
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
                        AttrVo toAttrVo = attrMapper.getAttrByCiIdAndName(toCiVo.getId(), toAttr);
                        if (toAttrVo == null) {
                            throw new ResourceViewFieldMappingException(viewName, field, "toAttr", toAttr);
                        }
                        newFieldMappingVo.setToAttrId(toAttrVo.getId());
                        if(Objects.equals(toCiVo.getIsVirtual(), 0)) {
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
                        AttrVo fromAttrVo = attrMapper.getAttrByCiIdAndName(fromCiVo.getId(), fromAttr);
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
                            AttrVo toAttrVo = attrMapper.getAttrByCiIdAndName(toCiVo.getId(), toAttr);
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
            } else {
                throw new ResourceViewFieldMappingException(viewName, field, "type", type);
            }
            resultList.add(newFieldMappingVo);
        }
        if (CollectionUtils.isNotEmpty(fieldNameList)) {
            throw new ResourceViewFieldMappingException(viewName, fieldNameList);
        }
        newConfig.setFieldMappingList(resultList);
        return newConfig;
    }
}
