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
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.Ordered;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import neatlogic.module.cmdb.utils.ResourceEntityFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultResourceCenterDataSourceImpl implements IResourceCenterDataSource {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public Ordered getOrdered() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public JSONArray getAppResourceList(Long appSystemId, Long appModuleId, Long envId, List<Long> typeIdList, Integer currentPage, Integer pageSize) {
        if (appSystemId != null && ciEntityMapper.getCiEntityBaseInfoById(appSystemId) == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        if (appModuleId != null && ciEntityMapper.getCiEntityBaseInfoById(appModuleId) == null) {
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
    public JSONArray getTheadList(List<String> fieldNameList) {
        JSONArray theadList = new JSONArray();
        if (CollectionUtils.isNotEmpty(fieldNameList)) {
            fieldNameList = ResourceEntityFactory.getFieldNameListByViewName("scence_ipobject_detail");
        }
        if (CollectionUtils.isNotEmpty(fieldNameList)) {
            List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName("scence_ipobject_detail");
            Map<Object, String> field2TitleMap = fieldList.stream().collect(Collectors.toMap(ValueTextVo::getValue, ValueTextVo::getText));
            List<ValueTextVo> fieldAliasList = ResourceEntityFactory.getFieldAliasListByViewName("scence_ipobject_detail");
            Map<Object, String> field2KeyMap = fieldAliasList.stream().collect(Collectors.toMap(ValueTextVo::getValue, ValueTextVo::getText));
            for (String fieldName : fieldNameList) {
                String title = field2TitleMap.get(fieldName);
                String key = field2KeyMap.get(fieldName);
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(title)) {
                    JSONObject thead = new JSONObject();
                    thead.put("key", key);
                    thead.put("title", title);
                    theadList.add(thead);
                }
            }
        }
        return theadList;
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
