/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.enums.resourcecenter.AppModuleResourceType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigNotFoundException;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;
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
    ResourceCenterCommonGenerateSqlService resourceCenterCommonGenerateSqlService;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;

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
        List<ResourceAccountVo> resourceAccountVoList = resourceCenterMapper.getResourceAccountListByResourceIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            Set<Long> accountIdSet = resourceAccountVoList.stream().map(ResourceAccountVo::getAccountId).collect(Collectors.toSet());
            List<AccountVo> accountList = resourceCenterMapper.getAccountListByIdList(new ArrayList<>(accountIdSet));
            Map<Long, AccountVo> accountMap = accountList.stream().collect(Collectors.toMap(AccountVo::getId, e -> e));
            for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
                resourceAccountVoMap.computeIfAbsent(resourceAccountVo.getResourceId(), k -> new ArrayList<>()).add(accountMap.get(resourceAccountVo.getAccountId()));
            }
        }
        return resourceAccountVoMap;
    }

    @Override
    public Map<Long, List<TagVo>> getResourceTagByResourceIdList(List<Long> idList) {
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
        return resourceTagVoMap;
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
     * 其中清单列表有 系统 应用实例 应用实例集群 DB实例 DB实例集群 访问入口
     *
     * @param searchVo
     * @return
     */
    @Override
    public JSONArray getAppModuleResourceList(ResourceSearchVo searchVo) {
        JSONArray tableList = new JSONArray();
//        String schemaName = TenantContext.get().getDataDbName();
        Long appModuleId = searchVo.getAppModuleId();
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(appModuleId);
        if (ciEntityVo == null) {
            throw new AppModuleNotFoundException(appModuleId);
        }
//        if (resourceCenterMapper.checkAppModuleIsExists(appModuleId, schemaName) == 0) {
//            throw new AppModuleNotFoundException(appModuleId);
//        }
        //20220302跟产品确认应用清单的模块中不需要出现“网络设备”和“存储设备StorageDevice”
//        List<String> resourceTypeNameList = Arrays.asList("OS", "StorageDevice", "NetworkDevice", "APPIns", "APPInsCluster", "DBIns", "DBCluster", "AccessEndPoint", "Database");
//        Map<String, String> typeNameActionMap = new HashMap<>();
//        typeNameActionMap.put("OS", "OS");
//        typeNameActionMap.put("APPIns", "APPIns");
//        typeNameActionMap.put("APPInsCluster", "ipObject");
//        typeNameActionMap.put("DBIns", "DBIns");
//        typeNameActionMap.put("DBCluster", "ipObject");
//        typeNameActionMap.put("AccessEndPoint", "ipObject");
//        typeNameActionMap.put("Database", "ipObject");
//        List<CiVo> resourceCiVoList = new ArrayList<>();
//        Map<Long, CiVo> ciVoMap = new HashMap<>();
//        List<CiVo> ciVoList = ciMapper.getAllCi(null);
//        for (CiVo ciVo : ciVoList) {
//            ciVoMap.put(ciVo.getId(), ciVo);
//            if (typeNameActionMap.containsKey(ciVo.getName())) {
//                resourceCiVoList.add(ciVo);
//            }
//        }
        List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(AppModuleResourceType.getNameList());
        List<Long> resourceTypeIdList = new ArrayList<>();
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            resourceTypeIdList.add(typeId);
        } else {
            Set<Long> resourceTypeIdSet = resourceCenterMapper.getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
            resourceTypeIdList.addAll(resourceTypeIdSet);
//            if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
//                resourceTypeIdSet = resourceCenterMapper.getOsResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
//                resourceTypeIdList.addAll(resourceTypeIdSet);
////                if (CollectionUtils.isNotEmpty(resourceTypeIdSet)) {
////                    resourceTypeIdSet = resourceCenterMapper.getNetWorkDeviceResourceTypeIdListByAppModuleIdAndEnvId(searchVo);
////                    resourceTypeIdList.addAll(resourceTypeIdSet);
////                }
//            }
        }

        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
            List<CiVo> ciList = ciMapper.getAllCi(resourceTypeIdList);
            for (CiVo ciVo : ciList) {
                ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName());
                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                if (StringUtils.isBlank(resourceTypeName)) {
                    continue;
                }
//                String actionKey = typeNameActionMap.get(resourceTypeName);
                String actionKey = AppModuleResourceType.getAction(resourceTypeName);
                if (StringUtils.isBlank(actionKey)) {
                    continue;
                }
                searchVo.setTypeId(ciVo.getId());
                long startTime = System.currentTimeMillis();
                List<ResourceVo> returnList = searchMap.get(actionKey).execute(searchVo);
                System.out.println(actionKey + "=" + (System.currentTimeMillis() - startTime));
                if (CollectionUtils.isNotEmpty(returnList)) {
                    JSONObject tableObj =TableResultUtil.getResult(returnList, searchVo);
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
            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
                    List<ResourceInfo> theadList = new ArrayList<>();
                    theadList.add(new ResourceInfo("resource_ipobject", "id"));
                    //1.IP地址:端口
                    theadList.add(new ResourceInfo("resource_ipobject", "ip"));
                    theadList.add(new ResourceInfo("resource_softwareservice", "port"));
                    //3.名称
                    theadList.add(new ResourceInfo("resource_ipobject", "name"));
                    theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
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
                    //9.数据中心
                    theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_id"));
                    theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_name"));
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
                    theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
                    theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
                    String sql = resourceCenterCommonGenerateSqlService.getResourceListByIdListSql("resource_ipobject", theadList, idList, unavailableResourceInfoList);
                    if (StringUtils.isBlank(sql)) {
                        return new ArrayList<>();
                    }
                    return resourceCenterCommonGenerateSqlService.getResourceList(sql);
                }
            }
            return new ArrayList<>();
        });
        //2022-07-06 产品经理决定不用显示系统模型下的数据
