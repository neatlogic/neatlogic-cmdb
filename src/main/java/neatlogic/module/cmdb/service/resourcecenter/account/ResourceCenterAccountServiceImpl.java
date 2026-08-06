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

package neatlogic.module.cmdb.service.resourcecenter.account;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.cmdb.exception.resourcecenter.*;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/8 14:41
 **/
@Service
public class ResourceCenterAccountServiceImpl implements ResourceCenterAccountService, IResourceCenterAccountCrossoverService {

    @Resource
    private ResourceTagMapper resourceTagMapper;
    @Resource
    ResourceMapper resourceMapper;
    @Resource
    ResourceAccountMapper resourceAccountMapper;
    @Resource
    TagentMapper tagentMapper;

    /**
     * 按以下规则顺序匹配account
     * 1、tagent 先获取主ip的账号，不存在再通过ip在 account_ip 匹配账号， 其它则从resource_account(资产清单)中匹配账号
     * 2、根据节点对应os资产获取账号
     * 3、通过 ”协议id“ 匹配默认账号
     *
     * @param accountByResourceList     通过执行节点的资产id+协议id+执行用户 查询回来的账号列表（tagent类型不适用）
     * @param tagentMainIpAccountMap    通过执行节点的ip 查询回来的主ip对应账号列表（目前仅用于tagent类型的匹配）
     * @param tagentIpAccountMap        通过执行节点的ip 查询回来的账号列表（目前仅用于tagent类型的匹配）
     * @param resourceId                执行节点的资产id
     * @param ip                        执行节点的ip
     * @param resourceOSResourceMap     节点resourceId->对应操作系统resourceId
     * @param protocolDefaultAccountMap 协议对应的默认账号
     * @return 匹配的账号
     */
    @Override
    public AccountBaseVo filterAccountByRules(List<AccountVo> accountByResourceList, Map<String, AccountBaseVo> tagentMainIpAccountMap, Map<String, AccountBaseVo> tagentIpAccountMap, Long resourceId, AccountProtocolVo protocolVo, String ip, Map<Long, Long> resourceOSResourceMap, Map<Long, AccountVo> protocolDefaultAccountMap) {
        AccountBaseVo accountVo = null;
        Optional<AccountVo> accountOp;
        //1
        if (Objects.equals(protocolVo.getName(), Protocol.TAGENT.getValue()) || (protocolVo.getName() != null && protocolVo.getName().startsWith(Protocol.TAGENT.getValue() + "."))) {
            accountVo = tagentMainIpAccountMap.get(ip);
            if(accountVo == null) {
                accountVo = tagentIpAccountMap.get(ip);
            }
        } else {
            accountOp = accountByResourceList.stream().filter(o -> Objects.equals(o.getResourceId(), resourceId)).findFirst();
            if (accountOp.isPresent()) {
                accountVo = accountOp.get();
            }
        }
        //2
        if (accountVo == null) {
            Long osResourceId = resourceOSResourceMap.get(resourceId);
            accountOp = accountByResourceList.stream().filter(o -> Objects.equals(o.getResourceId(), osResourceId)).findFirst();
            if (accountOp.isPresent()) {
                accountVo = accountOp.get();
            }
        }
        //3
        if (accountVo == null) {
            accountVo = protocolDefaultAccountMap.get(protocolVo.getId());
        }
        return accountVo;
    }

    /**
     * 删除账号
     *
     * @param accountIdList 账号idList
     */
    @Override
    public void deleteAccount(List<Long> accountIdList) {
        if (CollectionUtils.isNotEmpty(accountIdList)) {
            resourceAccountMapper.deleteAccountByIdList(accountIdList);
            resourceAccountMapper.deleteResourceAccountByAccountIdList(accountIdList);
            resourceAccountMapper.deleteAccountTagByAccountIdList(accountIdList);
//            resourceAccountMapper.deleteAccountIpByAccountIdList(accountIdList);
        }
    }

