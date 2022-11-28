/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.enums.resourcecenter.AppModuleResourceType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
    private CiEntityMapper ciEntityMapper;

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
                    return resourceMapper.getAppInstanceResourceListByIdList(idList);
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
}