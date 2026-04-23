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

package neatlogic.module.cmdb.dao.mapper.resourcecenter;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.ModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.SoftwareServiceOSVo;
import neatlogic.framework.common.dto.BasePageVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ResourceMapper extends IResourceCrossoverMapper {

    int getCountBySql(String sql);

    Long getIdBySql(String sql);

    List<Long> getIdListBySql(String sql);

    ResourceVo getResourceBySql(String sql);

    ResourceVo getResourceSimpleBySql(String sql);

    List<ResourceVo> getResourceListBySql(String sql);

    List<ResourceVo> getResourceSimpleListBySql(String sql);

    List<AccountComponentVo> searchAccountComponentListBySql(String sql);

    List<AppEnvVo> getAppEnvListBySql(String sql);

    List<Map<String, Object>> getMapListBySql(String sql);
    @Deprecated
    int getResourceCountByNameKeyword(ResourceSearchVo searchVo);
    @Deprecated
    int getResourceCountByIpKeyword(ResourceSearchVo searchVo);
    @Deprecated
    int getResourceCount(ResourceSearchVo searchVo);

    @Deprecated
    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    @Deprecated
    List<ResourceVo> getResourceListByIdList(List<Long> idList);

    int getAppResourceCount(ResourceSearchVo searchVo);

    List<Long> getAppResourceIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppResourceListByIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppInstanceResourceListByIdListSimple(List<Long> idList);

    List<ResourceVo> getResourceByIdList(List<Long> idList);

    List<ResourceVo> getAuthResourceList(ResourceSearchVo searchVo);

    ResourceVo getResourceById(Long id);

    Long getResourceIdByResourceId(Long id);

    List<Long> checkResourceIdListIsExists(List<Long> idList);

    List<Long> getHasModuleAppSystemIdListByAppSystemIdList(@Param("appSystemIdList") List<Long> appSystemIdList);

    int searchAppModuleCount(ResourceSearchVo searchVo);

    List<Long> searchAppModuleIdList(ResourceSearchVo searchVo);

    List<ResourceVo> searchAppModule(List<Long> idList);

    List<Long> getAppSystemModuleIdListByAppSystemId(Long appSystemId);

    List<Long> getAppSystemModuleIdListByAppSystemIdAndAppModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") JSONArray appModuleIdList);

    List<ModuleVo> getAppModuleListByAppSystemIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppModuleListByIdListSimple(@Param("idList") List<Long> idList, @Param("needOrder") boolean needOrder);

    List<Long> getResourceIdListByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);

    /**
     * 根据类型和IP列表查询资源
     *
     * @param typeIdList
     * @param ipList
     * @return
     */
    List<ResourceVo> getResourceListByTypeIdListAndIpList(@Param("typeIdList") List<Long> typeIdList, @Param("ipList") List<String> ipList);

    ResourceVo getResourceByIpAndPortAndNameAndTypeName(@Param("ip") String ip, @Param("port") Integer port, @Param("name") String nodeName, @Param("typeName") String nodeType);

    ResourceVo getResourceByIpAndPort(@Param("ip") String ip, @Param("port") Integer port);

    ResourceVo getOSByIp(String ip);

    List<ResourceVo> getOSByIdList(List<Long> idList);

    ResourceVo getAppSystemById(Long id);

    ResourceVo getAppSystemByName(String name);

    ResourceVo getAppModuleById(Long id);

    ResourceVo getAppModuleByName(String name);

    ResourceVo getAppEnvById(Long id);

    List<ResourceVo> getAppEnvListByIdList(List<Long> idList);

    ResourceVo getAppEnvByName(String name);

    List<AppEnvironmentVo> getAllAppEnv();

    List<AccountComponentVo> searchAccountComponent(AccountComponentVo accountComponentVo);

    Integer searchAccountComponentCount(AccountComponentVo accountComponentVo);

    int searchAppEnvCount(BasePageVo searchVo);

    List<Long> searchAppEnvIdList(BasePageVo searchVo);

    List<ResourceVo> searchAppEnvListByIdList(List<Long> idList);

    int searchAppSystemCount(BasePageVo searchVo);

    /**
     * 根据限定应用范围统计应用数量。
     *
     * @param keyword 关键字
     * @param appSystemIdList 可见应用id列表
     * @return 应用数量
     */
    int searchAppSystemCountByIdList(@Param("keyword") String keyword, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<Long> searchAppSystemIdList(BasePageVo searchVo);

    /**
     * 根据限定应用范围分页查询应用id列表。
     *
     * @param searchVo 查询条件
     * @param appSystemIdList 可见应用id列表
     * @return 应用id列表
     */
    List<Long> searchAppSystemIdListByIdList(@Param("searchVo") BasePageVo searchVo, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<ResourceVo> searchAppSystemListByIdList(List<Long> idList);

    int searchStateCount(BasePageVo searchVo);

    List<Long> searchStateIdList(BasePageVo searchVo);

    List<ResourceVo> searchStateListByIdList(List<Long> idList);

    int searchVendorCount(BasePageVo searchVo);

    List<Long> searchVendorIdList(BasePageVo searchVo);

    List<ResourceVo> searchVendorListByIdList(List<Long> idList);

    int getAppSystemIdListCountByKeyword(String keyword);

    /**
     * 根据关键字和限定应用范围统计应用树数量。
     *
     * @param keyword 关键字
     * @param appSystemIdList 可见应用id列表
     * @return 应用数量
     */
    int getAppSystemIdListCountByKeywordAndIdList(@Param("keyword") String keyword, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<Long> getAppSystemIdListByKeyword(BasePageVo searchVo);

    /**
     * 根据关键字和限定应用范围分页查询应用树id列表。
     *
     * @param searchVo 查询条件
     * @param appSystemIdList 可见应用id列表
     * @return 应用树id列表
     */
    List<Long> getAppSystemIdListByKeywordAndIdList(@Param("searchVo") BasePageVo searchVo, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<AppSystemVo> getAppSystemListByIdList(List<Long> appSystemIdList);

    Long getAppSystemLimitMaxId(BasePageVo searchVo);

    List<AppModuleVo> getAppModuleListByKeywordAndAppSystemIdList(@Param("keyword") String keyword, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<AppModuleVo> getAppModuleListByAppSystemId(Long appSystemId);

    int getAppSystemCountByKeyword(BasePageVo searchVo);

    List<AppSystemVo> getAppSystemListByKeyword(BasePageVo searchVo);

    List<AppEnvVo> getAppEnvListByViewNameAndAppSystemIdAndAppModuleIdAndInspectStatusList(@Param("viewName") String viewName, @Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("inspectStatusList") List<String> inspectStatusList);

    List<AppEnvVo> getAppEnvListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<Map<String, Long>> getAppEnvCountMapByAppSystemIdGroupByAppModuleId(Long appSystemId);

    List<SoftwareServiceOSVo> getOsResourceListByResourceIdList(List<Long> resourceIdList);

    List<Long> getResourceTypeIdListByAuth(ResourceSearchVo searchVo);

    List<Long> getAppResourceTypeIdListByViewNameAndAppSystemId(
            @Param("viewName") String viewName,
            @Param("appSystemId") Long appSystemId,
            @Param("appModuleId") Long appModuleId,
            @Param("envId") Long envId,
            @Param("inspectStatusList") List<String> inspectStatusList
    );

    List<Long> getAppSystemIdListById(@Param("viewName") String viewName, @Param("id") Long id);
}
