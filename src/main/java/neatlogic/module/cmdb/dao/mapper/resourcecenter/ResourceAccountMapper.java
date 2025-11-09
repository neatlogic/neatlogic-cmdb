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

import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountTagVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
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

    AccountVo getAccountByTagentIpAndPort(@Param("ip") String ip, @Param("port") Integer port);

    AccountVo getAccountByTagentId(Long id);

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

    List<AccountVo> getResourceAccountByResourceIdAndProtocolAndProtocolPortAndUsername(@Param("resourceId") Long resourceId, @Param("protocol") String protocol, @Param("protocolPort") Integer protocolPort, @Param("username") String username);

    List<TagVo> getTagListByAccountId(Long id);

    int updateAccount(AccountVo vo);

    int updateAccountPasswordById(@Param("id") Long id, @Param("password") String password);

    int resetAccountDefaultByProtocolIdAndAccount(@Param("protocolId") Long protocolId,@Param("account") String account);

    void insertAccount(AccountVo vo);

    int insertIgnoreResourceAccount(List<ResourceAccountVo> resourceAccountVoList);

    int insertIgnoreAccountTag(List<AccountTagVo> accountTagVoList);

    int insertAccountProtocol(AccountProtocolVo searchVo);

    int deleteAccountById(Long id);

    int deleteAccountByIdList(List<Long> list);

    int deleteResourceAccountByResourceId(Long resourceId);

    int deleteResourceAccountByResourceIdListAndAccountIdList(@Param("resourceIdList") List<Long> resourceIdList, @Param("accountIdList") List<Long> accountIdList);

    int deleteAccountTagByAccountId(Long accountId);

    int deleteAccountTagByAccountIdList(List<Long> list);

    int deleteResourceAccountProtocolById(Long id);

    int deleteResourceAccountByAccountId(Long accountId);

    int deleteResourceAccountByAccountIdList(List<Long> list);

}
