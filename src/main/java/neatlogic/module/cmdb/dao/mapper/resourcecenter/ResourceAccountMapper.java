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

import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResourceAccountMapper extends IResourceAccountCrossoverMapper {

    int getAccountCount(AccountVo searchVo);

    AccountVo getAccountById(Long id);

    AccountVo getPublicAccountByName(String name);

    List<AccountVo> getAccountListByIdList(List<Long> accountIdList);

    int checkAccountNameIsRepeats(AccountVo vo);

    int searchAccountCount(AccountVo searchVo);

    List<AccountVo> searchAccount(AccountVo searchVo);

    List<AccountVo> getAccountListForSelect(AccountVo searchVo);

    List<Long> getAccountIdListByAccountAndProtocol(@Param("account") String account, @Param("protocol") String protocol);

    List<Long> checkAccountIdListIsExists(List<Long> idList);

    AccountVo getResourceAccountByIpAndPort(@Param("host") String host, @Param("port") Integer port);

    AccountVo getAccountByTagentIpAndPort(@Param("ip") String ip, @Param("port") Integer port);

    List<AccountVo> getAccountListByIpListAndProtocolId(@Param("ipList") List<String> ipList, @Param("protocolId") Long protocolId);

    AccountVo getAccountByTagentId(Long id);

    List<AccountVo> getAccountListByIpAndProtocolNameAndAccountAndProtocolPort(@Param("ip") String ip, @Param("protocolName") String protocolName, @Param("account") String account, @Param("protocolPort") Integer protocolPort);

    List<AccountVo> getAccountListByProtocolNameAndAccountAndProtocolPort(@Param("protocolName") String protocolName, @Param("account") String account, @Param("protocolPort") Integer protocolPort);

    List<AccountVo> getResourceAccountListByResourceIdAndTypeAndProtocol(@Param("resourceId") Long resourceId, @Param("type") String type, @Param("protocol") String protocol);

    List<AccountVo> getResourceAccountListByResourceIdAndType(@Param("resourceId") Long resourceId, @Param("type") String type);

    List<AccountVo> getResourceAccountListByResourceId(Long resourceId);

    List<AccountVo> getAllAccountList();

    List<AccountVo> getDefaultAccountListByProtocolIdListAndAccount(@Param("list") List<Long> protocolIdList,@Param("account") String account);

    List<AccountProtocolVo> searchAccountProtocolListByProtocolName(AccountProtocolVo searchVo);

    List<AccountProtocolVo> getAllAccountProtocolList();

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

    AccountProtocolVo getAccountProtocolVoByNameAndPort(@Param("name") String name, @Param("port") Integer port);

    List<String> getAccountIpByIpListAndPort(@Param("ipList") List<String> ipList, @Param("port") Integer port);

    List<AccountVo> getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(@Param("resourceId") Long resourceId, @Param("protocol") String protocol, @Param("protocolPort") Integer protocolPort, @Param("username") String username);

    List<TagVo> getTagListByAccountId(Long id);

    int updateAccount(AccountVo vo);

    int updateAccountPasswordById(@Param("id") Long id, @Param("password") String password);

    int resetAccountDefaultByProtocolId(Long protocolId);

    void insertAccount(AccountVo vo);

    int insertIgnoreResourceAccount(List<ResourceAccountVo> resourceAccountVoList);

    int insertIgnoreAccountTag(List<AccountTagVo> accountTagVoList);

    int insertAccountProtocol(AccountProtocolVo searchVo);

    int insertAccountIp(AccountIpVo ipVo);

    int deleteAccountIpByAccountId(Long value);

    int deleteAccountIpByAccountIdList(List<Long> list);

    int deleteAccountById(Long id);

    int deleteAccountByIdList(List<Long> list);

    int deleteResourceAccountByResourceId(Long resourceId);

    int deleteResourceAccountByResourceIdListAndAccountIdList(@Param("resourceIdList") List<Long> resourceIdList, @Param("accountIdList") List<Long> accountIdList);

    int deleteAccountTagByAccountId(Long accountId);

    int deleteAccountTagByAccountIdList(List<Long> list);

    int deleteResourceAccountProtocolById(Long id);

    int deleteResourceAccountByAccountId(Long accountId);

    int deleteResourceAccountByAccountIdList(List<Long> list);

    int deleteAccountIpByIpList(@Param("ipList") List<String> resourceIpList);

}
