/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.cmdb.service.resourcecenter.account;

import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountBaseVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.tagent.dao.mapper.TagentMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
}
