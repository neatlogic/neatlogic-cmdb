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

package neatlogic.module.cmdb.resourcecenter.datasource.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityFieldMappingVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.SceneEntityVo;
import neatlogic.framework.cmdb.enums.CmdbTenantConfig;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.Ordered;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityCachedMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import neatlogic.module.cmdb.service.resourcecenter.resource.ResourceBuildSqlService;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
public class DefaultResourceCenterDataSourceImpl implements IResourceCenterDataSource {

    private final Logger logger = LoggerFactory.getLogger(DefaultResourceCenterDataSourceImpl.class);
    @Resource
    private CiMapper ciMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiEntityCachedMapper ciEntityCachedMapper;

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Resource
    private ResourceBuildSqlService resourceBuildSqlService;

    private final Map<ValueTextVo, BiFunction<ResourceVo, JSONObject, Object>> headFieldHandlerMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        headFieldHandlerMap.put(new ValueTextVo("id", "ID"), (resourceVo, cacheData) -> resourceVo.getId());
        headFieldHandlerMap.put(new ValueTextVo("ip", "IP地址"), (resourceVo, cacheData) -> {
            JSONObject resultObj = new JSONObject();
            resultObj.put("ip", resourceVo.getIp());
            resultObj.put("port", resourceVo.getPort());
            resultObj.put("ciId", resourceVo.getTypeId());
            resultObj.put("id", resourceVo.getId());
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("ci", "模型"), (resourceVo, cacheData) -> {
            if (resourceVo.getTypeId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            CiVo ciVo = cacheData.getObject(resourceVo.getTypeId().toString(), CiVo.class);
            if (ciVo != null) {
                resultObj.put("id", ciVo.getId());
                resultObj.put("name", ciVo.getName());
                resultObj.put("label", ciVo.getLabel());
                resultObj.put("icon", ciVo.getIcon());
            } else {
                resultObj.put("id", resourceVo.getTypeId());
                resultObj.put("name", resourceVo.getTypeName());
                resultObj.put("label", resourceVo.getTypeLabel());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("name", "名称"), (resourceVo, cacheData) -> resourceVo.getName());
        headFieldHandlerMap.put(new ValueTextVo("monitor", "监控状态"), (resourceVo, cacheData) -> {
            JSONObject resultObj = new JSONObject();
            if (StringUtils.isNotBlank(resourceVo.getMonitorStatus())) {
                JSONObject statusJson = InspectStatus.getInspectStatusJson(resourceVo.getMonitorStatus());
                if (MapUtils.isNotEmpty(statusJson)) {
                    resultObj.putAll(statusJson);
                }
            }
            if (resourceVo.getMonitorTime() != null) {
                resultObj.put("time", resourceVo.getMonitorTime());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("inspect", "巡检状态"), (resourceVo, cacheData) -> {
            JSONObject resultObj = new JSONObject();
            if (StringUtils.isNotBlank(resourceVo.getInspectStatus())) {
                JSONObject statusJson = InspectStatus.getInspectStatusJson(resourceVo.getInspectStatus());
                if (MapUtils.isNotEmpty(statusJson)) {
                    resultObj.putAll(statusJson);
                }
            }
            if (resourceVo.getInspectTime() != null) {
                resultObj.put("time", resourceVo.getInspectTime());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("appEnvironment", "应用环境"), (resourceVo, cacheData) -> {
            if (resourceVo.getEnvId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            Object obj = cacheData.get(resourceVo.getEnvId().toString());
            if (obj != null) {
                if (obj instanceof GlobalAttrItemVo) {
                    GlobalAttrItemVo globalAttrItemVo = (GlobalAttrItemVo) obj;
                    resultObj.put("id", globalAttrItemVo.getId());
                    resultObj.put("value", globalAttrItemVo.getValue());
                    resultObj.put("attrId", globalAttrItemVo.getAttrId());
                    resultObj.put("type", "globalAttr");
                } else if (obj instanceof CiEntityVo) {
                    CiEntityVo ciEntityVo = (CiEntityVo) obj;
                    resultObj = getResultObj(ciEntityVo);
                    resultObj.put("type", "attr");
                }
            } else {
                resultObj.put("id", resourceVo.getEnvId());
                resultObj.put("name", resourceVo.getEnvName());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("appModule", "应用模块"), (resourceVo, cacheData) -> {
            if (resourceVo.getAppModuleId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            CiEntityVo ciEntityVo = cacheData.getObject(resourceVo.getAppModuleId().toString(), CiEntityVo.class);
            if (ciEntityVo != null) {
                resultObj = getResultObj(ciEntityVo);
                resultObj.put("abbrName", resourceVo.getAppModuleAbbrName());
            } else {
                resultObj.put("id", resourceVo.getAppModuleId());
                resultObj.put("name", resourceVo.getAppModuleName());
                resultObj.put("abbrName", resourceVo.getAppModuleAbbrName());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("appSystem", "应用系统"), (resourceVo, cacheData) -> {
            if (resourceVo.getAppSystemId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            CiEntityVo ciEntityVo = cacheData.getObject(resourceVo.getAppSystemId().toString(), CiEntityVo.class);
            if (ciEntityVo != null) {
                resultObj = getResultObj(ciEntityVo);
                resultObj.put("abbrName", resourceVo.getAppSystemAbbrName());
            } else {
                resultObj.put("id", resourceVo.getAppSystemId());
                resultObj.put("name", resourceVo.getAppSystemName());
                resultObj.put("abbrName", resourceVo.getAppSystemAbbrName());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("allIpList", "IP列表"), (resourceVo, cacheData) -> {
            JSONArray resultList = new JSONArray();
            List<IpVo> ipList = resourceVo.getAllIp();
            if (CollectionUtils.isNotEmpty(ipList)) {
                for (IpVo ipVo : ipList) {
                    JSONObject resultObj = new JSONObject();
                    CiEntityVo ciEntityVo = cacheData.getObject(ipVo.getId().toString(), CiEntityVo.class);
                    if (ciEntityVo != null) {
                        resultObj = getResultObj(ciEntityVo);
                    } else {
                        resultObj.put("id", ipVo.getId());
                        resultObj.put("name", ipVo.getIp());
                    }
                    resultList.add(resultObj);
                }
            }
            return resultList;
        });
        headFieldHandlerMap.put(new ValueTextVo("businessGroupList", "所属部门"), (resourceVo, cacheData) -> {
            JSONArray resultList = new JSONArray();
            List<BgVo> bgList = resourceVo.getBgList();
            if (CollectionUtils.isNotEmpty(bgList)) {
                for (BgVo bgVo : bgList) {
                    JSONObject resultObj = new JSONObject();
                    CiEntityVo ciEntityVo = cacheData.getObject(bgVo.getBgId().toString(), CiEntityVo.class);
                    if (ciEntityVo != null) {
                        resultObj = getResultObj(ciEntityVo);
                    } else {
                        resultObj.put("id", bgVo.getBgId());
                        resultObj.put("name", bgVo.getBgName());
                    }
                    resultList.add(resultObj);
                }
            }
            return resultList;
        });
        headFieldHandlerMap.put(new ValueTextVo("ownerList", "所有者"), (resourceVo, cacheData) -> {
            JSONArray resultList = new JSONArray();
            List<OwnerVo> ownerList = resourceVo.getOwnerList();
            if (CollectionUtils.isNotEmpty(ownerList)) {
                for (OwnerVo ownerVo : ownerList) {
                    JSONObject resultObj = new JSONObject();
                    CiEntityVo ciEntityVo = cacheData.getObject(ownerVo.getUserId().toString(), CiEntityVo.class);
                    if (ciEntityVo != null) {
                        resultObj = getResultObj(ciEntityVo);
                    } else {
                        resultObj.put("id", ownerVo.getUserId());
                        resultObj.put("name", ownerVo.getUserName());
                    }
                    resultList.add(resultObj);
                }
            }
            return resultList;
        });
        headFieldHandlerMap.put(new ValueTextVo("state", "资产状态"), (resourceVo, cacheData) -> {
            if (resourceVo.getStateId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            CiEntityVo ciEntityVo = cacheData.getObject(resourceVo.getStateId().toString(), CiEntityVo.class);
            if (ciEntityVo != null) {
                resultObj = getResultObj(ciEntityVo);
            } else {
                resultObj.put("id", resourceVo.getStateId());
                resultObj.put("name", resourceVo.getStateName());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("networkArea", "网络区域"), (resourceVo, cacheData) -> resourceVo.getNetworkArea());
        headFieldHandlerMap.put(new ValueTextVo("maintenanceWindow", "维护窗口"), (resourceVo, cacheData) -> resourceVo.getMaintenanceWindow());
        headFieldHandlerMap.put(new ValueTextVo("tagList", "标签"), (resourceVo, cacheData) -> resourceVo.getTagList());
        headFieldHandlerMap.put(new ValueTextVo("accountList", "账号"), (resourceVo, cacheData) -> resourceVo.getAccountList());
        headFieldHandlerMap.put(new ValueTextVo("description", "描述"), (resourceVo, cacheData) -> resourceVo.getDescription());
        headFieldHandlerMap.put(new ValueTextVo("vendor", "厂商"), (resourceVo, cacheData) -> {
            if (resourceVo.getVendorId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            CiEntityVo ciEntityVo = cacheData.getObject(resourceVo.getVendorId().toString(), CiEntityVo.class);
            if (ciEntityVo != null) {
                resultObj = getResultObj(ciEntityVo);
            } else {
                resultObj.put("id", resourceVo.getVendorId());
                resultObj.put("name", resourceVo.getVendorName());
            }
            return resultObj;

        });
        headFieldHandlerMap.put(new ValueTextVo("dataCenter", "数据中心"), (resourceVo, cacheData) -> {
            if (resourceVo.getDataCenterId() == null) {
                return null;
            }
            JSONObject resultObj = new JSONObject();
            CiEntityVo ciEntityVo = cacheData.getObject(resourceVo.getDataCenterId().toString(), CiEntityVo.class);
            if (ciEntityVo != null) {
                resultObj = getResultObj(ciEntityVo);
            } else {
                resultObj.put("id", resourceVo.getDataCenterId());
                resultObj.put("name", resourceVo.getDataCenterName());
            }
            return resultObj;
        });
        headFieldHandlerMap.put(new ValueTextVo("fcu", "创建者"), (resourceVo, cacheData) -> {
            String fcu = resourceVo.getFcu();
            if (StringUtils.isNotBlank(fcu)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("uuid", fcu);
                resultObj.put("initType", "user");
                return resultObj;
            }
            return null;
        });
        headFieldHandlerMap.put(new ValueTextVo("fcd", "创建日期"), (resourceVo, cacheData) -> resourceVo.getFcd());
        headFieldHandlerMap.put(new ValueTextVo("lcu", "修改者"), (resourceVo, cacheData) -> {
            String lcu = resourceVo.getLcu();
            if (StringUtils.isNotBlank(lcu)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("uuid", lcu);
                resultObj.put("initType", "user");
                return resultObj;
            }
            return null;
        });
        headFieldHandlerMap.put(new ValueTextVo("lcd", "修改日期"), (resourceVo, cacheData) -> resourceVo.getLcd());
    }

    private JSONObject getResultObj(CiEntityVo ciEntityVo) {
        JSONObject resultObj = new JSONObject();
        resultObj.put("id", ciEntityVo.getId());
        resultObj.put("name", ciEntityVo.getName());
        resultObj.put("ciId", ciEntityVo.getCiId());
        resultObj.put("ciName", ciEntityVo.getCiName());
        resultObj.put("ciLabel", ciEntityVo.getCiLabel());
        resultObj.put("ciIcon", ciEntityVo.getCiIcon());
        resultObj.put("isVirtual", ciEntityVo.getIsVirtual());
        return resultObj;
    }

    @Override
    public Ordered getOrdered() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public JSONArray getAppResourceList(@Nullable Long appSystemId, @Nullable Long appModuleId, @Nullable Long envId, @Nullable List<String> inspectStatusList, @Nullable String viewName, @Nullable Integer currentPage, @Nullable Integer pageSize) {
        if (appSystemId != null && ciEntityCachedMapper.getCiEntityBaseInfoById(appSystemId) == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        if (appModuleId != null && ciEntityCachedMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new AppModuleNotFoundException(appModuleId);
        }
        JSONArray tableList = new JSONArray();
        List<String> viewNameList = new ArrayList<>();
        Map<String, List<String>> viewName2FieldListMap = new HashMap<>();
        ApplicationListDisplayVo applicationListDisplay = resourceEntityMapper.getApplicationListDisplay();
        if (applicationListDisplay != null) {
            JSONObject config = applicationListDisplay.getConfig();
            if (MapUtils.isNotEmpty(config)) {
                JSONArray tableSettingList = config.getJSONArray("tableSettingList");
                if (CollectionUtils.isNotEmpty(tableSettingList)) {
                    for (int i = 0; i < tableSettingList.size(); i++) {
                        JSONObject tableObj = tableSettingList.getJSONObject(i);
                        if (MapUtils.isNotEmpty(tableObj)) {
                            String name = tableObj.getString("viewName");
                            if (StringUtils.isNotBlank(viewName) && !Objects.equals(name, viewName)) {
                                continue;
                            }
                            viewNameList.add(name);
                            JSONArray fieldList = tableObj.getJSONArray("fieldList");
                            if (CollectionUtils.isNotEmpty(fieldList)) {
                                viewName2FieldListMap.put(name, fieldList.toJavaList(String.class));
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(viewNameList)) {
            ResourceSearchVo searchVo = new ResourceSearchVo();
            if (appSystemId != null) {
                searchVo.setAppSystemId(appSystemId);
            }
            if (appModuleId != null) {
                searchVo.setAppModuleId(appModuleId);
            }
            if (envId != null) {
                searchVo.setEnvId(envId);
            }
            if (currentPage != null) {
                searchVo.setCurrentPage(currentPage);
            }
            if (pageSize != null) {
                searchVo.setPageSize(pageSize);
            }
            if (CollectionUtils.isNotEmpty(inspectStatusList)) {
                searchVo.setInspectStatusList(inspectStatusList);
            }
            for (String name : viewNameList) {
                ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
                if (resourceEntityVo != null) {
                    searchVo.setViewName(name);
                    List<String> fieldList = viewName2FieldListMap.get(name);
                    List<ResourceVo> resourceList = getAppResourceList(searchVo, true);
                    JSONArray tbodyList = new JSONArray();
                    if (CollectionUtils.isNotEmpty(resourceList)) {
                        tbodyList = getTbodyList(fieldList, resourceList, resourceEntityVo);
                    }
                    JSONArray theadList = getTheadList(fieldList);
                    JSONObject tableObj = TableResultUtil.getResult(theadList, tbodyList, searchVo);
                    tableObj.put("viewName", name);
                    tableObj.put("viewLabel", resourceEntityVo.getLabel());
                    tableList.add(tableObj);
                }
            }
        }
        return tableList;
    }

    @Override
    public List<ResourceVo> getAppResourceList(ResourceSearchVo searchVo, boolean needPage) {
        List<ResourceVo> resultList = new ArrayList<>();
        int rowNum = 0;
        if (searchVo.getRowNum() == 0) {
            rowNum = resourceMapper.getAppResourceCount(searchVo);
        } else {
            rowNum = searchVo.getRowNum();
        }
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            if (!needPage) {
                searchVo.setPageSize(100);
            }
            Integer pageCount = searchVo.getPageCount();
            for (int i = 1; i <= pageCount; i++) {
                if (needPage && !Objects.equals(i, searchVo.getCurrentPage())) {
                    continue;
                }
                searchVo.setCurrentPage(i);
                List<Long> resourceIdList = resourceMapper.getAppResourceIdList(searchVo);
                if (CollectionUtils.isNotEmpty(resourceIdList)) {
                    searchVo.setIdList(resourceIdList);
                    List<ResourceVo> resourceList = resourceMapper.getAppResourceListByIdList(searchVo);
                    resultList.addAll(resourceList);
                }
                if (needPage) {
                    break;
                }
            }
        }
        return resultList;
    }

    @Override
    public List<Long> getAppResourceIdList(ResourceSearchVo searchVo, boolean needPage) {
        List<Long> resultList = new ArrayList<>();
        int rowNum = resourceMapper.getAppResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            if (needPage) {
                searchVo.setPageSize(100);
            }
            Integer pageCount = searchVo.getPageCount();
            for (int i = 1; i <= pageCount; i++) {
                if (needPage && !Objects.equals(i, searchVo.getCurrentPage())) {
                    continue;
                }
                searchVo.setCurrentPage(i);
                List<Long> resourceIdList = resourceMapper.getAppResourceIdList(searchVo);
                resultList.addAll(resourceIdList);
                if (needPage) {
                    break;
                }
            }
        }
        return resultList;
    }

    @Override
    public JSONArray getTbodyList(List<String> fieldList, List<ResourceVo> resourceList, ResourceEntityVo resourceEntityVo) {
        JSONArray tbodyList = new JSONArray();
        Map<Object, ValueTextVo> keyMap = new HashMap<>();
        for (ValueTextVo key : headFieldHandlerMap.keySet()) {
            keyMap.put(key.getValue(), key);
        }
        JSONObject cacheData = new JSONObject();
        cacheData.put("resourceEntityVo", resourceEntityVo);
        if (fieldList.contains("ci")) {
            List<Long> ciIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getTypeId).filter(Objects::nonNull).collect(Collectors.toList());
            List<CiVo> ciList = ciMapper.getCiByIdList(ciIdList);
            for (CiVo ciVo : ciList) {
                cacheData.put(ciVo.getId().toString(), ciVo);
            }
        }
        Set<Long> ciEntityIdSet = new HashSet<>();
        if (fieldList.contains("state")) {
            List<Long> stateIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getStateId).filter(Objects::nonNull).collect(Collectors.toList());
            ciEntityIdSet.addAll(stateIdList);
        }
        if (fieldList.contains("vendor")) {
            List<Long> vendorIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getVendorId).filter(Objects::nonNull).collect(Collectors.toList());
            ciEntityIdSet.addAll(vendorIdList);
        }
        if (fieldList.contains("dataCenter")) {
            List<Long> dataCenterIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getDataCenterId).filter(Objects::nonNull).collect(Collectors.toList());
            ciEntityIdSet.addAll(dataCenterIdList);
        }
        if (fieldList.contains("appModule")) {
            List<Long> appModuleIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getAppModuleId).filter(Objects::nonNull).collect(Collectors.toList());
            ciEntityIdSet.addAll(appModuleIdList);
        }
        if (fieldList.contains("appSystem")) {
            List<Long> appSystemIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getAppSystemId).filter(Objects::nonNull).collect(Collectors.toList());
            ciEntityIdSet.addAll(appSystemIdList);
        }
        if (fieldList.contains("appEnvironment")) {
            ResourceEntityConfigVo config = resourceEntityVo.getConfig();
            if (config != null) {
                List<ResourceEntityFieldMappingVo> fieldMappingList = config.getFieldMappingList();
                if (CollectionUtils.isNotEmpty(fieldMappingList)) {
                    for (ResourceEntityFieldMappingVo fieldMappingVo : fieldMappingList) {
                        if (Objects.equals(fieldMappingVo.getField(), "env_id")) {
                            List<Long> envIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getEnvId).filter(Objects::nonNull).collect(Collectors.toList());
                            if (Objects.equals(fieldMappingVo.getType(), "globalAttr")) {
                                for (Long envId : envIdList) {
                                    GlobalAttrItemVo globalAttrItemVo = globalAttrMapper.getGlobalAttrItemById(envId);
                                    if (globalAttrItemVo != null) {
                                        cacheData.put(globalAttrItemVo.getId().toString(), globalAttrItemVo);
                                    }
                                }
                            } else if (Objects.equals(fieldMappingVo.getType(), "attr")) {
                                ciEntityIdSet.addAll(envIdList);
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (fieldList.contains("allIpList")) {
            List<Long> ipIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getAllIp).filter(Objects::nonNull).flatMap(List::stream).filter(Objects::nonNull).map(IpVo::getId).collect(Collectors.toList());
            ciEntityIdSet.addAll(ipIdList);
        }
        if (fieldList.contains("ownerList")) {
            List<Long> userIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getOwnerList).filter(Objects::nonNull).flatMap(List::stream).filter(Objects::nonNull).map(OwnerVo::getUserId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(userIdList)) {
                ResourceEntityConfigVo config = resourceEntityVo.getConfig();
                if (config != null) {
                    List<ResourceEntityFieldMappingVo> fieldMappingList = config.getFieldMappingList();
                    if (CollectionUtils.isNotEmpty(fieldMappingList)) {
                        for (ResourceEntityFieldMappingVo fieldMappingVo : fieldMappingList) {
                            if (Objects.equals(fieldMappingVo.getField(), "user_id")) {
                                if (Objects.equals(fieldMappingVo.getType(), "attr") && StringUtils.isNotBlank(fieldMappingVo.getToCi())) {
                                    CiVo ciVo = ciMapper.getCiByName(fieldMappingVo.getToCi());
                                    if (ciVo != null) {
                                        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
                                            CiEntityVo searchVo = new CiEntityVo();
                                            searchVo.setCiId(ciVo.getId());
                                            searchVo.setIdList(userIdList);
                                            List<CiEntityVo> virtualCiEntityList = ciEntityCachedMapper.getVirtualCiEntityBaseInfoByIdList(searchVo);
                                            for (CiEntityVo virtualCiEntity : virtualCiEntityList) {
                                                virtualCiEntity.setCiId(ciVo.getId());
                                                virtualCiEntity.setCiName(ciVo.getName());
                                                virtualCiEntity.setCiLabel(ciVo.getLabel());
                                                virtualCiEntity.setCiIcon(ciVo.getIcon());
                                                virtualCiEntity.setIsVirtual(ciVo.getIsVirtual());
                                                cacheData.put(virtualCiEntity.getId().toString(), virtualCiEntity);
                                            }
                                        } else {
                                            ciEntityIdSet.addAll(userIdList);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

        }
        if (fieldList.contains("businessGroupList")) {
            List<Long> bgIdList = resourceList.stream().filter(Objects::nonNull).map(ResourceVo::getBgList).filter(Objects::nonNull).flatMap(List::stream).filter(Objects::nonNull).map(BgVo::getBgId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bgIdList)) {
                ResourceEntityConfigVo config = resourceEntityVo.getConfig();
                if (config != null) {
                    List<ResourceEntityFieldMappingVo> fieldMappingList = config.getFieldMappingList();
                    if (CollectionUtils.isNotEmpty(fieldMappingList)) {
                        for (ResourceEntityFieldMappingVo fieldMappingVo : fieldMappingList) {
                            if (Objects.equals(fieldMappingVo.getField(), "bg_id")) {
                                if (Objects.equals(fieldMappingVo.getType(), "attr") && StringUtils.isNotBlank(fieldMappingVo.getToCi())) {
                                    CiVo ciVo = ciMapper.getCiByName(fieldMappingVo.getToCi());
                                    if (ciVo != null) {
                                        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
                                            CiEntityVo searchVo = new CiEntityVo();
                                            searchVo.setCiId(ciVo.getId());
                                            searchVo.setIdList(bgIdList);
                                            List<CiEntityVo> virtualCiEntityList = ciEntityCachedMapper.getVirtualCiEntityBaseInfoByIdList(searchVo);
                                            for (CiEntityVo virtualCiEntity : virtualCiEntityList) {
                                                virtualCiEntity.setCiId(ciVo.getId());
                                                virtualCiEntity.setCiName(ciVo.getName());
                                                virtualCiEntity.setCiLabel(ciVo.getLabel());
                                                virtualCiEntity.setCiIcon(ciVo.getIcon());
                                                virtualCiEntity.setIsVirtual(ciVo.getIsVirtual());
                                                cacheData.put(virtualCiEntity.getId().toString(), virtualCiEntity);
                                            }
                                        } else {
                                            ciEntityIdSet.addAll(bgIdList);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(ciEntityIdSet)) {
            List<CiEntityVo> ciEntityList = ciEntityCachedMapper.getCiEntityBaseInfoByIdList(new ArrayList<>(ciEntityIdSet));
            for (CiEntityVo ciEntityVo : ciEntityList) {
                cacheData.put(ciEntityVo.getId().toString(), ciEntityVo);
            }
        }
        if (fieldList.contains("tagList")) {
            resourceCenterResourceService.addTagInformation(resourceList);
        }
        if (fieldList.contains("accountList")) {
            resourceCenterResourceService.addAccountInformation(resourceList);
        }
        for (ResourceVo resourceVo : resourceList) {
            JSONObject tbodyObj = new JSONObject();
            for (String field : fieldList) {
                ValueTextVo key = keyMap.get(field);
                if (key != null) {
                    BiFunction<ResourceVo, JSONObject, Object> biFunction = headFieldHandlerMap.get(key);
                    tbodyObj.put(key.getValue().toString(), biFunction.apply(resourceVo, cacheData));
                }
            }
            if (!fieldList.contains("id")) {
                tbodyObj.put("id", resourceVo.getId());
            }
            tbodyList.add(tbodyObj);
        }
        return tbodyList;
    }

    @Override
    public List<ResourceVo> getResourceList(ResourceSearchVo searchVo) {
        String enable = ConfigManager.getConfig(CmdbTenantConfig.RESOURCECENTER_DATA_COMPARISON_MODE_ENABLE);
        List<ResourceVo> resultList = new ArrayList<>();
        String getResourceIdListSql = resourceBuildSqlService.buildGetResourceIdListSql(searchVo);
        List<Long> idList = resourceMapper.getIdListBySql(getResourceIdListSql);
        //是否存在前置条件
        if (searchVo.getPreCondition() != null && searchVo.getPreCondition().isCustomCondition()) {
            StringBuilder preSqlSb = new StringBuilder();
            searchVo.getPreCondition().buildConditionWhereSql(preSqlSb, searchVo.getPreCondition());
            searchVo.setPreConditionWhereSql(preSqlSb.toString());
        }
        if (Objects.equals(enable, "1")) {
            List<Long> oldIdList = resourceMapper.getResourceIdList(searchVo);
            if (!Objects.equals(oldIdList, idList)) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("idList", idList);
                errorObj.put("oldIdList", oldIdList);
                logger.error("资产清单新旧SQL获取idList结果不一致：{}", errorObj);
            }
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            String getResourceListSql = resourceBuildSqlService.buildGetResourceListSql(idList);
            List<ResourceVo> resourceList = resourceMapper.getResourceListBySql(getResourceListSql);
            if (Objects.equals(enable, "1")) {
                List<ResourceVo> oldResourceList = resourceMapper.getResourceListByIdList(idList);
                checkResourceListIsEquals(resourceList, oldResourceList);
            }
            //排序
            for (Long id : idList) {
                for (ResourceVo resourceVo : resourceList) {
                    if (Objects.equals(id, resourceVo.getId())) {
                        resultList.add(resourceVo);
                        break;
                    }
                }
            }
            if (Objects.equals(searchVo.getRowNum(), 0)) {
                String getResourceCountSql = resourceBuildSqlService.buildGetResourceCountSql(searchVo);
                int rowNum = resourceMapper.getCountBySql(getResourceCountSql);
                if (Objects.equals(enable, "1")) {
                    int oldRowNum = 0;
                    if (noFilterCondition(searchVo)) {
                        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
                        if (resourceEntityVo != null) {
                            ResourceEntityConfigVo config = resourceEntityVo.getConfig();
                            if (config != null) {
                                CiVo ciVo = ciMapper.getCiByName(config.getMainCi());
                                if (ciVo != null) {
                                    searchVo.setViewName(ciVo.getCiTableName(false));
                                    oldRowNum = resourceMapper.getAllResourceCount(searchVo);
                                }
                            }
                        }
                    } else {
                        oldRowNum = resourceMapper.getResourceCount(searchVo);
                    }
                    if (oldRowNum != rowNum) {
                        JSONObject errorObj = new JSONObject();
                        errorObj.put("rowNum", rowNum);
                        errorObj.put("oldRowNum", oldRowNum);
                        logger.error("资产清单新旧SQL获取rowNum结果不一致：{}", errorObj);
                    }
                }
                searchVo.setRowNum(rowNum);
            }
        } else {
            searchVo.setRowNum(0);
        }
        return resultList;
    }

    @Override
    public List<ResourceTypeVo> getResourceTypeTree(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase();
        }
        List<ResourceTypeVo> resultList = new ArrayList<>();
//        List<Long> ciIdList = resourceEntityMapper.getAllResourceTypeCiIdList();
//        if (CollectionUtils.isEmpty(ciIdList)) {
//            return resultList;
//        }
        AssetListDisplayVo assetListDisplayVo = resourceEntityMapper.getAssetListDisplay();
        if (assetListDisplayVo == null) {
            return resultList;
        }
        String rootCiName = assetListDisplayVo.getRootCiName();
        if (StringUtils.isBlank(rootCiName)) {
            return resultList;
        }
        CiVo rootCiVo = ciMapper.getCiByName(rootCiName);
        if (rootCiVo == null) {
            throw new CiNotFoundException(rootCiName);
        }
        List<Long> ciIdList = Collections.singletonList(rootCiVo.getId());
        List<CiVo> authCiVoList = new ArrayList<>();
        ResourceSearchVo searchVo = new ResourceSearchVo();
        searchVo.setTypeIdList(ciIdList);
        resourceCenterResourceService.assembleResourceSearchVo(searchVo, false);
//        jsonObj.put("typeIdList", ciIdList);
//        ResourceSearchVo searchVo = resourceCenterResourceService.assembleResourceSearchVo(jsonObj, false);
        //先找出所有有权限的配置项的模型idList
        if (!searchVo.getIsHasAuth()) {
            Set<Long> authCiIdList = ciMapper.getAllAuthCi(UserContext.get().getAuthenticationInfoVo()).stream().map(CiVo::getId).collect(Collectors.toSet());
            authCiIdList.addAll(resourceMapper.getResourceTypeIdListByAuth(searchVo));
            if (CollectionUtils.isEmpty(authCiIdList)) {
                return resultList;
            }
            authCiVoList = ciMapper.getCiByIdList(new ArrayList<>(authCiIdList));
        }

        if (CollectionUtils.isNotEmpty(ciIdList)) {
            List<CiVo> ciVoList = ciMapper.getCiByIdList(ciIdList);
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            for (CiVo ciVo : ciVoList) {
                Set<CiVo> ciList = new HashSet<>();
                List<CiVo> ciListTmp = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                //过滤出所有有权限的配置项的模型idList
                if (!searchVo.getIsHasAuth()) {
                    if (CollectionUtils.isNotEmpty(authCiVoList) && CollectionUtils.isNotEmpty(ciListTmp)) {
                        for (CiVo ci : ciListTmp) {
                            for (CiVo authCi : authCiVoList) {
                                if (ci.getLft() <= authCi.getLft() && ci.getRht() >= authCi.getRht()) {
                                    ciList.add(ci);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    ciList = new HashSet<>(ciListTmp);
                }
                int size = ciList.size();
                List<ResourceTypeVo> resourceTypeVoList = new ArrayList<>(size);
                Map<Long, ResourceTypeVo> resourceTypeMap = new HashMap<>(size);
                for (CiVo ci : ciList) {
                    ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ci.getId(), ci.getParentCiId(), ci.getLabel(), ci.getName());
                    resourceTypeMap.put(resourceTypeVo.getId(), resourceTypeVo);
                    resourceTypeVoList.add(resourceTypeVo);
                }
                if (StringUtils.isNotBlank(keyword)) {
                    // 建立父子关系
                    for (ResourceTypeVo resourceType : resourceTypeVoList) {
                        if (resourceType.getParentId() != null) {
                            ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                            if (parentResourceType != null) {
                                resourceType.setParent(parentResourceType);
                                parentResourceType.addChild(resourceType);
                            }
                        }
                    }
                    // 判断节点名称是否与关键字keyword匹配，如果匹配就将该节点及其父子节点的isKeywordMatch字段值设置为1，否则设置为0。
                    for (ResourceTypeVo resourceType : resourceTypeVoList) {
                        if (resourceType.getLabel().toLowerCase().contains(keyword)) {
                            if (resourceType.getIsKeywordMatch() == null) {
                                resourceType.setIsKeywordMatch(1);
                                resourceType.setUpwardIsKeywordMatch(1);
                                resourceType.setDownwardIsKeywordMatch(1);
                            }
                        } else {
                            if (resourceType.getIsKeywordMatch() == null) {
                                resourceType.setIsKeywordMatch(0);
                            }
                        }
                    }
                    // 将isKeywordMatch字段值为0的节点从其父级中移除。
                    Iterator<ResourceTypeVo> iterator = resourceTypeVoList.iterator();
                    while (iterator.hasNext()) {
                        ResourceTypeVo resourceType = iterator.next();
                        if (Objects.equals(resourceType.getIsKeywordMatch(), 0)) {
                            ResourceTypeVo parent = resourceType.getParent();
                            if (parent != null) {
                                parent.removeChild(resourceType);
                            }
                            iterator.remove();
                        }
                    }
                } else {
                    for (ResourceTypeVo resourceType : resourceTypeVoList) {
                        if (resourceType.getParentId() != null) {
                            ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                            if (parentResourceType != null) {
                                parentResourceType.addChild(resourceType);
                            }
                        }
                    }
                }
                for (ResourceTypeVo resourceType : resourceTypeVoList) {
                    if (resourceType.getParentId() != null) {
                        ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                        if (parentResourceType == null) {
                            resultList.add(resourceType);
                        }
                    } else {
                        resultList.add(resourceType);
                    }
                }
            }
        }
        return resultList;
    }

    @Override
    public List<ResourceTypeVo> getResourceTypeListTree(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase();
        }
        List<ResourceTypeVo> resultList = new ArrayList<>();
        ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName("scence_ipobject_detail");
        if (resourceEntityVo == null) {
            return resultList;
        }
        ResourceEntityConfigVo config = resourceEntityVo.getConfig();
        if (config == null) {
            return resultList;
        }
        CiVo ciVo = ciMapper.getCiByName(config.getMainCi());
        if (ciVo == null) {
            return resultList;
        }

        List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        int size = ciList.size();
        List<ResourceTypeVo> resourceTypeVoList = new ArrayList<>(size);
        Map<Long, ResourceTypeVo> resourceTypeMap = new HashMap<>(size);
        for (CiVo ci : ciList) {
            ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ci.getId(), ci.getParentCiId(), ci.getLabel(), ci.getName());
            resourceTypeMap.put(resourceTypeVo.getId(), resourceTypeVo);
            resourceTypeVoList.add(resourceTypeVo);
        }
        if (StringUtils.isNotBlank(keyword)) {
            // 建立父子关系
            for (ResourceTypeVo resourceType : resourceTypeVoList) {
                if (resourceType.getParentId() != null) {
                    ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                    if (parentResourceType != null) {
                        resourceType.setParent(parentResourceType);
                        parentResourceType.addChild(resourceType);
                    }
                }
            }
            // 判断节点名称是否与关键字keyword匹配，如果匹配就将该节点及其父子节点的isKeywordMatch字段值设置为1，否则设置为0。
            for (ResourceTypeVo resourceType : resourceTypeVoList) {
                if (resourceType.getLabel().toLowerCase().contains(keyword)) {
                    if (resourceType.getIsKeywordMatch() == null) {
                        resourceType.setIsKeywordMatch(1);
                        resourceType.setUpwardIsKeywordMatch(1);
                        resourceType.setDownwardIsKeywordMatch(1);
                    }
                } else {
                    if (resourceType.getIsKeywordMatch() == null) {
                        resourceType.setIsKeywordMatch(0);
                    }
                }
            }
            // 将isKeywordMatch字段值为0的节点从其父级中移除。
            Iterator<ResourceTypeVo> iterator = resourceTypeVoList.iterator();
            while (iterator.hasNext()) {
                ResourceTypeVo resourceType = iterator.next();
                if (Objects.equals(resourceType.getIsKeywordMatch(), 0)) {
                    ResourceTypeVo parent = resourceType.getParent();
                    if (parent != null) {
                        parent.removeChild(resourceType);
                    }
                    iterator.remove();
                }
            }
        } else {
            for (ResourceTypeVo resourceType : resourceTypeVoList) {
                if (resourceType.getParentId() != null) {
                    ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                    if (parentResourceType != null) {
                        parentResourceType.addChild(resourceType);
                    }
                }
            }
        }
        for (ResourceTypeVo resourceType : resourceTypeVoList) {
            if (resourceType.getParentId() != null) {
                ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                if (parentResourceType == null) {
                    resultList.add(resourceType);
                }
            } else {
                resultList.add(resourceType);
            }
        }
        return resultList;
    }

    @Override
    public JSONArray getTheadList(List<String> fieldList) {
        JSONArray theadList = new JSONArray();
        Map<Object, ValueTextVo> keyMap = new HashMap<>();
        for (ValueTextVo key : headFieldHandlerMap.keySet()) {
            keyMap.put(key.getValue(), key);
        }
        for (String field : fieldList) {
            ValueTextVo valueTextVo = keyMap.get(field);
            if (valueTextVo != null) {
                JSONObject thead = new JSONObject();
                thead.put("key", valueTextVo.getValue());
                thead.put("title", valueTextVo.getText());
                theadList.add(thead);
            }
        }
        return theadList;
    }

    @Override
    public List<ValueTextVo> getAssertAllTheadList() {
        Set<ValueTextVo> keySet = headFieldHandlerMap.keySet();
        return new ArrayList<>(keySet);
    }

    @Override
    public List<ValueTextVo> getAppAssertAllTheadList() {
        List<ValueTextVo> resultList = new ArrayList<>();
        for (ValueTextVo valueTextVo : headFieldHandlerMap.keySet()) {
            if (Objects.equals(valueTextVo.getValue(), "tagList")) {
                continue;
            }
            if (Objects.equals(valueTextVo.getValue(), "accountList")) {
                continue;
            }
            resultList.add(valueTextVo);
        }
        return resultList;
    }

    @Override
    public List<AppSystemVo> getAppSystemListForTree(BasePageVo searchVo) {
        List<AppSystemVo> appSystemList = new ArrayList<>();
        String keyword = searchVo.getKeyword();
        int count = resourceMapper.getAppSystemIdListCountByKeyword(keyword);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> appSystemIdList = resourceMapper.getAppSystemIdListByKeyword(searchVo);
            if (CollectionUtils.isEmpty(appSystemIdList)) {
                return appSystemList;
            }
            appSystemList = resourceMapper.getAppSystemListByIdList(appSystemIdList);
            List<Long> hasModuleAppSystemIdList = resourceMapper.getHasModuleAppSystemIdListByAppSystemIdList(appSystemIdList);
            if (CollectionUtils.isNotEmpty(hasModuleAppSystemIdList)) {
                for (AppSystemVo appSystemVo : appSystemList) {
                    if (hasModuleAppSystemIdList.contains(appSystemVo.getId())) {
                        appSystemVo.setIsHasModule(1);
                    }
                }
            }
            if (StringUtils.isNotEmpty(keyword)) {
                List<AppModuleVo> appModuleList = resourceMapper.getAppModuleListByKeywordAndAppSystemIdList(keyword, appSystemIdList);
                if (CollectionUtils.isNotEmpty(appModuleList)) {
                    Map<Long, List<AppModuleVo>> appModuleMap = new HashMap<>();
                    for (AppModuleVo appModuleVo : appModuleList) {
                        appModuleMap.computeIfAbsent(appModuleVo.getAppSystemId(), key -> new ArrayList<>()).add(appModuleVo);
                    }
                    for (AppSystemVo appSystemVo : appSystemList) {
                        List<AppModuleVo> appModuleVoList = appModuleMap.get(appSystemVo.getId());
                        if (CollectionUtils.isNotEmpty(appModuleVoList)) {
                            appSystemVo.setAppModuleList(appModuleVoList);
                            appSystemVo.setIsHasModule(1);
                        }
                    }
                }
            }
        }
        return appSystemList;
    }

    @Override
    public List<ResourceVo> getAppSystemListForSelect(BasePageVo searchVo) {
        List<ResourceVo> resourceList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            resourceList = resourceMapper.searchAppSystemListByIdList(idList);
        } else {
            int rowNum = resourceMapper.searchAppSystemCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                if (searchVo.getNeedPage()) {
                    List<Long> idList = resourceMapper.searchAppSystemIdList(searchVo);
                    resourceList = resourceMapper.searchAppSystemListByIdList(idList);
                } else {
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = resourceMapper.searchAppSystemIdList(searchVo);
                        List<ResourceVo> list = resourceMapper.searchAppSystemListByIdList(idList);
                        resourceList.addAll(list);
                    }
                }
            }
        }
        return resourceList;
    }

    @Override
    public List<AppModuleVo> getAppModuleListForTree(Long appSystemId) {
        List<AppModuleVo> appModuleList = resourceMapper.getAppModuleListByAppSystemId(appSystemId);
        if (CollectionUtils.isNotEmpty(appModuleList)) {
            Map<Long, Long> appEnvCountMap = new HashMap<>();
            List<Map<String, Long>> appEnvCountMapList = resourceMapper.getAppEnvCountMapByAppSystemIdGroupByAppModuleId(appSystemId);
            for (Map<String, Long> map : appEnvCountMapList) {
                Long count = map.get("count");
                Long appModuleId = map.get("appModuleId");
                appEnvCountMap.put(appModuleId, count);
            }
            for (AppModuleVo appModuleVo : appModuleList) {
                Long count = appEnvCountMap.get(appModuleVo.getId());
                if (count == null) {
                    appModuleVo.setIsHasEnv(0);
                } else if (count == 0) {
                    appModuleVo.setIsHasEnv(0);
                } else {
                    appModuleVo.setIsHasEnv(1);
                }
            }
        }
        return appModuleList;
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
    public List<ResourceVo> getAppEnvListForSelect(BasePageVo searchVo, boolean needPage) {
        List<ResourceVo> appEnvList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            appEnvList = resourceMapper.searchAppEnvListByIdList(idList);
        } else {
            int rowNum = resourceMapper.searchAppEnvCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                if (needPage) {
                    List<Long> idList = resourceMapper.searchAppEnvIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        appEnvList = resourceMapper.searchAppEnvListByIdList(idList);
                    }
                } else {
                    searchVo.setPageSize(100);
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = resourceMapper.searchAppEnvIdList(searchVo);
                        if (CollectionUtils.isNotEmpty(idList)) {
                            List<ResourceVo> list = resourceMapper.searchAppEnvListByIdList(idList);
                            appEnvList.addAll(list);
                        }
                    }
                }
            }
        }
        return appEnvList;
    }

    @Override
    public List<AppEnvVo> getAppEnvListByAppSystemIdAndAppModuleIdAndInspectStatusList(Long appSystemId, Long appModuleId, List<String> inspectStatusList) {
        List<AppEnvVo> resultList = new ArrayList<>();
        Map<Long, AppEnvVo> appEnvMap = new HashMap<>();
        Map<Long, List<AppModuleVo>> appEnvId2AppModuleListMap = new HashMap<>();
        List<ResourceEntityVo> appViewList = getAppViewList();
        for (ResourceEntityVo resourceEntityVo : appViewList) {
            List<AppEnvVo> appEnvList = resourceMapper.getAppEnvListByViewNameAndAppSystemIdAndAppModuleIdAndInspectStatusList(resourceEntityVo.getName(), appSystemId, appModuleId, inspectStatusList);
            for (AppEnvVo appEnvVo : appEnvList) {
                appEnvMap.put(appEnvVo.getId(), appEnvVo);
                List<AppModuleVo> appModuleList = appEnvVo.getAppModuleList();
                if (CollectionUtils.isNotEmpty(appModuleList)) {
                    for (AppModuleVo appModuleVo : appModuleList) {
                        appEnvId2AppModuleListMap.computeIfAbsent(appEnvVo.getId(), key -> new ArrayList<>()).add(appModuleVo);
                    }

                }
            }
        }
        for (Map.Entry<Long, AppEnvVo> entry : appEnvMap.entrySet()) {
            AppEnvVo appEnvVo = entry.getValue();
            Map<Long, AppModuleVo> appModuleMap = new HashMap<>();
            List<AppModuleVo> appModuleList = appEnvId2AppModuleListMap.get(appEnvVo.getId());
            for (AppModuleVo appModuleVo : appModuleList) {
                AppModuleVo appModule = appModuleMap.get(appModuleVo.getId());
                if (appModule == null) {
                    appModule = new AppModuleVo();
                    appModule.setId(appModuleVo.getId());
                    appModule.setName(appModuleVo.getName());
                    appModule.setAbbrName(appModuleVo.getAbbrName());
                    List<CiVo> ciList = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(appModuleVo.getCiList())) {
                        ciList.addAll(appModuleVo.getCiList());
                    }
                    appModule.setCiList(ciList);
                    appModuleMap.put(appModuleVo.getId(), appModule);
                } else {
                    if (CollectionUtils.isNotEmpty(appModuleVo.getCiList())) {
                        appModule.getCiList().addAll(appModuleVo.getCiList());
                    }
                }
            }
            List<AppModuleVo> mergeAppModuleList = new ArrayList<>(appModuleMap.values());
            mergeAppModuleList.sort(Comparator.comparing(AppModuleVo::getId));
            appEnvVo.setAppModuleList(mergeAppModuleList);
            resultList.add(appEnvVo);
        }
        resultList.sort(Comparator.comparing(AppEnvVo::getSeqNo));
        return resultList;
    }

    @Override
    public List<ResourceVo> getStateListForSelect(BasePageVo searchVo) {
        List<ResourceVo> stateList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            stateList = resourceMapper.searchStateListByIdList(idList);
        } else {
            int rowNum = resourceMapper.searchStateCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                if (searchVo.getNeedPage()) {
                    List<Long> idList = resourceMapper.searchStateIdList(searchVo);
                    stateList = resourceMapper.searchStateListByIdList(idList);
                } else {
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = resourceMapper.searchStateIdList(searchVo);
                        List<ResourceVo> list = resourceMapper.searchStateListByIdList(idList);
                        stateList.addAll(list);
                    }
                }
            }
        }
        return stateList;
    }

    @Override
    public List<ResourceVo> getVendorListForSelect(BasePageVo searchVo) {
        List<ResourceVo> vendorList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            vendorList = resourceMapper.searchVendorListByIdList(idList);
        } else {
            int rowNum = resourceMapper.searchVendorCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                if (searchVo.getNeedPage()) {
                    List<Long> idList = resourceMapper.searchVendorIdList(searchVo);
                    vendorList = resourceMapper.searchVendorListByIdList(idList);
                } else {
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        searchVo.setCurrentPage(currentPage);
                        List<Long> idList = resourceMapper.searchVendorIdList(searchVo);
                        List<ResourceVo> list = resourceMapper.searchVendorListByIdList(idList);
                        vendorList.addAll(list);
                    }
                }
            }
        }
        return vendorList;
    }

    @Override
    public Map<String, List<Long>> getAppResourceTypeIdListByAppSystemId(Long appSystemId) {
        return getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(appSystemId, null, null, null);
    }

    @Override
    public Map<String, List<Long>> getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvId(Long appSystemId, Long appModuleId, Long envId) {
        return getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(appSystemId, appModuleId, envId, null);
    }

    @Override
    public Map<String, List<Long>> getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvIdAndInspectStatusList(Long appSystemId, Long appModuleId, Long envId, List<String> inspectStatusList) {
        Map<String, List<Long>> viewName2TypeIdListMap = new HashMap<>();
        List<ResourceEntityVo> appViewList = getAppViewList();
        if (CollectionUtils.isNotEmpty(appViewList)) {
            for (ResourceEntityVo resourceEntityVo : appViewList) {
                List<Long> typeIdList = resourceMapper.getAppResourceTypeIdListByViewNameAndAppSystemId(resourceEntityVo.getName(), appSystemId, appModuleId, envId, inspectStatusList);
                viewName2TypeIdListMap.put(resourceEntityVo.getName(), typeIdList);
            }
        }
        return viewName2TypeIdListMap;
    }

    @Override
    public List<Long> getAppSystemIdListById(Long id) {
        List<Long> appSystemIdList = new ArrayList<>();
        List<ResourceEntityVo> appViewList = getAppViewList();
        if (CollectionUtils.isNotEmpty(appViewList)) {
            for (ResourceEntityVo resourceEntityVo : appViewList) {
                List<Long> list = resourceMapper.getAppSystemIdListById(resourceEntityVo.getName(), id);
                appSystemIdList.addAll(list);
            }
        }
        return appSystemIdList;
    }

    @Override
    public List<ResourceEntityVo> getAppViewList() {
        List<ResourceEntityVo> resultList = new ArrayList<>();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityList();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
            if (sceneEntityVo == null) {
                String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
                if (StringUtils.isNotBlank(config)) {
                    ResourceEntityConfigVo resourceEntityConfigVo = JSONObject.parseObject(config, ResourceEntityConfigVo.class);
                    if (Objects.equals(resourceEntityConfigVo.getSceneTemplateName(), "scence_application_asset_list_detail")) {
                        resourceEntityVo.setConfig(resourceEntityConfigVo);
                        resultList.add(resourceEntityVo);
                    }
                }
            }
        }
        return resultList;
    }

    /**
     * 判断是否有过滤条件
     *
     * @param searchVo
     * @return
     */
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
}
