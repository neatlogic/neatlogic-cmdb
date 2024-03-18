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

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
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
        return "保存资源账号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, desc = "账号id列表")
    })
    @Description(desc = "保存资源账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        int successCount = 0;
        List<String> failureReasonList = new ArrayList<>();
        Long resourceId = paramObj.getLong("resourceId");
        if (resourceMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        // 查询该资产绑定的公有账号列表，再根据账号ID解绑
        List<AccountVo> accountList = resourceAccountMapper.getResourceAccountListByResourceIdAndType(resourceId, AccountType.PUBLIC.getValue());
        if (CollectionUtils.isNotEmpty(accountList)) {
            List<Long> accountIdList = accountList.stream().map(AccountVo::getId).collect(Collectors.toList());
            resourceAccountMapper.deleteResourceAccountByResourceIdListAndAccountIdList(Arrays.asList(resourceId), accountIdList);
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
                failureReasonList.add("选中项中\"" + accountVo.getName() + "（" + accountVo.getProtocol() + "/" + accountVo.getAccount() + "）\"与\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"的协议相同且用户名相同，同一资产不可绑定多个协议相同且用户名相同的账号");
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
