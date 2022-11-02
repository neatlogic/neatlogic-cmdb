/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.ModuleVo;
import codedriver.framework.common.dto.BasePageVo;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface ResourceMapper extends IResourceCrossoverMapper {

    int getResourceCountByNameKeyword(ResourceSearchVo searchVo);

    int getResourceCountByIpKeyword(ResourceSearchVo searchVo);

    int getResourceCount(ResourceSearchVo searchVo);

    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceListByIdList(List<Long> idList);

    int getIpObjectResourceCountByAppModuleIdAndTypeIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<Long> getIpObjectResourceIdListByAppModuleIdAndTypeIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<ResourceVo> getAppInstanceResourceListByIdList(List<Long> idList);

    List<ResourceVo> getAppInstanceResourceListByIdListAndKeyword(@Param("idList") List<Long> idList, @Param("keyword") String keyword);

    List<ResourceVo> getAppInstanceResourceListByIdListSimple(List<Long> idList);

    List<ResourceVo> getDbInstanceResourceListByIdList(List<Long> idList);

    Long getResourceIdByIpAndPortAndName(ResourceSearchVo searchVo);

    Long getResourceIdByIpAndPortAndNameWithFilter(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceByIdList(List<Long> idList);

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

    List<ModuleVo> getAppModuleListByAppSystemIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppModuleListByIdListSimple(@Param("idList") List<Long> idList, @Param("needOrder") boolean needOrder);

    Set<Long> getIpObjectResourceTypeIdListByAppModuleIdAndEnvId(ResourceSearchVo searchVo);

    Set<Long> getIpObjectResourceTypeIdListByAppSystemIdAndEnvId(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceListByResourceVoList(@Param("resourceList") List<ResourceVo> resourceList);

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

    int getAppSystemIdListCountByKeyword(String keyword);

    List<Long> getAppSystemIdListByKeyword(BasePageVo searchVo);

    List<AppSystemVo> getAppSystemListByIdList(List<Long> appSystemIdList);

    List<AppModuleVo> getAppModuleListByKeywordAndAppSystemIdList(@Param("keyword") String keyword, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<AppModuleVo> getAppModuleListByAppSystemId(Long appSystemId);
}
