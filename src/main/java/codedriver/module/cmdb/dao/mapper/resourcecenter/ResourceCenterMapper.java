/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.TagVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author linbq
 * @since 2021/5/27 16:34
 **/
public interface ResourceCenterMapper {
    int getResourceCount(ResourceVo searchVo);

    List<Long> getResourceIdList(ResourceVo searchVo);

    List<ResourceVo> getResourceListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName);

    Long getResourceIdByIpAndPort(ResourceVo resourceVo);

    ResourceVo getResourceIpPortById(@Param("id") Long id, @Param("schemaName") String schemaName);

    List<String> getTagNameListByResourceId(Long resourceId);

    int getAccountCount(AccountVo searchVo);

    int searchAccountCount(AccountVo searchVo);

    List<AccountVo> searchAccount(AccountVo searchVo);

    List<AccountVo> getAccountListForSelect(AccountVo searchVo);

    int getTagCount(TagVo searchVo);

    List<TagVo> getTagListForSelect(TagVo searchVo);

    List<Long> getAccountIdListByAccountAndProtocol(@Param("account") String account, @Param("protocol") String protocol);

    List<Long> getNoCorrespondingAccountResourceIdListByTagListAndAccountIdAndProtocol(@Param("tagList") List<Long> tagList, @Param("account") String account, @Param("protocol") String protocol);

    Long checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(@Param("resourceId") Long resourceId, @Param("account") String account, @Param("protocol") String protocol);
}
