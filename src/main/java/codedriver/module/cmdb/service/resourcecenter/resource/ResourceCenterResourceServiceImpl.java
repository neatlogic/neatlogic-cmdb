/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSON;
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
    ResourceCenterMapper resourceCenterMapper;

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
        ResourceSearchVo searchVo = JSON.toJavaObject(jsonObj, ResourceSearchVo.class);
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
        List<Long> resourceIdList = null;
        if (CollectionUtils.isNotEmpty(searchVo.getProtocolIdList())) {
            List<Long> idList = resourceCenterMapper.getResourceIdListByProtocolIdList(searchVo);
            if (resourceIdList == null) {
                resourceIdList = idList;
            } else {
                resourceIdList.retainAll(idList);
            }
        }
        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
            List<Long> idList = resourceCenterMapper.getResourceIdListByTagIdList(searchVo);
            if (resourceIdList == null) {
                resourceIdList = idList;
            } else {
                resourceIdList.retainAll(idList);
            }
        }
        searchVo.setIdList(resourceIdList);
        return searchVo;
    }

    @Override
    public void addResourceAccount(List<Long> idList, List<ResourceVo> resourceVoList) {
        Map<Long, List<AccountVo>> resourceAccountVoMap = new HashMap<>();
        List<ResourceAccountVo> resourceAccountVoList = resourceCenterMapper.getResourceAccountListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            Set<Long> accountIdSet = resourceAccountVoList.stream().map(ResourceAccountVo::getAccountId).collect(Collectors.toSet());
            List<AccountVo> accountList = resourceCenterMapper.getAccountListByIdList(new ArrayList<>(accountIdSet));
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
        List<ResourceTagVo> resourceTagVoList = resourceCenterMapper.getResourceTagListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            Set<Long> tagIdSet = resourceTagVoList.stream().map(ResourceTagVo::getTagId).collect(Collectors.toSet());
            List<TagVo> tagList = resourceCenterMapper.getTagListByIdList(new ArrayList<>(tagIdSet));
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
     * 其中清单列表有 系统 存储设备 网络设备 应用实例 应用实例集群 DB实例 DB实例集群 访问入口
     *
     * @param searchVo
     * @return
     */
    @Override
    public JSONArray getAppModuleResourceList(ResourceSearchVo searchVo) {
        JSONArray tableList = new JSONArray();
        String schemaName = TenantContext.get().getDataDbName();
        Long appModuleId = searchVo.getAppModuleId();
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(appModuleId);
        if (ciEntityVo == null) {
            throw new AppModuleNotFoundException(appModuleId);
        }
//        if (resourceCenterMapper.checkAppModuleIsExists(appModuleId, schemaName) == 0) {
//            throw new AppModuleNotFoundException(appModuleId);
//        }
        //20220302跟产品确认应用清单的模块中不需要出现“网络设备”和“存储设备StorageDevice”
        List<String> resourceTypeNameList = Arrays.asList("OS", "StorageDevice", "NetworkDevice", "APPIns", "APPInsCluster", "DBIns", "DBCluster", "AccessEndPoint", "Database");
        Map<String, String> typeNameActionMap = new HashMap<>();
        typeNameActionMap.put("OS", "OS");
        typeNameActionMap.put("APPIns", "APPIns");
        typeNameActionMap.put("APPInsCluster", "ipObject");
        typeNameActionMap.put("DBIns", "DBIns");
        typeNameActionMap.put("DBCluster", "ipObject");
        typeNameActionMap.put("AccessEndPoint", "ipObject");
        typeNameActionMap.put("Database", "ipObject");
        List<CiVo> resourceCiVoList = new ArrayList<>();
        Map<Long, CiVo> ciVoMap = new HashMap<>();
        List<CiVo> ciVoList = ciMapper.getAllCi(null);
        for (CiVo ciVo : ciVoList) {
            ciVoMap.put(ciVo.getId(), ciVo);
            if (resourceTypeNameList.contains(ciVo.getName())) {
                resourceCiVoList.add(ciVo);
            }
        }
        List<Long> resourceTypeIdList = new ArrayList<>();
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciVoMap.get(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            resourceTypeIdList.add(typeId);
        } else {
            Set<Long> resourceTypeIdSet = resourceCenterMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
            resourceTypeIdList.addAll(resourceTypeIdSet);
            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                resourceTypeIdSet = resourceCenterMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                resourceTypeIdList.addAll(resourceTypeIdSet);
                if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
                    resourceTypeIdSet = resourceCenterMapper.getNetWorkDeviceResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
                    resourceTypeIdList.addAll(resourceTypeIdSet);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
            for (Long resourceTypeId : resourceTypeIdList) {
                CiVo ciVo = ciVoMap.get(resourceTypeId);
                if (ciVo == null) {
                    throw new CiNotFoundException(resourceTypeId);
                }
                ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName());
                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                if (StringUtils.isBlank(resourceTypeName)) {
                    continue;
                }
                searchVo.setTypeId(resourceTypeId);
                JSONObject tableObj = TableResultUtil.getResult(searchMap.get(resourceTypeName).execute(searchVo), searchVo);
                tableObj.put("type", resourceTypeVo);
                tableList.add(tableObj);
            }
        }
        return tableList;
    }

    @PostConstruct
    public void searchDispatcherInit() {
        searchMap.put("ipObject", (searchVo) -> {
            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
            return new ArrayList<>();
        });
        searchMap.put("OS", (searchVo) -> {
            int rowNum = resourceCenterMapper.getOsResourceCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getOsResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getOsResourceListByIdList(idList, TenantContext.get().getDataDbName());
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
            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getAppInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
            return new ArrayList<>();
        });

//        searchMap.put("APPInsCluster", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });

        searchMap.put("DBIns", (searchVo) -> {
            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    return resourceCenterMapper.getDbInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
                }
            }
            return new ArrayList<>();
        });

//        searchMap.put("DBCluster", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });
//
//        searchMap.put("AccessEndPoint", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });
//        searchMap.put("Database", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });

    }

    private String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo) {
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }
}