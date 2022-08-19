/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.resourcecenter;

import codedriver.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResourceAccountMapper extends IResourceAccountCrossoverMapper {

    int getAccountCount(AccountVo searchVo);

    AccountVo getAccountById(Long id);

    List<AccountVo> getAccountListByIdList(List<Long> accountIdList);

    int checkAccountNameIsRepeats(AccountVo vo);

    int searchAccountCount(AccountVo searchVo);

    List<AccountVo> searchAccount(AccountVo searchVo);

    List<AccountVo> getAccountListForSelect(AccountVo searchVo);

    List<Long> getAccountIdListByAccountAndProtocol(@Param("account") String account, @Param("protocol") String protocol);

    List<Long> checkAccountIdListIsExists(List<Long> idList);

    AccountVo getResourceAccountByIpAndPort(@Param("host") String host, @Param("port") Integer port);

    AccountVo getAccountByTagentIpAndPort(@Param("ip") String ip, @Param("port") Integer port);

    List<AccountVo> getAccountListByIpList(@Param("ipList") List<String> ipList);

    List<AccountVo> getResourceAccountListByResourceId(@Param("resourceId") Long resourceId, @Param("protocol") String protocol);

    List<AccountVo> getAllAccountList();

    List<AccountProtocolVo> searchAccountProtocolListByProtocolName(AccountProtocolVo searchVo);

    int checkAccountProtocolIsRepeats(AccountProtocolVo searchVo);

    List<AccountTagVo> getAccountTagListByAccountIdList(List<Long> AccountIdList);

    int checkAccountIsExists(Long accountId);

    int checkAccountProtocolHasBeenReferredByProtocolId(Long id);

    AccountProtocolVo getAccountProtocolVoByProtocolId(Long protocolId);

    List<AccountProtocolVo> getAccountProtocolListByIdList(List<Long> idList);

    Long checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(@Param("resourceId") Long resourceId, @Param("account") String account, @Param("protocol") String protocol);

    List<ResourceAccountVo> getResourceAccountListByResourceIdList(List<Long> resourceIdList);

    List<ResourceAccountVo> getResourceAccountListByAccountId(Long accountId);

    List<Long> getResourceIdListByAccountIdWithPage(@Param("accountId") Long accountId, @Param("startNum") int startNum, @Param("pageSize") int pageSize);

    List<AccountVo> getResourceAccountListByResourceIdAndProtocolAndAccount(@Param("resourceIdList") List<Long> resourceIdList, @Param("protocolId") Long protocolId, @Param("userName") String userName);

    AccountProtocolVo getAccountProtocolVoByProtocolName(String name);

    List<AccountVo> getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(@Param("resourceId") Long resourceId, @Param("protocol") String protocol, @Param("protocolPort") Integer protocolPort, @Param("username") String username);

    List<TagVo> getTagListByAccountId(Long id);

    int updateAccount(AccountVo vo);

    int updateAccountPasswordById(@Param("id") Long id, @Param("password") String password);

    int updateAccountProtocol(AccountProtocolVo searchVo);

    void insertAccount(AccountVo vo);

    int insertIgnoreResourceAccount(List<ResourceAccountVo> resourceAccountVoList);

    int insertIgnoreAccountTag(List<AccountTagVo> accountTagVoList);

    int insertAccountProtocol(AccountProtocolVo searchVo);

    int insertAccountIp(AccountIpVo ipVo);

    int deleteAccountIpByAccountId(Long value);

    int deleteAccountById(Long id);

    int deleteResourceAccountByResourceId(Long resourceId);

    int deleteResourceAccountByResourceIdListAndAccountIdList(@Param("resourceIdList") List<Long> resourceIdList, @Param("accountIdList") List<Long> accountIdList);

    int deleteAccountTagByAccountId(Long accountId);

    int deleteResourceAccountProtocolById(Long id);

    int deleteResourceAccountByAccountId(Long accountId);

    int deleteAccountIpByIpList(@Param("ipList") List<String> resourceIpList);

}
