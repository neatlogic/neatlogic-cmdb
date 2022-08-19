/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.account;

import java.util.List;

/**
 * @author lvzk
 * @since 2021/11/8 14:41
 **/
public interface ResourceCenterAccountService {

    /**
     * 根据账号id刷新账号ip
     *
     * @param accountId 账号id
     */
    void refreshAccountIpByAccountId(Long accountId);

    /**
     * 根据资产id刷新账号ip
     * @param resourceIdList 资产id
     */
    void refreshAccountIpByResourceIdList(List<Long> resourceIdList);

    /**
     * 删除账号
     *
     * @param accountIdList 账号idList
     */
    void deleteAccount(List<Long> accountIdList);
}