//        searchMap.put("OS", (searchVo) -> {
//            int rowNum = resourceCenterMapper.getOsResourceCountByAppModuleIdAndEnvIdAndTypeId(searchVo);
//            if (rowNum > 0) {
//                searchVo.setRowNum(rowNum);
//                List<Long> idList = resourceCenterMapper.getOsResourceIdListByAppModuleIdAndEnvIdAndTypeId(searchVo);
//                if (CollectionUtils.isNotEmpty(idList)) {
//                    return resourceCenterMapper.getOsResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                }
//            }
//            return new ArrayList<>();
//        });

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
            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
                    List<ResourceInfo> theadList = new ArrayList<>();
                    theadList.add(new ResourceInfo("resource_appinstance", "id"));
                    //1.IP地址:端口
                    theadList.add(new ResourceInfo("resource_appinstance", "ip"));
                    theadList.add(new ResourceInfo("resource_softwareservice", "port"));
                    //3.名称
                    theadList.add(new ResourceInfo("resource_appinstance", "name"));
                    theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
                    //4.监控状态
                    theadList.add(new ResourceInfo("resource_appinstance", "monitor_status"));
                    theadList.add(new ResourceInfo("resource_appinstance", "monitor_time"));
                    //5.巡检状态
                    theadList.add(new ResourceInfo("resource_appinstance", "inspect_status"));
                    theadList.add(new ResourceInfo("resource_appinstance", "inspect_time"));
                    //12.网络区域
                    theadList.add(new ResourceInfo("resource_appinstance", "network_area"));
                    //14.维护窗口
                    theadList.add(new ResourceInfo("resource_appinstance", "maintenance_window"));
                    //16.描述
                    theadList.add(new ResourceInfo("resource_appinstance", "description"));
                    //9.数据中心
                    theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_id"));
                    theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_name"));
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
                    theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
                    theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
                    //所在集群
                    theadList.add(new ResourceInfo("resource_appinstance_appinstancecluster", "cluster_id"));
                    theadList.add(new ResourceInfo("resource_appinstance_appinstancecluster", "cluster_type_id"));
                    theadList.add(new ResourceInfo("resource_appinstance_appinstancecluster", "cluster_name"));
                    String sql = resourceCenterCommonGenerateSqlService.getResourceListByIdListSql("resource_appinstance", theadList, idList, unavailableResourceInfoList);
                    if (StringUtils.isBlank(sql)) {
                        return new ArrayList<>();
                    }
