/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
    @Resource
    ResourceCenterMapper resourceCenterMapper;

    @Resource
    private CiMapper ciMapper;

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
    public void getResourceAccountAndTag(List<Long> idList, List<ResourceVo> resourceVoList) {
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
            List<AccountVo> accountVoList = resourceAccountVoMap.get(resourceVo.getId());
            if (CollectionUtils.isNotEmpty(accountVoList)) {
                resourceVo.setAccountList(accountVoList);
            }
        }
    }

    /**
     * 获取应用清单信息
     *
     * @param paramObj
     * @return
     */
    @Override
    public JSONArray getAppModuleResourceList(JSONObject paramObj) {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        JSONArray tableList = new JSONArray();
        String schemaName = TenantContext.get().getDataDbName();
        Long appModuleId = searchVo.getAppModuleId();
        if (resourceCenterMapper.checkAppModuleIsExists(appModuleId, schemaName) == 0) {
            throw new AppModuleNotFoundException(appModuleId);
        }

        List<String> resourceTypeNameList = new ArrayList<>();
        resourceTypeNameList.add("OS");
        resourceTypeNameList.add("StorageDevice");
        resourceTypeNameList.add("NetworkDevice");
        resourceTypeNameList.add("APPIns");
        resourceTypeNameList.add("APPInsCluster");
        resourceTypeNameList.add("DBIns");
        resourceTypeNameList.add("DBCluster");
        resourceTypeNameList.add("AccessEndPoint");
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
//                JSONArray theadList = null;
                List<ResourceVo> tbodyList = null;
                if ("OS".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getOsTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getOsTbodyList(searchVo);
                } else if ("StorageDevice".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getStorageDeviceTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getStorageDeviceTbodyList(searchVo);
                } else if ("NetworkDevice".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getNetworkDeviceTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getNetworkDeviceTbodyList(searchVo);
                } else if ("APPIns".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getAPPInsTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getAPPInsTbodyList(searchVo);
                } else if ("APPInsCluster".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getAPPInsClusterTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getAPPInsClusterTbodyList(searchVo);
                } else if ("DBIns".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getDBInsTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getDBInsTbodyList(searchVo);
                } else if ("DBCluster".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getDBClusterTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getDBClusterTbodyList(searchVo);
                } else if ("AccessEndPoint".equals(resourceTypeName)) {
//                    theadList = theadListMap.get(resourceTypeName);
//                    if (theadList == null) {
//                        theadList = getAccessEndPointTheadList();
//                        theadListMap.put(resourceTypeName, theadList);
//                    }
                    tbodyList = getAccessEndPointTbodyList(searchVo);
                }
                JSONObject tableObj = TableResultUtil.getResult(tbodyList, searchVo);
                tableObj.put("type", resourceTypeVo);
                tableList.add(tableObj);
            }
        }
        return tableList;
    }

    private String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo){
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }
    private List<ResourceVo> getOsTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getOsResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getOsResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getOsResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getStorageDeviceTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getStorageResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getStorageResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getStorageResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getNetworkDeviceTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getNetDevResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getNetDevResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getNetDevResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getAPPInsTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getAppInstanceResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getAppInstanceResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getAppInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getAPPInsClusterTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getAppInstanceClusterResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getAppInstanceClusterResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getAppInstanceClusterResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getDBInsTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getDbInstanceResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getDbInstanceResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getDbInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getDBClusterTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getDbInstanceClusterResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getDbInstanceClusterResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getDbInstanceClusterResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }
    private List<ResourceVo> getAccessEndPointTbodyList(ResourceSearchVo searchVo) {
        int rowNum = resourceCenterMapper.getAccessEndPointResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getAccessEndPointResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                return resourceCenterMapper.getAccessEndPointResourceListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        return new ArrayList<>();
    }


//    private JSONArray getOsTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getStorageDeviceTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getNetworkDeviceTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getAPPInsTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getAPPInsClusterTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getDBInsTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getDBClusterTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
//    private JSONArray getAccessEndPointTheadList() {
//        JSONArray theadList = new JSONArray();
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "name");
//            theadObj.put("title", "名称");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ip");
//            theadObj.put("title", "IP地址");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "stateName");
//            theadObj.put("title", "状态");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "envName");
//            theadObj.put("title", "应用环境");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "clusterName");
//            theadObj.put("title", "所在集群");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "dataCenterName");
//            theadObj.put("title", "数据中心");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "networkArea");
//            theadObj.put("title", "网络区域");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "ownerList");
//            theadObj.put("title", "所有者");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "bgList");
//            theadObj.put("title", "所属部门");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "maintenanceWindow");
//            theadObj.put("title", "维护窗口");
//            theadList.add(theadObj);
//        }
//        {
//            JSONObject theadObj = new JSONObject();
//            theadObj.put("key", "description");
//            theadObj.put("title", "描述");
//            theadList.add(theadObj);
//        }
//        return theadList;
//    }
}