    @Override
    public JSONObject saveAccount(Long id, AccountVo paramAccountVo) {
        AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(paramAccountVo.getProtocolId());
        if (protocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(paramAccountVo.getProtocolId());
        }
        paramAccountVo.setProtocol(protocolVo.getName());
        if (!StringUtils.equals(protocolVo.getName(), "tagent") && StringUtils.isEmpty(paramAccountVo.getAccount())) {
            throw new ResourceCenterAccountNameIsNotNullException();
        }
        String type = paramAccountVo.getType();
        if (Objects.equals(type, AccountType.PUBLIC.getValue())) {
            if (resourceAccountMapper.checkAccountNameIsRepeats(paramAccountVo) > 0) {
                throw new ResourceCenterAccountNameRepeatsException(paramAccountVo.getName());
            }
        } else {
            // 如果是私有类型账号，需要校验该资产中所有公有和私有账号中是否存在账号及协议都相同的，如果存在则不能更新
            Long resourceId = paramAccountVo.getResourceId();
            if (resourceId == null) {
                throw new ParamNotExistsException("resourceId");
            }
            List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
            for (AccountVo accountVo : accountVoList) {
                if (Objects.equals(paramAccountVo.getName(), accountVo.getName()) && !Objects.equals(paramAccountVo.getId(), accountVo.getId())) {
                    throw new ResourceCenterAccountNameRepeatsException(paramAccountVo.getName());
                }
            }
            List<String> failureReasonList = check(resourceId, paramAccountVo);
            if (CollectionUtils.isNotEmpty(failureReasonList)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("failureReasonList", failureReasonList);
                return resultObj;
            }
            List<ResourceAccountVo> resourceAccountVoList = new ArrayList<>();
            resourceAccountVoList.add(new ResourceAccountVo(resourceId, paramAccountVo.getId()));
            resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
        }
        List<Long> tagIdList = paramAccountVo.getTagIdList();
        List<AccountTagVo> accountTagVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagIdList)) {
            List<Long> searchTagIdList = null;
            List<Long> insertTagIdList = new ArrayList<>(tagIdList);
            List<TagVo> tagVoList = resourceTagMapper.searchTagListByIdList(tagIdList);
            searchTagIdList = tagVoList.stream().map(TagVo::getId).collect(Collectors.toList());
            insertTagIdList.removeAll(searchTagIdList);
            if (CollectionUtils.isNotEmpty(insertTagIdList)) {
                List<Long> notFoundTagIdList = new ArrayList<>(insertTagIdList);
                if (CollectionUtils.isNotEmpty(notFoundTagIdList)) {
                    throw new ResourceCenterTagNotFoundException(notFoundTagIdList);
                }
            }
            resourceAccountMapper.deleteAccountTagByAccountId(paramAccountVo.getId());
            for (Long tagId : tagIdList) {
                accountTagVoList.add(new AccountTagVo(paramAccountVo.getId(), tagId));
                if (accountTagVoList.size() > 100) {
                    resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
                    accountTagVoList.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(accountTagVoList)) {
                resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
            }
        }
        paramAccountVo.setLcu(UserContext.get().getUserUuid());

        //一个协议只能存一个默认账号。例如，ssh协议当前默认账号为app，如果在编辑root账号时，把root设置为默认账号，需要替换掉原有的app默认账号表示标识。root代替app成为了新的ssh协议默认账号。
        if (Objects.equals(type, AccountType.PUBLIC.getValue()) && Objects.equals(paramAccountVo.getIsDefault(), 1)) {
            resourceAccountMapper.resetAccountDefaultByProtocolIdAndAccount(paramAccountVo.getProtocolId(), paramAccountVo.getAccount());
        }

        if (id != null) {
            AccountVo oldVo = resourceAccountMapper.getAccountById(id);
            if (oldVo == null) {
                throw new ResourceCenterAccountNotFoundException(id);
            }
            paramAccountVo.setProtocolId(protocolVo.getId());
            resourceAccountMapper.updateAccount(paramAccountVo);
        } else {
            if (Objects.equals(protocolVo.getName(), "tagent")) {
                throw new ResourceCenterAccountNotCreateTagentAccountException();
            }
            paramAccountVo.setFcu(UserContext.get().getUserUuid());
            resourceAccountMapper.insertAccount(paramAccountVo);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("id", paramAccountVo.getId());
        return resultObj;
    }

    @Override
    public JSONObject saveResourceAccount(Long resourceId, List<Long> accountIdList) {
        int successCount = 0;
        List<String> failureReasonList = new ArrayList<>();
        // 查询该资产绑定的公有账号列表，再根据账号ID解绑
        List<AccountVo> accountList = resourceAccountMapper.getResourceAccountListByResourceIdAndType(resourceId, AccountType.PUBLIC.getValue());
        if (CollectionUtils.isNotEmpty(accountList)) {
            List<Long> accountIds = accountList.stream().map(AccountVo::getId).collect(Collectors.toList());
            resourceAccountMapper.deleteResourceAccountByResourceIdListAndAccountIdList(Collections.singletonList(resourceId), accountIds);
        }
        if (CollectionUtils.isEmpty(accountIdList)) {
            return null;
        }
        Map<String, AccountVo> accountVoMap = new HashMap<>();
        List<Long> existAccountIdList = new ArrayList<>();
        Set<Long> excludeAccountIdSet = new HashSet<>();
        List<AccountVo> accountVoList = resourceAccountMapper.getAccountListByIdList(accountIdList);
        for (AccountVo accountVo : accountVoList) {
            existAccountIdList.add(accountVo.getId());
            String key = accountVo.getProtocol() + "#" + accountVo.getAccount();
            AccountVo account = accountVoMap.get(key);
            if (account == null) {
                accountVoMap.put(key, accountVo);
            } else {
                failureReasonList.add("选中项中\"" + accountVo.getName() + "（" + accountVo.getProtocol() + "/" + accountVo.getAccount() + "）\"与\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"的协议相同且用户名相同，同一资产不可绑定多个协议相同且用户名相同的账号");
                excludeAccountIdSet.add(accountVo.getId());
                excludeAccountIdSet.add(account.getId());
            }
        }
        if (accountIdList.size() > existAccountIdList.size()) {
            List<Long> notFoundIdList = ListUtils.removeAll(accountIdList, existAccountIdList);
            if (CollectionUtils.isNotEmpty(notFoundIdList)) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Long accountId : notFoundIdList) {
                    stringBuilder.append(accountId);
                    stringBuilder.append("、");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                throw new ResourceCenterAccountNotFoundException(stringBuilder.toString());
            }
        }
        accountIdList.removeAll(excludeAccountIdSet);
        List<ResourceAccountVo> resourceAccountVoList = new ArrayList<>();
        for (Long accountId : accountIdList) {
            resourceAccountVoList.add(new ResourceAccountVo(resourceId, accountId));
            successCount++;
            if (resourceAccountVoList.size() > 100) {
                resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
                resourceAccountVoList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureReasonList.size());
        resultObj.put("failureReasonList", failureReasonList);
        return resultObj;
    }

    /**
     * 校验该资产中所有公有和私有账号中是否存在账号及协议都相同的
     *
     * @param resourceId   资产ID
     * @param newAccountVo 新账号信息
     */
    private List<String> check(Long resourceId, AccountVo newAccountVo) {
        List<String> failureReasonList = new ArrayList<>();
        Map<String, AccountVo> accountVoMap = new HashMap<>();
        List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
        accountVoList.removeIf(accountVo -> Objects.equals(accountVo.getId(), newAccountVo.getId()));
        accountVoList.add(newAccountVo);
        for (AccountVo accountVo : accountVoList) {
            String key = accountVo.getProtocol() + "#" + accountVo.getAccount();
            AccountVo account = accountVoMap.get(key);
            if (account == null) {
                accountVoMap.put(key, accountVo);
            } else {
                failureReasonList.add("选中项中\"" + accountVo.getName() + "（" + accountVo.getProtocol() + "/" + accountVo.getAccount() + "）\"与\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"");
            }
        }
        return failureReasonList;
    }
}
