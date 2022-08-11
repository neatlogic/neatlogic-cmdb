/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountComponentVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.ModuleVo;
import codedriver.framework.common.dto.BasePageVo;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface ResourceMapper extends IResourceCrossoverMapper {

    int getResourceCount(ResourceSearchVo searchVo);

    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    int getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<Long> getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<ResourceVo> getAppInstanceResourceListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    List<ResourceVo> getAppInstanceResourceListByIdListAndKeyword(@Param("idList") List<Long> idList, @Param("keyword") String keyword, @Param("schemaName") String schemaName);

    List<ResourceVo> getAppInstanceResourceListByIdListSimple(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    List<ResourceVo> getDbInstanceResourceListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    Long getResourceIdByIpAndPortAndName(ResourceSearchVo searchVo);

    Long getResourceIdByIpAndPortAndNameWithFilter(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    ResourceVo getResourceById(@Param("id") Long id, @Param("schemaName") String schemaName);

    List<ResourceVo> getResourceFromSoftwareServiceByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    int checkResourceIsExists(@Param("id") Long id, @Param("schemaName") String schemaName);

    List<Long> checkResourceIdListIsExists(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    List<Long> getHasModuleAppSystemIdListByAppSystemIdList(@Param("appSystemIdList") List<Long> appSystemIdList);

    List<Long> getHasEnvAppSystemIdListByAppSystemIdList(@Param("appSystemIdList") List<Long> appSystemIdList);

    int searchAppModuleCount(ResourceSearchVo searchVo);

    List<Long> searchAppModuleIdList(ResourceSearchVo searchVo);

    List<ResourceVo> searchAppModule(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    List<Long> getAppSystemModuleIdListByAppSystemId(@Param("appSystemId") Long appSystemId);

    List<Long> getAppSystemModuleIdListByAppSystemIdAndAppModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") JSONArray appModuleIdList);

    List<ModuleVo> getAppModuleListByAppSystemIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppModuleListByIdListSimple(@Param("idList") List<Long> idList);

    Set<Long> getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceListByResourceVoList(@Param("resourceList") List<ResourceVo> resourceList, @Param("schemaName") String schemaName);

    List<Long> getResourceIdListByAppSystemIdAndModuleIdAndEnvId(@Param("resourceVo") ResourceVo resourceVo, @Param("schemaName") String schemaName);

    List<Long> getAppInstanceResourceIdListByAppSystemIdAndModuleIdAndEnvId(@Param("resourceVo") ResourceVo resourceVo, @Param("schemaName") String schemaName);

    /**
     * 根据类型和IP列表查询资源
     *
     * @param schemaName
     * @param typeIdList
     * @param ipList
     * @return
     */
    List<ResourceVo> getResourceListByTypeIdListAndIpList(@Param("schemaName") String schemaName, @Param("typeIdList") List<Long> typeIdList, @Param("ipList") List<String> ipList);

    ResourceVo getResourceByIpAndPortAndNameAndTypeName(@Param("schemaName") String dataDbName, @Param("ip") String ip, @Param("port") Integer port, @Param("name") String nodeName, @Param("typeName") String nodeType);

    ResourceVo getResourceByIpAndPort(@Param("schemaName") String dataDbName, @Param("ip") String ip, @Param("port") Integer port);

    ResourceVo getAppSystemById(@Param("id") Long id, @Param("schemaName") String schemaName);

    ResourceVo getAppSystemByName(@Param("name") String name, @Param("schemaName") String schemaName);

    ResourceVo getAppModuleById(@Param("id") Long id, @Param("schemaName") String schemaName);

    ResourceVo getAppModuleByName(@Param("name") String name, @Param("schemaName") String schemaName);

    ResourceVo getAppEnvById(@Param("id") Long id, @Param("schemaName") String schemaName);

    ResourceVo getAppEnvByName(@Param("name") String name, @Param("schemaName") String schemaName);

    List<AppEnvironmentVo> getAllAppEnv(@Param("schemaName") String schemaName);

    List<AccountComponentVo> searchAccountComponent(@Param("accountComponentVo") AccountComponentVo accountComponentVo, @Param("schemaName") String schemaName);

    Integer searchAccountComponentCount(@Param("accountComponentVo") AccountComponentVo accountComponentVo, @Param("schemaName") String schemaName);

    int searchAppEnvCount(BasePageVo searchVo);

    List<Long> searchAppEnvIdList(BasePageVo searchVo);

    List<ResourceVo> searchAppEnvListByIdList(List<Long> idList);

    int searchAppSystemCount(BasePageVo searchVo);

    List<Long> searchAppSystemIdList(BasePageVo searchVo);

    List<ResourceVo> searchAppSystemListByIdList(List<Long> idList);

    int searchStateCount(BasePageVo searchVo);

    List<Long> searchStateIdList(BasePageVo searchVo);

    List<ResourceVo> searchStateListByIdList(List<Long> idList);
}
