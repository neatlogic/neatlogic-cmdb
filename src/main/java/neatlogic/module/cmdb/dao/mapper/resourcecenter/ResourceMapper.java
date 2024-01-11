/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
import java.util.Set;

public interface ResourceMapper extends IResourceCrossoverMapper {

    int getResourceCountByNameKeyword(ResourceSearchVo searchVo);

    int getResourceCountByIpKeyword(ResourceSearchVo searchVo);

    int getResourceCount(ResourceSearchVo searchVo);

    int getResourceCountByDynamicCondition(@Param("searchVo") ResourceSearchVo searchVo, @Param("conditionSql") String conditionSql);

    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    List<Long> getResourceIdListByDynamicCondition(@Param("searchVo") ResourceSearchVo searchVo, @Param("conditionSql") String conditionSql);

    List<ResourceVo> getResourceListByIdList(List<Long> idList);

    int getIpObjectResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<Long> getIpObjectResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    int getOsResourceCountByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<Long> getOsResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    int getOsResourceCountByAppSystemIdAndAppModuleIdListAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<Long> getOsResourceIdListByAppSystemIdAndAppModuleIdListAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<ResourceVo> getAppInstanceResourceListByIdList(@Param("idList") List<Long> idList);

    List<ResourceVo> getAppInstanceResourceListByIdListAndKeyword(@Param("idList") List<Long> idList, @Param("keyword") String keyword);

    List<ResourceVo> getAppInstanceResourceListByIdListSimple(List<Long> idList);

    List<ResourceVo> getDbInstanceResourceListByIdList(List<Long> idList);

    List<ResourceVo> getOsResourceListByIdList(List<Long> idList);

    Long getResourceIdByIpAndPortAndName(ResourceSearchVo searchVo);

    Long getResourceIdByIpAndPortAndNameWithFilter(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceByIdList(List<Long> idList);

    List<ResourceVo> getAuthResourceList(ResourceSearchVo searchVo);

    ResourceVo getResourceById(Long id);

    int checkResourceIsExists(Long id);

    List<Long> checkResourceIdListIsExists(List<Long> idList);

    List<Long> getHasModuleAppSystemIdListByAppSystemIdList(@Param("appSystemIdList") List<Long> appSystemIdList);

    List<Long> getHasEnvAppSystemIdListByAppSystemIdList(@Param("appSystemIdList") List<Long> appSystemIdList);

    int searchAppModuleCount(ResourceSearchVo searchVo);

    List<Long> searchAppModuleIdList(ResourceSearchVo searchVo);

    List<ResourceVo> searchAppModule(List<Long> idList);

    List<Long> getAppSystemModuleIdListByAppSystemId(Long appSystemId);

    List<Long> getAppSystemModuleIdListByAppSystemIdAndAppModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") JSONArray appModuleIdList);

    List<Long> getAppSystemModuleIdListByAppSystemIdAndAppModuleIdListAndEnvId(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId, @Param("appModuleIdList") JSONArray appModuleIdList);

    List<ModuleVo> getAppModuleListByAppSystemIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppModuleListByIdListSimple(@Param("idList") List<Long> idList, @Param("needOrder") boolean needOrder);

    Set<Long> getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(ResourceSearchVo searchVo);

    Set<Long> getOsResourceTypeIdListByAppModuleIdAndEnvId(ResourceSearchVo searchVo);

    Set<Long> getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(ResourceSearchVo searchVo);

    Set<Long> getOsResourceTypeIdListByAppSystemIdAndEnvId(ResourceSearchVo searchVo);

    Set<Long> getResourceAppSystemIdListByResourceId(Long id);

    List<ResourceVo> getResourceAppSystemListByResourceIdList(List<Long> id);

    List<ResourceVo> getResourceListByResourceVoList(@Param("resourceList") List<ResourceVo> resourceList,@Param("searchVo") ResourceSearchVo searchVo);

    Long getAppSystemIdByResourceId(Long id);

    Set<Long> getResourceTypeIdListByAppSystemIdAndModuleIdAndEnvIdAndInspectStatusList(ResourceSearchVo searchVo);

    List<Long> getResourceIdListByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);

    List<Long> getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);

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

    List<Long> searchAppSystemIdList(BasePageVo searchVo);

    List<ResourceVo> searchAppSystemListByIdList(List<Long> idList);

    int searchStateCount(BasePageVo searchVo);

    List<Long> searchStateIdList(BasePageVo searchVo);

    List<ResourceVo> searchStateListByIdList(List<Long> idList);

    int searchVendorCount(BasePageVo searchVo);

    List<Long> searchVendorIdList(BasePageVo searchVo);

    List<ResourceVo> searchVendorListByIdList(List<Long> idList);

    int getAppSystemIdListCountByKeyword(String keyword);

    List<Long> getAppSystemIdListByKeyword(BasePageVo searchVo);

    List<AppSystemVo> getAppSystemListByIdList(List<Long> appSystemIdList);

    List<AppModuleVo> getAppModuleListByKeywordAndAppSystemIdList(@Param("keyword") String keyword, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<AppModuleVo> getAppModuleListByAppSystemId(Long appSystemId);

    int getAppSystemCountByKeyword(BasePageVo searchVo);

    List<AppSystemVo> getAppSystemListByKeyword(BasePageVo searchVo);

    List<AppEnvVo> getOsEnvListByAppSystemIdAndTypeId(ResourceSearchVo searchVo);

    List<AppEnvVo> getIpObjectEnvListByAppSystemIdAndTypeId(ResourceSearchVo searchVo);

    List<AppEnvVo> getAppEnvListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<Map<String, Long>> getAppEnvCountMapByAppSystemIdGroupByAppModuleId(Long appSystemId);

    List<ResourceVo> getOsResourceListenPortListByResourceIdList(List<Long> resourceIdList);

    List<ResourceVo> getSoftwareResourceListenPortListByResourceIdList(List<Long> resourceIdList);

    List<SoftwareServiceOSVo> getOsResourceListByResourceIdList(List<Long> resourceIdList);

    List<Long> getResourceTypeIdListByAuth(ResourceSearchVo searchVo);
}
