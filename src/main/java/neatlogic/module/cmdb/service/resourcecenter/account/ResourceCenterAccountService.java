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

import java.util.List;

/**
 * @author lvzk
 * @since 2021/11/8 14:41
 **/
public interface ResourceCenterAccountService {

    /**
     * 根据帐号id刷新帐号ip
     *
     * @param accountId 帐号id
     */
    void refreshAccountIpByAccountId(Long accountId);

    /**
     * 根据资产id刷新帐号ip
     * @param resourceIdList 资产id
     */
    void refreshAccountIpByResourceIdList(List<Long> resourceIdList);

    /**
     * 删除帐号
     *
     * @param accountIdList 帐号idList
     */
    void deleteAccount(List<Long> accountIdList);
}
