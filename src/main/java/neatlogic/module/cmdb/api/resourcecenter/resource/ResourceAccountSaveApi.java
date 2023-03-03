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

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/6/22 15:55
 **/
@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceAccountSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/save";
    }

    @Override
    public String getName() {
        return "保存资源帐号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, desc = "帐号id列表")
    })
    @Description(desc = "保存资源帐号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        int successCount = 0;
        List<String> failureReasonList = new ArrayList<>();
        Long resourceId = paramObj.getLong("resourceId");
        if (resourceMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        // 查询该资产绑定的公有帐号列表，再根据帐号ID解绑
        List<AccountVo> accountList = resourceAccountMapper.getResourceAccountListByResourceIdAndType(resourceId, AccountType.PUBLIC.getValue());
        if (CollectionUtils.isNotEmpty(accountList)) {
            List<Long> idList = accountList.stream().map(AccountVo::getId).collect(Collectors.toList());
            resourceAccountMapper.deleteResourceAccountByAccountIdList(idList);
        }
        JSONArray accountIdArray = paramObj.getJSONArray("accountIdList");
        if (CollectionUtils.isEmpty(accountIdArray)) {
            return null;
        }
        List<Long> accountIdList = accountIdArray.toJavaList(Long.class);
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
                failureReasonList.add("选中项中\"" + accountVo.getName() + "（" + accountVo.getProtocol() + "/" + accountVo.getAccount() + "）\"与\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"的协议相同且用户名相同，同一资产不可绑定多个协议相同且用户名相同的帐号");
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
}
