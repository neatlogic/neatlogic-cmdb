/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.account;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.*;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountHasBeenReferredException;
import codedriver.framework.tagent.dao.mapper.TagentMapper;
import codedriver.framework.tagent.dto.TagentVo;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
            List<ResourceVo> resourceVoList = resourceMapper.getResourceByIdList(resourceAccountVoList.stream().map(ResourceAccountVo::getResourceId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
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
        List<ResourceVo> resourceVoList = resourceMapper.getResourceByIdList(resourceAccountVoList.stream().map(ResourceAccountVo::getResourceId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
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
     * 1、通过 ”组合工具配置的执行节点的资产id+协议id+执行用户“ 匹配账号表
     * 2、通过 ”组合工具配置的执行节点的ip+协议id“ 匹配 账号表
     * 3、通过 ”组合工具配置的执行节点的ip+端口“ 匹配 账号表
     *
     * @param accountVoList    通过执行节点的资产id+协议id+执行用户 查询回来的账号列表
     * @param allAccountVoList 通过执行节点的ip 查询回来的站好列表
     * @param protocolVoList   所有协议列表
     * @param resourceId       执行节点的资产id
     * @param protocolId       执行节点协议id
     * @param ip               执行节点的ip
     * @param port             执行节点的port
     * @return 匹配的账号
     */
    @Override
    public Optional<AccountVo> filterAccountByRules(List<AccountVo> accountVoList, List<AccountVo> allAccountVoList, List<AccountProtocolVo> protocolVoList, Long resourceId, Long protocolId, String ip, Integer port) {
        //1
        Optional<AccountVo> accountOp = accountVoList.stream().filter(o -> Objects.equals(o.getResourceId(), resourceId)).findFirst();
        if (!accountOp.isPresent()) {
            //2
            Optional<AccountProtocolVo> protocolVoOptional = protocolVoList.stream().filter(o -> Objects.equals(o.getId(), protocolId)).findFirst();
            if (protocolVoOptional.isPresent()) {
                accountOp = allAccountVoList.stream().filter(o -> Objects.equals(o.getIp(), ip) && Objects.equals(o.getProtocolId(), protocolVoOptional.get().getId())).findFirst();
            } else {
                //3
                accountOp = allAccountVoList.stream().filter(o -> Objects.equals(o.getIp(), ip) && Objects.equals(o.getProtocolPort(), port)).findFirst();
            }
        }
        return accountOp;
    }

    /**
     * 删除账号
     *
     * @param accountIdList 账号idList
     */
    @Override
    public void deleteAccount(List<Long> accountIdList, boolean isTagent) {
        for (Long accountId : accountIdList) {
            //如果是tagent 无需判断直接删除账号相关信息
            if (!isTagent) {
                //判断是否为tagent包含ip的账号
                List<TagentVo> tagentList = tagentMapper.getTagentListByAccountId(accountId);
                if (CollectionUtils.isNotEmpty(tagentList)) {
                    throw new ResourceCenterAccountHasBeenReferredException("tagent");
                }
                //判断是否为tagent的账号
                List<TagentVo> tagentVoList = tagentMapper.getTagentByAccountId(accountId);
                if (CollectionUtils.isNotEmpty(tagentVoList)) {
                    throw new ResourceCenterAccountHasBeenReferredException("tagent");
                }
                //判断是否被资产引用
                List<ResourceAccountVo> resourceAccountVoList = resourceAccountMapper.getResourceAccountListByAccountId(accountId);
                if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
                    throw new ResourceCenterAccountHasBeenReferredException("resource");
                }
            }
            resourceAccountMapper.deleteAccountById(accountId);
            resourceAccountMapper.deleteResourceAccountByAccountId(accountId);
            resourceAccountMapper.deleteAccountTagByAccountId(accountId);
            resourceAccountMapper.deleteAccountIpByAccountId(accountId);
        }
    }
}