//                    System.out.println("APPIns:" + sql + ";");
                    List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlService.getResourceList(sql);
//                    List<ResourceVo> resourceList2 = resourceCenterMapper.getAppInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                    for (int i = 0; i < idList.size(); i++) {
//                        String resourceVoString = JSONObject.toJSONString(resourceList.get(i));
//                        String resourceVo2String = JSONObject.toJSONString(resourceList2.get(i));
//                        if (!Objects.equals(resourceVoString, resourceVo2String)) {
//                            System.out.println("resourceVoString=" + resourceVoString);
//                            System.out.println("resourceVo2String=" + resourceVo2String);
//                        }
//                    }
                    return resourceList;
//                    return resourceCenterMapper.getAppInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
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
            int rowNum = resourceCenterMapper.getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvIdAndTypeId(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                List<Long> idList = resourceCenterMapper.getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvIdAndTypeId(searchVo);
                if (CollectionUtils.isNotEmpty(idList)) {
                    List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
                    List<ResourceInfo> theadList = new ArrayList<>();
                    theadList.add(new ResourceInfo("resource_dbinstance", "id"));
                    //1.IP地址:端口
                    theadList.add(new ResourceInfo("resource_dbinstance", "ip"));
                    theadList.add(new ResourceInfo("resource_softwareservice", "port"));
                    //3.名称
                    theadList.add(new ResourceInfo("resource_dbinstance", "name"));
                    theadList.add(new ResourceInfo("resource_ipobject", "type_id"));
                    //4.监控状态
                    theadList.add(new ResourceInfo("resource_dbinstance", "monitor_status"));
                    theadList.add(new ResourceInfo("resource_dbinstance", "monitor_time"));
                    //5.巡检状态
                    theadList.add(new ResourceInfo("resource_dbinstance", "inspect_status"));
                    theadList.add(new ResourceInfo("resource_dbinstance", "inspect_time"));
                    //12.网络区域
                    theadList.add(new ResourceInfo("resource_dbinstance", "network_area"));
                    //14.维护窗口
                    theadList.add(new ResourceInfo("resource_dbinstance", "maintenance_window"));
                    //16.描述
                    theadList.add(new ResourceInfo("resource_dbinstance", "description"));
                    //9.数据中心
                    theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_id"));
                    theadList.add(new ResourceInfo("resource_ipobject_datacenter", "datacenter_name"));
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
                    theadList.add(new ResourceInfo("resource_softwareservice_env", "env_id"));
                    theadList.add(new ResourceInfo("resource_softwareservice_env", "env_name"));
                    //所在集群
                    theadList.add(new ResourceInfo("resource_dbinstance_dbcluster", "cluster_id"));
                    theadList.add(new ResourceInfo("resource_dbinstance_dbcluster", "cluster_type_id"));
                    theadList.add(new ResourceInfo("resource_dbinstance_dbcluster", "cluster_name"));
                    String sql = resourceCenterCommonGenerateSqlService.getResourceListByIdListSql("resource_dbinstance", theadList, idList, unavailableResourceInfoList);
                    if (StringUtils.isBlank(sql)) {
                        return new ArrayList<>();
                    }
//                    System.out.println("DBIns:" + sql + ";");
                    List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlService.getResourceList(sql);
//                    List<ResourceVo> resourceList2 = resourceCenterMapper.getDbInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
//                    for (int i = 0; i < idList.size(); i++) {
//                        String resourceVoString = JSONObject.toJSONString(resourceList.get(i));
//                        String resourceVo2String = JSONObject.toJSONString(resourceList2.get(i));
//                        if (!Objects.equals(resourceVoString, resourceVo2String)) {
//                            System.out.println("resourceVoString=" + resourceVoString);
//                            System.out.println("resourceVo2String=" + resourceVo2String);
//                        }
//                    }
                    return resourceList;
//                    return resourceCenterMapper.getDbInstanceResourceListByIdList(idList, TenantContext.get().getDataDbName());
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

    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo) {
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }
}