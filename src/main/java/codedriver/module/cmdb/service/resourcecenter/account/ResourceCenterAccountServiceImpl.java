/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.account;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.ResourceCenterAccountCrossoverService;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountIpVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountHasBeenReferredException;
import codedriver.framework.tagent.dao.mapper.TagentMapper;
import codedriver.framework.tagent.dto.TagentVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/8 14:41
 **/
@Service
public class ResourceCenterAccountServiceImpl implements ResourceCenterAccountService, ResourceCenterAccountCrossoverService {
    @Resource
    ResourceCenterMapper resourceCenterMapper;
    @Resource
    TagentMapper tagentMapper;

    @Override
    public void refreshAccountIpByAccountId(Long accountId) {
        resourceCenterMapper.deleteAccountIpByAccountId(accountId);
        //账号是否被resource引用
        List<ResourceAccountVo> resourceAccountVoList = resourceCenterMapper.getResourceAccountListByAccountId(accountId);
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceByIdList(resourceAccountVoList.stream().map(ResourceAccountVo::getResourceId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
            for (ResourceVo resourceVo : resourceVoList) {
                resourceCenterMapper.insertIgnoreAccountIp(new AccountIpVo(accountId, resourceVo.getIp()));
            }
        }
        //账号是否被tagent引用
        List<TagentVo> tagentVoList = tagentMapper.getTagentByAccountId(accountId);
        if (CollectionUtils.isNotEmpty(tagentVoList)) {
            for (TagentVo tagentVo : tagentVoList) {
                resourceCenterMapper.insertIgnoreAccountIp(new AccountIpVo(tagentVo.getAccountId(), tagentVo.getIp()));
            }
        }
    }

    @Override
    public void refreshAccountIpByResourceIdList(List<Long> resourceIdList) {
        List<ResourceAccountVo> resourceAccountVoList = resourceCenterMapper.getResourceAccountListByResourceIdList(resourceIdList);
        List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceByIdList(resourceAccountVoList.stream().map(ResourceAccountVo::getResourceId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
        //账号是否被resource引用
        if(CollectionUtils.isNotEmpty(resourceVoList)) {
            List<String> resourceIpList =  resourceVoList.stream().map(ResourceVo::getIp).collect(Collectors.toList());
            resourceCenterMapper.deleteAccountIpByIpList(resourceIpList);
            HashMap<Long, ResourceVo> resourceVoHashMap = new HashMap<>();
            for (ResourceVo resourceVo : resourceVoList) {
                resourceVoHashMap.put(resourceVo.getId(), resourceVo);
            }
            for (ResourceAccountVo resourceAccountVo : resourceAccountVoList) {
                if (resourceVoHashMap.containsKey(resourceAccountVo.getResourceId())) {
                    resourceCenterMapper.insertIgnoreAccountIp(new AccountIpVo(resourceAccountVo.getAccountId(), resourceVoHashMap.get(resourceAccountVo.getResourceId()).getIp()));
                }
            }
            //账号是否被tagent引用
            List<TagentVo> tagentVoList = tagentMapper.getTagentByIpList(resourceIpList);
            if (CollectionUtils.isNotEmpty(tagentVoList)) {
                for (TagentVo tagentVo : tagentVoList) {
                    resourceCenterMapper.insertIgnoreAccountIp(new AccountIpVo(tagentVo.getAccountId(), tagentVo.getIp()));
                }
            }
        }
    }

    /**
     * 删除账号
     *
     * @param accountId 账号id
     */
    @Override
    public void deleteAccount(Long accountId, boolean isTagent) {
        List<TagentVo> tagentVoList = tagentMapper.getTagentByAccountId(accountId);
        List<ResourceAccountVo> resourceAccountVoList = resourceCenterMapper.getResourceAccountListByAccountId(accountId);
        if (CollectionUtils.isNotEmpty(tagentVoList)) {
            throw new ResourceCenterAccountHasBeenReferredException("tagent");
        }
        //如果是tagent 无需判断直接删除账号相关信息
        if (!isTagent && CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            throw new ResourceCenterAccountHasBeenReferredException("resource");
        }
        resourceCenterMapper.deleteAccountById(accountId);
        resourceCenterMapper.deleteResourceAccountByAccountId(accountId);
        resourceCenterMapper.deleteAccountTagByAccountId(accountId);
        resourceCenterMapper.deleteAccountIpByAccountId(accountId);
    }
}
