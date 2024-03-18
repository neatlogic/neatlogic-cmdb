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
     * 2、通过 ”组合工具配置的执行节点的ip+协议id“ 匹配 账号表
     * 3、通过 ”组合工具配置的执行节点的ip+端口“ 匹配 账号表
     *
     * @param accountByResourceList     通过执行节点的资产id+协议id+执行用户 查询回来的账号列表
     * @param tagentIpAccountMap        通过执行节点的ip 查询回来的站好列表
     * @param resourceId                执行节点的资产id
     * @param protocolVo                执行节点协议
     * @param ip                        执行节点的ip
     * @param resourceOSResourceMap     节点resourceId->对应操作系统resourceId
     * @param protocolDefaultAccountMap 协议对应的默认账号
     * @return 匹配的账号
     */
    AccountBaseVo filterAccountByRules(List<AccountVo> accountByResourceList, Map<String, AccountBaseVo> tagentIpAccountMap, Long resourceId, AccountProtocolVo protocolVo, String ip, Map<Long, Long> resourceOSResourceMap, Map<Long, AccountVo> protocolDefaultAccountMap);

    /**
     * 删除账号
     *
     * @param accountIdList 账号idList
     */
    void deleteAccount(List<Long> accountIdList);
}
