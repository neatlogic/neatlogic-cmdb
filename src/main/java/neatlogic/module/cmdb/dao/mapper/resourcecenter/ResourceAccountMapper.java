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
