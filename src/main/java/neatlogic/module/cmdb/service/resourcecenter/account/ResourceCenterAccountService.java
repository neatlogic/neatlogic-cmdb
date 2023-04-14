/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.cmdb.service.resourcecenter.account;

import neatlogic.framework.cmdb.dto.resourcecenter.AccountBaseVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;

import java.util.List;
import java.util.Map;

/**
 * @author lvzk
 * @since 2021/11/8 14:41
 **/
public interface ResourceCenterAccountService {
    /**
     * 按以下规则顺序匹配account
     * 1、通过 ”组合工具配置的执行节点的资产id+协议id+执行用户“ 匹配
     * 2、通过 ”组合工具配置的执行节点的ip+协议id“ 匹配 帐号表
     * 3、通过 ”组合工具配置的执行节点的ip+端口“ 匹配 帐号表
     *
     * @param accountByResourceList     通过执行节点的资产id+协议id+执行用户 查询回来的帐号列表
     * @param tagentIpAccountMap        通过执行节点的ip 查询回来的站好列表
     * @param resourceId                执行节点的资产id
     * @param protocolVo                执行节点协议
     * @param ip                        执行节点的ip
     * @param resourceOSResourceMap     节点resourceId->对应操作系统resourceId
     * @param protocolDefaultAccountMap 协议对应的默认帐号
     * @return 匹配的帐号
     */
    AccountBaseVo filterAccountByRules(List<AccountVo> accountByResourceList, Map<String, AccountBaseVo> tagentIpAccountMap, Long resourceId, AccountProtocolVo protocolVo, String ip, Map<Long, Long> resourceOSResourceMap, Map<Long, AccountVo> protocolDefaultAccountMap);

    /**
     * 删除帐号
     *
     * @param accountIdList 帐号idList
     */
    void deleteAccount(List<Long> accountIdList);
}
