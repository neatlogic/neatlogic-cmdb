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
     * @param tagentIpAccountMap        通过执行节点的ip 查询回来的账号列表（目前仅用于tagent类型的匹配）
     * @param tagentIpAccountMap        通过执行节点的ip 查询回来的站好列表
     * @param resourceId                执行节点的资产id
     * @param protocolVo                执行节点协议
     * @param ip                        执行节点的ip
     * @param resourceOSResourceMap     节点resourceId->对应操作系统resourceId
     * @param protocolDefaultAccountMap 协议对应的默认账号
     * @return 匹配的账号
     */
    AccountBaseVo filterAccountByRules(List<AccountVo> accountByResourceList, Map<String, AccountBaseVo> tagentMainIpAccountMap,Map<String, AccountBaseVo> tagentIpAccountMap, Long resourceId, AccountProtocolVo protocolVo, String ip, Map<Long, Long> resourceOSResourceMap, Map<Long, AccountVo> protocolDefaultAccountMap);

    /**
     * 删除账号
     *
     * @param accountIdList 账号idList
     */
    void deleteAccount(List<Long> accountIdList);

    /**
     * 保存账号
     * @param id
     * @param paramAccountVo
     * @return
     */
    JSONObject saveAccount(Long id, AccountVo paramAccountVo);

    /**
     * 绑定账号与资源关系
     * @param resourceId
     * @param accountIdList
     * @return
     */
    JSONObject saveResourceAccount(Long resourceId, List<Long> accountIdList);
}
