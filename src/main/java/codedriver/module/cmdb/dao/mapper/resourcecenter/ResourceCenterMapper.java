/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnviromentVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.StatusVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author linbq
 * @since 2021/5/27 16:34
 **/
public interface ResourceCenterMapper {

    int getResourceCount(ResourceSearchVo searchVo);

    List<Long> getResourceIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    Long getResourceIdByIpAndPortAndName(ResourceSearchVo searchVo);

    ResourceVo getResourceIpPortById(@Param("id") Long id, @Param("schemaName") String schemaName);

    List<String> getTagNameListByResourceId(Long resourceId);

    int getAccountCount(AccountVo searchVo);

    AccountVo getAccountById(Long id);

    int checkAccountNameIsRepeats(AccountVo vo);

    int checkAccountHasBeenReferredById(Long id);

    int searchAccountCount(AccountVo searchVo);

    List<AccountVo> searchAccount(AccountVo searchVo);

    List<AccountVo> getAccountListForSelect(AccountVo searchVo);

    int getTagCount(TagVo searchVo);

    List<TagVo> getTagListForSelect(TagVo searchVo);

    List<TagVo> searchTag(TagVo vo);

    int searchTagCount(TagVo vo);

    int checkTagNameIsRepeats(TagVo vo);

    int checkTagIsExistsById(Long id);

    TagVo getTagById(Long id);

    int checkTagHasBeenReferredById(Long id);

    List<Long> getAccountIdListByAccountAndProtocol(@Param("account") String account, @Param("protocol") String protocol);

    List<Long> getNoCorrespondingAccountResourceIdListByTagListAndAccountIdAndProtocol(@Param("tagList") List<Long> tagList, @Param("account") String account, @Param("protocol") String protocol);

    Long checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(@Param("resourceId") Long resourceId, @Param("account") String account, @Param("protocol") String protocol);

    int getAppModuleCount(ResourceSearchVo searchVo);

    List<Long> getAppModuleIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppModuleListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    int getAppSystemCount(ResourceSearchVo searchVo);

    List<Long> getAppSystemIdList(ResourceSearchVo searchVo);

    List<ResourceVo> getAppSystemListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    int checkAppModuleIsExists(@Param("id") Long id, @Param("schemaName") String schemaName);

    List<AppEnviromentVo> getEnvironmentListByAppModuleId(@Param("appModuleId") Long appModuleId, @Param("schemaName") String schemaName);

    List<ResourceTypeVo> getResourceTypeListByAppModuleIdAndEnvId(@Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("schemaName") String schemaName);

    List<ResourceTypeVo> getResourceTypeList(@Param("schemaName") String schemaName);

    List<StatusVo> getStatusList(@Param("schemaName") String schemaName);

    List<Long> getResourceIdListByProtocolList(ResourceSearchVo searchVo);

    List<Long> getResourceIdListByTagIdList(ResourceSearchVo searchVo);

    int updateAccount(AccountVo vo);

    int updateTag(TagVo vo);

    int insertAccount(AccountVo vo);

    int insertTag(TagVo vo);

    int deleteTagById(Long id);

    int deleteAccountById(Long id);
}
