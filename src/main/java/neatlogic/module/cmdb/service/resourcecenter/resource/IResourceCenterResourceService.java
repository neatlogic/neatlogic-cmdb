/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.service.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.common.dto.BasePageVo;

import java.util.List;
import java.util.Map;


/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
public interface IResourceCenterResourceService {

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj);

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj,boolean isIncludeSon);

    void assembleResourceSearchVo(ResourceSearchVo searchVo, boolean isIncludeSon);

    /**
     * 处理批量搜索关键字
     * @param resourceSearchVo
     */
    void handleBatchSearchList(ResourceSearchVo resourceSearchVo);

    /**
     * 设置ipFieldAttrId字段和nameFieldAttrId字段
     * @param resourceSearchVo
     */
    void setIpFieldAttrIdAndNameFieldAttrId(ResourceSearchVo resourceSearchVo);

    /**
     * 设置isIpFieldSort字段和isNameFieldSort字段
     * @param resourceSearchVo
     */
    void setIsIpFieldSortAndIsNameFieldSort(ResourceSearchVo resourceSearchVo);

    /**
     * 查询所有后代模型的id列表
     *
     * @param idList
     * @return
     */
    List<Long> getDownwardCiIdListByCiIdList(List<Long> idList);

    /**
     * 获取资产的账号信息
     *
     * @param idList
     */
    Map<Long, List<AccountVo>> getResourceAccountByResourceIdList(List<Long> idList);

    /**
     * 获取资产的标签信息
     *
     * @param idList 资产id列表
     * @return map<资产id ， 标签列表>
     */
    Map<Long, List<TagVo>> getResourceTagByResourceIdList(List<Long> idList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagAndAccountInformation(List<ResourceVo> resourceList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagInformation(List<ResourceVo> resourceList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addAccountInformation(List<ResourceVo> resourceList);

    int getResourceCount(ResourceSearchVo searchVo);

    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceListByIdList(List<Long> idList);

    List<ResourceVo> getResourceListByIdList(List<Long> idList, List<String> selectFieldNameList);

    int getResourceCountByNameKeyword(ResourceSearchVo searchVo);

    int getResourceCountByIpKeyword(ResourceSearchVo searchVo);

    int getAppResourceCount(ResourceSearchVo searchVo);

    List<Long> getAppResourceIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppResourceListByIdList(ResourceSearchVo searchVo);

//    Long getResourceIdByIpAndPortAndName(ResourceSearchVo searchVo);

//    List<Long> getResourceIdListByIpAndPortAndName(ResourceSearchVo searchVo);

//    List<ResourceVo> getResourceListByIpAndPortAndName(ResourceSearchVo searchVo);

//    List<ResourceVo> getResourceListByIpAndPortAndNameWithFilter(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceByIdList(List<Long> idList);

    List<ResourceVo> getAuthResourceList(ResourceSearchVo searchVo);

    ResourceVo getResourceById(Long id);

    Long getResourceIdByResourceId(Long id);

    List<Long> checkResourceIdListIsExists(List<Long> idList);

    List<Long> getResourceIdListByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);

    List<ResourceVo> getResourceListByTypeIdListAndIpList(List<Long> typeIdList, List<String> ipList);

    ResourceVo getResourceByIpAndPortAndNameAndTypeName(String ip, Integer port, String name, String typeName);

    ResourceVo getResourceByIpAndPort(String ip, Integer port);

    List<AccountComponentVo> searchAccountComponent(AccountComponentVo accountComponentVo);

    int searchAccountComponentCount(AccountComponentVo accountComponentVo);

    List<AppEnvVo> getAppEnvListByViewNameAndAppSystemIdAndAppModuleIdAndInspectStatusList(String viewName, Long appSystemId, Long appModuleId, List<String> inspectStatusList);

    List<AppEnvVo> getAppEnvListByAppSystemIdAndAppModuleId(Long appSystemId, Long appModuleId);

    List<Map<String, Long>> getAppEnvCountMapByAppSystemIdGroupByAppModuleId(Long appSystemId);

    List<Long> getResourceTypeIdListByAuth(ResourceSearchVo searchVo);

    List<Long> getAppResourceTypeIdListByViewNameAndAppSystemId(
            String viewName,
            Long appSystemId,
            Long appModuleId,
            Long envId,
            List<String> inspectStatusList
    );

    List<Long> getAppSystemIdListById(String viewName, Long id);

    int searchVendorCount(BasePageVo searchVo);

    List<Long> searchVendorIdList(BasePageVo searchVo);

    List<ResourceVo> searchVendorListByIdList(List<Long> idList);

    int searchStateCount(BasePageVo searchVo);

    List<Long> searchStateIdList(BasePageVo searchVo);

    List<ResourceVo> searchStateListByIdList(List<Long> idList);

}
