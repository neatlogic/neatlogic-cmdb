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
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.Ordered;
import neatlogic.framework.common.constvalue.InspectStatus;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityCachedMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
public class DefaultResourceCenterDataSourceImpl implements IResourceCenterDataSource {

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

    private final Map<ValueTextVo, BiFunction<ResourceVo, JSONObject, Object>> map = new HashMap<>();//BiFunction

    @PostConstruct
    public void init() {
        map.put(new ValueTextVo("id", "ID"), (resourceVo, cacheData) -> resourceVo.getId());
        map.put(new ValueTextVo("name", "名称"), (resourceVo, cacheData) -> resourceVo.getName());
        map.put(new ValueTextVo("ip", "IP地址"), (resourceVo, cacheData) -> {
            JSONObject resultObj = new JSONObject();
            resultObj.put("ip", resourceVo.getIp());
            resultObj.put("port", resourceVo.getPort());
            resultObj.put("ciId", resourceVo.getTypeId());
            resultObj.put("id", resourceVo.getId());
            return resultObj;
        });
        map.put(new ValueTextVo("ci", "模型"), (resourceVo, cacheData) -> {
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

        map.put(new ValueTextVo("fcu", "创建者"), (resourceVo, cacheData) -> {
            String fcu = resourceVo.getFcu();
            if (StringUtils.isNotBlank(fcu)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("uuid", fcu);
                resultObj.put("initType", "user");
                return resultObj;
            }
            return null;
        });
        map.put(new ValueTextVo("fcd", "创建日期"), (resourceVo, cacheData) -> resourceVo.getFcd());
        map.put(new ValueTextVo("lcu", "修改者"), (resourceVo, cacheData) -> {
            String lcu = resourceVo.getLcu();
            if (StringUtils.isNotBlank(lcu)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("uuid", lcu);
                resultObj.put("initType", "user");
                return resultObj;
            }
            return null;
        });
        map.put(new ValueTextVo("lcd", "修改日期"), (resourceVo, cacheData) -> resourceVo.getLcd());

        map.put(new ValueTextVo("maintenanceWindow", "维护窗口"), (resourceVo, cacheData) -> resourceVo.getMaintenanceWindow());
        map.put(new ValueTextVo("description", "描述"), (resourceVo, cacheData) -> resourceVo.getDescription());
        map.put(new ValueTextVo("networkArea", "网络区域"), (resourceVo, cacheData) -> resourceVo.getNetworkArea());

        map.put(new ValueTextVo("inspect", "巡检状态"), (resourceVo, cacheData) -> {
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
//        map.put(new ValueTextVo("inspectTime", "巡检时间"), (resourceVo, cacheData) -> resourceVo.getInspectTime());
        map.put(new ValueTextVo("monitor", "监控状态"), (resourceVo, cacheData) -> {
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
//        map.put(new ValueTextVo("monitorTime", "监控时间"), (resourceVo, cacheData) -> resourceVo.getMonitorTime());

//        map.put(new ValueTextVo("port", "端口"), (resourceVo, cacheData) -> resourceVo.getPort());
        map.put(new ValueTextVo("businessGroupList", "所属部门"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("allIpList", "IP列表"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("ownerList", "所有者"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("state", "资产状态"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("vendor", "厂商"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("dataCenter", "数据中心"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("appEnvironment", "应用环境"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("appModule", "应用模块"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("appSystem", "应用系统"), (resourceVo, cacheData) -> {
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
        map.put(new ValueTextVo("tagList", "标签"), (resourceVo, cacheData) -> resourceVo.getTagList());
        map.put(new ValueTextVo("accountList", "账号"), (resourceVo, cacheData) -> resourceVo.getAccountList());
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

//    @Override
    private JSONArray getAppResourceList2(Long appSystemId, Long appModuleId, Long envId, List<Long> typeIdList, Integer currentPage, Integer pageSize) {
        if (appSystemId != null && ciEntityCachedMapper.getCiEntityBaseInfoById(appSystemId) == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        if (appModuleId != null && ciEntityCachedMapper.getCiEntityBaseInfoById(appModuleId) == null) {
            throw new AppModuleNotFoundException(appModuleId);
        }
        if (CollectionUtils.isNotEmpty(typeIdList)) {
            for (Long typeId : typeIdList) {
                CiVo ciVo = ciMapper.getCiById(typeId);
                if (ciVo == null) {
                    throw new CiNotFoundException(typeId);
                }
            }
        }
        JSONArray tableList = new JSONArray();
        ApplicationListDisplayVo applicationListDisplay = resourceEntityMapper.getApplicationListDisplay();
        if (applicationListDisplay != null) {
            JSONObject config = applicationListDisplay.getConfig();
            if (MapUtils.isNotEmpty(config)) {
                JSONArray tableSettingList = config.getJSONArray("tableSettingList");
                if (CollectionUtils.isNotEmpty(tableSettingList)) {
                    List<String> ciNameList = new ArrayList<>();
                    Map<String, List<String>> ciName2FieldListMap = new HashMap<>();
                    for (int i = 0; i < tableSettingList.size(); i++) {
                        JSONObject tableObj = tableSettingList.getJSONObject(i);
                        if (MapUtils.isNotEmpty(tableObj)) {
                            String ciName = tableObj.getString("ciName");
                            ciNameList.add(ciName);
                            JSONArray fieldList = tableObj.getJSONArray("fieldList");
                            if (CollectionUtils.isNotEmpty(fieldList)) {
                                ciName2FieldListMap.put(ciName, fieldList.toJavaList(String.class));
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(ciNameList)) {
                        List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(ciNameList);
                        if (CollectionUtils.isEmpty(typeIdList) && CollectionUtils.isNotEmpty(resourceCiVoList)) {
                            List<CiVo> downwardCiList = ciMapper.getBatchDownwardCiListByCiList(resourceCiVoList);
                            for (CiVo downwardCi : downwardCiList) {
                                // 找出叶子节点模型
                                if (downwardCi.getRht() != null && downwardCi.getLft() != null && (downwardCi.getRht() - downwardCi.getLft() == 1)) {
                                    typeIdList.add(downwardCi.getId());
                                }
                            }
                        }
                        if (CollectionUtils.isNotEmpty(typeIdList)) {
                            ResourceSearchVo searchVo = new ResourceSearchVo();
                            searchVo.setAppSystemIdList(Collections.singletonList(appSystemId));
                            if (appModuleId != null) {
                                searchVo.setAppModuleIdList(Collections.singletonList(appModuleId));
                            }
                            if (envId != null) {
                                searchVo.setEnvIdList(Collections.singletonList(envId));
                            }
                            searchVo.setCurrentPage(currentPage);
                            searchVo.setPageSize(pageSize);
                            List<CiVo> ciList = ciMapper.getAllCi(typeIdList);
                            for (CiVo ciVo : ciList) {
                                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                                if (StringUtils.isNotBlank(resourceTypeName)) {
                                    ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName());
                                    searchVo.setTypeIdList(Collections.singletonList(ciVo.getId()));
                                    resourceCenterResourceService.assembleResourceSearchVo(searchVo, false);
                                    List<ResourceVo> resourceList = getResourceList(searchVo);
                                    if (CollectionUtils.isNotEmpty(resourceList)) {
                                        List<String> fieldList = ciName2FieldListMap.get(resourceTypeName);
                                        JSONObject tableObj = TableResultUtil.getResult(getTheadList(fieldList), resourceList, searchVo);
                                        tableObj.put("type", resourceTypeVo);
                                        tableList.add(tableObj);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return tableList;
    }

    @Override
    public JSONArray getAppResourceList(Long appSystemId, Long appModuleId, Long envId, List<Long> typeIdList, String viewName, Integer currentPage, Integer pageSize) {
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
            searchVo.setAppSystemId(appSystemId);
            if (appModuleId != null) {
                searchVo.setAppModuleId(appModuleId);
            }
            if (envId != null) {
                searchVo.setEnvId(envId);
            }
            searchVo.setCurrentPage(currentPage);
            searchVo.setPageSize(pageSize);
            for (String name : viewNameList) {
                ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
                if (resourceEntityVo != null) {
                    searchVo.setViewName(name);
                    List<ResourceVo> resourceList = getAppResourceList(searchVo, true);
                    if (CollectionUtils.isNotEmpty(resourceList)) {
                        List<String> fieldList = viewName2FieldListMap.get(name);
                        JSONArray theadList = getTheadList(fieldList);
                        JSONArray tbodyList = getTbodyList(fieldList, resourceList, resourceEntityVo);
                        JSONObject tableObj = TableResultUtil.getResult(theadList, tbodyList, searchVo);
//                                    tableObj.put("type", new JSONObject());
                        tableObj.put("viewName", name);
                        tableObj.put("viewLabel", resourceEntityVo.getLabel());
                        tableList.add(tableObj);
                    }
                }
            }
//            ResourceSearchVo searchVo = new ResourceSearchVo();
//            searchVo.setAppSystemId(appSystemId);
//            if (appModuleId != null) {
//                searchVo.setAppModuleId(appModuleId);
//            }
//            if (envId != null) {
//                searchVo.setEnvId(envId);
//            }
//            searchVo.setCurrentPage(currentPage);
//            searchVo.setPageSize(pageSize);
//            for (String name : viewNameList) {
//                ResourceEntityVo resourceEntityVo = resourceEntityMapper.getResourceEntityByName(name);
//                if (resourceEntityVo != null) {
//                    searchVo.setViewName(name);
//                    int rowNum = resourceMapper.getAppResourceCount(searchVo);
//                    if (rowNum > 0) {
//                        searchVo.setRowNum(rowNum);
//                        List<Long> resourceIdList = resourceMapper.getAppResourceIdList(searchVo);
//                        if (CollectionUtils.isNotEmpty(resourceIdList)) {
//                            searchVo.setIdList(resourceIdList);
//                            List<ResourceVo> resourceList = resourceMapper.getAppResourceListByIdList(searchVo);
//                            if (CollectionUtils.isNotEmpty(resourceList)) {
//                                List<String> fieldList = viewName2FieldListMap.get(name);
//                                JSONArray theadList = getTheadList(fieldList);
//                                JSONArray tbodyList = getTbodyList(fieldList, resourceList, resourceEntityVo);
//                                JSONObject tableObj = TableResultUtil.getResult(theadList, tbodyList, searchVo);
//                                tableObj.put("type", new JSONObject());
//                                tableObj.put("viewName", name);
//                                tableObj.put("viewLabel", resourceEntityVo.getLabel());
//                                tableList.add(tableObj);
//                            }
//                        }
//                    }
//                }
//            }
        }
        return tableList;
    }

    @Override
    public List<ResourceVo> getAppResourceList(ResourceSearchVo searchVo, boolean needPage) {
        List<ResourceVo> resultList = new ArrayList<>();
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
//                if (CollectionUtils.isNotEmpty(resourceIdList)) {
//                    searchVo.setIdList(resourceIdList);
//                    List<ResourceVo> resourceList = resourceMapper.getAppResourceListByIdList(searchVo);
//                    resultList.addAll(resourceList);
//                }
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
        for (ValueTextVo key : map.keySet()) {
            keyMap.put(key.getValue(), key);
        }
        JSONObject cacheData = new JSONObject();
        cacheData.put("resourceEntityVo", resourceEntityVo);
        //ci ip state vendor dataCenter appEnvironment appModule appSystem allIpList
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
        // businessGroupList ownerList
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
                    BiFunction<ResourceVo, JSONObject, Object> biFunction = map.get(key);
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
        List<ResourceVo> resultList = new ArrayList<>();
        List<Long> idList = resourceMapper.getResourceIdList(searchVo);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<ResourceVo> resourceList = resourceMapper.getResourceListByIdList(idList);
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
                int rowNum = 0;
                if (noFilterCondition(searchVo)) {
                    rowNum = resourceMapper.getAllResourceCount(searchVo);
                } else {
                    rowNum = resourceMapper.getResourceCount(searchVo);
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
        List<Long> ciIdList = resourceEntityMapper.getAllResourceTypeCiIdList();
        if (CollectionUtils.isEmpty(ciIdList)) {
            return resultList;
        }
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
//        if (CollectionUtils.isEmpty(fieldNameList)) {
//            fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
//        }
//        if (CollectionUtils.isNotEmpty(fieldNameList)) {
//            List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName("scence_ipobject_detail");
//            Map<Object, String> field2TitleMap = fieldList.stream().collect(Collectors.toMap(ValueTextVo::getValue, ValueTextVo::getText));
//            List<ValueTextVo> fieldAliasList = ResourceEntityFactory.getFieldAliasListByViewName("scence_ipobject_detail");
//            Map<Object, String> field2KeyMap = fieldAliasList.stream().collect(Collectors.toMap(ValueTextVo::getValue, ValueTextVo::getText));
//            for (String fieldName : fieldNameList) {
//                String title = field2TitleMap.get(fieldName);
//                String key = field2KeyMap.get(fieldName);
//                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(title)) {
//                    JSONObject thead = new JSONObject();
//                    thead.put("key", key);
//                    thead.put("title", title);
//                    theadList.add(thead);
//                }
//            }
//        }
        Map<Object, ValueTextVo> keyMap = new HashMap<>();
        for (ValueTextVo key : map.keySet()) {
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
        Set<ValueTextVo> keySet = map.keySet();
        List<ValueTextVo> resultList = new ArrayList<>(keySet);
        resultList.sort((o1, o2) -> o1.getText().compareToIgnoreCase(o2.getText()));
        return resultList;
    }

    @Override
    public List<ValueTextVo> getAppAssertAllTheadList() {
        List<ValueTextVo> resultList = new ArrayList<>();
        for (ValueTextVo valueTextVo : map.keySet()) {
            if (Objects.equals(valueTextVo.getValue(), "tagList")) {
                continue;
            }
            if (Objects.equals(valueTextVo.getValue(), "accountList")) {
                continue;
            }
            resultList.add(valueTextVo);
        }
        resultList.sort((o1, o2) -> o1.getText().compareToIgnoreCase(o2.getText()));
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
    public List<ResourceVo> getAppEnvListForSelect(BasePageVo searchVo) {
        List<ResourceVo> appEnvList = new ArrayList<>();
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            appEnvList = resourceMapper.searchAppEnvListByIdList(idList);
        } else {
            int rowNum = resourceMapper.searchAppEnvCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                if (searchVo.getNeedPage()) {
                    List<Long> idList = resourceMapper.searchAppEnvIdList(searchVo);
                    if (CollectionUtils.isNotEmpty(idList)) {
                        appEnvList = resourceMapper.searchAppEnvListByIdList(idList);
                    }
                } else {
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
    public List<AppEnvVo> getAppEnvListByAppSystemId(Long appSystemId) {
        List<AppEnvVo> appEnvList = resourceMapper.getAppEnvListByAppSystemId(appSystemId);
        appEnvList.sort(Comparator.comparing(AppEnvVo::getSeqNo));
        return appEnvList;
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
        Map<String, List<Long>> viewName2TypeIdListMap = new HashMap<>();
        List<ResourceEntityVo> appViewList = getAppViewList();
        if (CollectionUtils.isNotEmpty(appViewList)) {
            for (ResourceEntityVo resourceEntityVo : appViewList) {
                List<Long> typeIdList = resourceMapper.getAppResourceTypeIdListByViewNameAndAppSystemId(resourceEntityVo.getName(), appSystemId, null, null);
                viewName2TypeIdListMap.put(resourceEntityVo.getName(), typeIdList);
            }
        }
        return viewName2TypeIdListMap;
    }

    @Override
    public Map<String, List<Long>> getAppResourceTypeIdListByAppSystemIdAndAppModuleIdAndEnvId(Long appSystemId, Long appModuleId, Long envId) {
        Map<String, List<Long>> viewName2TypeIdListMap = new HashMap<>();
        List<ResourceEntityVo> appViewList = getAppViewList();
        if (CollectionUtils.isNotEmpty(appViewList)) {
            for (ResourceEntityVo resourceEntityVo : appViewList) {
                List<Long> typeIdList = resourceMapper.getAppResourceTypeIdListByViewNameAndAppSystemId(resourceEntityVo.getName(), appSystemId, appModuleId, envId);
                viewName2TypeIdListMap.put(resourceEntityVo.getName(), typeIdList);
            }
        }
        return viewName2TypeIdListMap;
    }

    private List<ResourceEntityVo> getAppViewList() {
        List<ResourceEntityVo> resultList = new ArrayList<>();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getResourceEntityList();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            SceneEntityVo sceneEntityVo = ResourceEntityFactory.getSceneEntityByViewName(resourceEntityVo.getName());
            if (sceneEntityVo == null) {
                String config = resourceEntityMapper.getResourceEntityConfigByName(resourceEntityVo.getName());
                if (StringUtils.isNotBlank(config)) {
                    ResourceEntityConfigVo resourceEntityConfigVo = JSONObject.parseObject(config, ResourceEntityConfigVo.class);
                    if (Objects.equals(resourceEntityConfigVo.getSceneTemplateName(), "")) {
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

    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo) {
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }


}
