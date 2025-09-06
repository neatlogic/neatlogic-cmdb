/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

    int getAllResourceCount(ResourceSearchVo searchVo);

    @Deprecated
    int getResourceCountByDynamicCondition(@Param("searchVo") ResourceSearchVo searchVo, @Param("conditionSql") String conditionSql);

    @Deprecated
    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    @Deprecated
    List<Long> getResourceIdListByDynamicCondition(@Param("searchVo") ResourceSearchVo searchVo, @Param("conditionSql") String conditionSql);
    @Deprecated
    List<ResourceVo> getResourceListByIdList(List<Long> idList);

    @Deprecated
    int getAppResourceCount(ResourceSearchVo searchVo);

    @Deprecated
    List<Long> getAppResourceIdList(ResourceSearchVo searchVo);

    @Deprecated
    List<ResourceVo> getAppResourceListByIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppInstanceResourceListByIdListSimple(List<Long> idList);

    @Deprecated
    Long getResourceIdByIpAndPortAndName(ResourceSearchVo searchVo);

    @Deprecated
    List<Long> getResourceIdListByIpAndPortAndName(ResourceSearchVo searchVo);

    @Deprecated
    List<ResourceVo> getResourceListByIpAndPortAndName(ResourceSearchVo searchVo);

//    Long getResourceIdByIpAndPortAndNameWithFilter(ResourceSearchVo searchVo);

    @Deprecated
    List<ResourceVo> getResourceListByIpAndPortAndNameWithFilter(ResourceSearchVo searchVo);

    @Deprecated
    List<ResourceVo> getResourceByIdList(List<Long> idList);

    @Deprecated
    List<ResourceVo> getAuthResourceList(ResourceSearchVo searchVo);

    @Deprecated
    ResourceVo getResourceById(Long id);

    @Deprecated
    Long getResourceIdByResourceId(Long id);

    @Deprecated
    List<Long> checkResourceIdListIsExists(List<Long> idList);

    List<Long> getHasModuleAppSystemIdListByAppSystemIdList(@Param("appSystemIdList") List<Long> appSystemIdList);

    int searchAppModuleCount(ResourceSearchVo searchVo);

    List<Long> searchAppModuleIdList(ResourceSearchVo searchVo);

    List<ResourceVo> searchAppModule(List<Long> idList);

    List<Long> getAppSystemModuleIdListByAppSystemId(Long appSystemId);

    List<Long> getAppSystemModuleIdListByAppSystemIdAndAppModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") JSONArray appModuleIdList);

    List<ModuleVo> getAppModuleListByAppSystemIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppModuleListByIdListSimple(@Param("idList") List<Long> idList, @Param("needOrder") boolean needOrder);
    // 该SQL语句可以使用 getResourceListByIpAndPortAndName 代替
    List<ResourceVo> getResourceListByResourceVoList(@Param("resourceList") List<ResourceVo> resourceList,@Param("searchVo") ResourceSearchVo searchVo);

//    Set<Long> getResourceTypeIdListByAppSystemIdAndModuleIdAndEnvIdAndInspectStatusList(ResourceSearchVo searchVo);

    @Deprecated
    List<Long> getResourceIdListByAppSystemIdAndModuleIdAndEnvId(ResourceVo resourceVo);

    /**
     * 根据类型和IP列表查询资源
     *
     * @param typeIdList
     * @param ipList
     * @return
     */
    @Deprecated
    List<ResourceVo> getResourceListByTypeIdListAndIpList(@Param("typeIdList") List<Long> typeIdList, @Param("ipList") List<String> ipList);

    @Deprecated
    ResourceVo getResourceByIpAndPortAndNameAndTypeName(@Param("ip") String ip, @Param("port") Integer port, @Param("name") String nodeName, @Param("typeName") String nodeType);

    @Deprecated
    ResourceVo getResourceByIpAndPort(@Param("ip") String ip, @Param("port") Integer port);

    ResourceVo getOSByIp(String ip);

    ResourceVo getAppSystemById(Long id);

    ResourceVo getAppSystemByName(String name);

    ResourceVo getAppModuleById(Long id);

    ResourceVo getAppModuleByName(String name);

    ResourceVo getAppEnvById(Long id);

    List<ResourceVo> getAppEnvListByIdList(List<Long> idList);

    ResourceVo getAppEnvByName(String name);

    List<AppEnvironmentVo> getAllAppEnv();

    @Deprecated
    List<AccountComponentVo> searchAccountComponent(AccountComponentVo accountComponentVo);

    @Deprecated
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

    Long getAppSystemLimitMaxId(BasePageVo searchVo);

    List<AppModuleVo> getAppModuleListByKeywordAndAppSystemIdList(@Param("keyword") String keyword, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<AppModuleVo> getAppModuleListByAppSystemId(Long appSystemId);

    int getAppSystemCountByKeyword(BasePageVo searchVo);

    List<AppSystemVo> getAppSystemListByKeyword(BasePageVo searchVo);

    @Deprecated
    List<AppEnvVo> getAppEnvListByViewNameAndAppSystemIdAndAppModuleIdAndInspectStatusList(@Param("viewName") String viewName, @Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("inspectStatusList") List<String> inspectStatusList);

    @Deprecated
    List<AppEnvVo> getAppEnvListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    @Deprecated
    List<Map<String, Long>> getAppEnvCountMapByAppSystemIdGroupByAppModuleId(Long appSystemId);

    List<SoftwareServiceOSVo> getOsResourceListByResourceIdList(List<Long> resourceIdList);

    @Deprecated
    List<Long> getResourceTypeIdListByAuth(ResourceSearchVo searchVo);

    @Deprecated
    List<Long> getAppResourceTypeIdListByViewNameAndAppSystemId(
            @Param("viewName") String viewName,
            @Param("appSystemId") Long appSystemId,
            @Param("appModuleId") Long appModuleId,
            @Param("envId") Long envId,
            @Param("inspectStatusList") List<String> inspectStatusList
    );

    @Deprecated
    List<Long> getAppSystemIdListById(@Param("viewName") String viewName, @Param("id") Long id);
}
