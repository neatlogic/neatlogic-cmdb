/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.service.resourcecenter.account;

import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.*;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
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
    ResourceMapper resourceMapper;
    @Resource
    ResourceAccountMapper resourceAccountMapper;
    @Resource
    TagentMapper tagentMapper;

    @Override
    public void refreshAccountIpByAccountId(Long accountId) {
        resourceAccountMapper.deleteAccountIpByAccountId(accountId);
        //账号是否被resource引用
        List<ResourceAccountVo> resourceAccountVoList = resourceAccountMapper.getResourceAccountListByAccountId(accountId);
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            List<ResourceVo> resourceVoList = resourceMapper.getResourceByIdList(resourceAccountVoList.stream().map(ResourceAccountVo::getResourceId).collect(Collectors.toList()));
            for (ResourceVo resourceVo : resourceVoList) {
                resourceAccountMapper.insertAccountIp(new AccountIpVo(accountId, resourceVo.getIp()));
            }
        }
        //账号是否被tagent引用
        List<TagentVo> tagentVoList = tagentMapper.getTagentByAccountId(accountId);
        if (CollectionUtils.isNotEmpty(tagentVoList)) {
            for (TagentVo tagentVo : tagentVoList) {
                resourceAccountMapper.insertAccountIp(new AccountIpVo(tagentVo.getAccountId(), tagentVo.getIp()));
            }
        }
    }

    @Override
    public void refreshAccountIpByResourceIdList(List<Long> resourceIdList) {
        List<ResourceAccountVo> resourceAccountVoList = resourceAccountMapper.getResourceAccountListByResourceIdList(resourceIdList);
        List<ResourceVo> resourceVoList = resourceMapper.getResourceByIdList(resourceAccountVoList.stream().map(ResourceAccountVo::getResourceId).collect(Collectors.toList()));
        //账号是否被resource引用
        if (CollectionUtils.isNotEmpty(resourceVoList)) {
            List<String> resourceIpList = resourceVoList.stream().map(ResourceVo::getIp).collect(Collectors.toList());
            resourceAccountMapper.deleteAccountIpByIpList(resourceIpList);
            HashMap<Long, ResourceVo> resourceVoHashMap = new HashMap<>();
            for (ResourceVo resourceVo : resourceVoList) {
                resourceVoHashMap.put(resourceVo.getId(), resourceVo);
            }
            for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
                if (resourceVoHashMap.containsKey(resourceAccountVo.getResourceId())) {
                    resourceAccountMapper.insertAccountIp(new AccountIpVo(resourceAccountVo.getAccountId(), resourceVoHashMap.get(resourceAccountVo.getResourceId()).getIp()));
                }
            }
            //账号是否被tagent引用
            List<TagentVo> tagentVoList = tagentMapper.getTagentByIpList(resourceIpList);
            if (CollectionUtils.isNotEmpty(tagentVoList)) {
                for (TagentVo tagentVo : tagentVoList) {
                    resourceAccountMapper.insertAccountIp(new AccountIpVo(tagentVo.getAccountId(), tagentVo.getIp()));
                }
            }
        }
    }

    /**
     * 按以下规则顺序匹配account
     * 1、tagent 直接通过ip在 account_ip 匹配账号， 其它则从resource_account(资产清单)中匹配账号
     * 2、根据节点对应os资产获取账号
     * 3、通过 ”协议id“ 匹配默认账号
     *
     * @param accountByResourceList     通过执行节点的资产id+协议id+执行用户 查询回来的账号列表（tagent类型不适用）
     * @param tagentIpAccountMap        通过执行节点的ip 查询回来的账号列表（目前仅用于tagent类型的匹配）
     * @param resourceId                执行节点的资产id
     * @param ip                        执行节点的ip
     * @param resourceOSResourceMap     节点resourceId->对应操作系统resourceId
     * @param protocolDefaultAccountMap 协议对应的默认账号
     * @return 匹配的账号
     */
    @Override
    public AccountBaseVo filterAccountByRules(List<AccountVo> accountByResourceList, Map<String, AccountBaseVo> tagentIpAccountMap, Long resourceId, AccountProtocolVo protocolVo, String ip, Map<Long, Long> resourceOSResourceMap, Map<Long, AccountVo> protocolDefaultAccountMap) {
        AccountBaseVo accountVo = null;
        Optional<AccountVo> accountOp;
        //1
        if (Objects.equals(protocolVo.getName(), Protocol.TAGENT.getValue())) {
            accountVo = tagentIpAccountMap.get(ip);
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
}
