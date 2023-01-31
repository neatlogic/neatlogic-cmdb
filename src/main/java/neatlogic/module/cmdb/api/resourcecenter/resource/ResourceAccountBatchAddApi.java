/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
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

/**
 * @author linbq
 * @since 2021/6/22 15:56
 **/
@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceAccountBatchAddApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/batch/add";
    }

    @Override
    public String getName() {
        return "批量添加资源账号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "资源id列表"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "账号id列表")
    })
    @Description(desc = "批量添加资源账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        int successCount = 0;
        List<String> failureReasonList = new ArrayList<>();
        failureReasonList.add("同一资产不可绑定多个协议相同且用户名相同的账号");
        JSONArray resourceIdArray = paramObj.getJSONArray("resourceIdList");
        JSONArray accountIdArray = paramObj.getJSONArray("accountIdList");

        List<Long> resourceIdList = resourceIdArray.toJavaList(Long.class);
        Map<Long, ResourceVo> resourceVoMap = new HashMap<>();
        List<Long> existResourceIdList = new ArrayList<>();
        List<ResourceVo> resourceVoList = resourceMapper.getResourceListByIdList(resourceIdList);
        for (ResourceVo resourceVo : resourceVoList) {
            resourceVoMap.put(resourceVo.getId(), resourceVo);
            existResourceIdList.add(resourceVo.getId());
        }
        if (resourceIdList.size() > existResourceIdList.size()) {
            List<Long> notFoundIdList = ListUtils.removeAll(resourceIdList, existResourceIdList);
            if (CollectionUtils.isNotEmpty(notFoundIdList)) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Long resourceId : notFoundIdList) {
                    stringBuilder.append(resourceId);
                    stringBuilder.append("、");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                throw new ResourceNotFoundException(stringBuilder.toString());
            }
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
                failureReasonList.add("选中项中\"" + accountVo.getName() + "（" + accountVo.getProtocol() + "/" + accountVo.getAccount() + "）\"与\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"");
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

        List<ResourceAccountVo> resourceAccountVoList = new ArrayList<>();
        for (Long resourceId : resourceIdList) {
            for (AccountVo accountVo : accountVoList) {
                if (excludeAccountIdSet.contains(accountVo.getId())) {
                    continue;
                }
                Long accountId = resourceAccountMapper.checkResourceIsExistsCorrespondingAccountByResourceIdAndAccountIdAndProtocol(resourceId, accountVo.getAccount(), accountVo.getProtocol());
                if (accountId != null) {
                    ResourceVo resourecVo = resourceVoMap.get(resourceId);
                    AccountVo account = resourceAccountMapper.getAccountById(accountId);
                    failureReasonList.add(resourecVo.getName() + "（" + resourecVo.getIp() + "）'已绑定账号\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"");
                    continue;
                }
                resourceAccountVoList.add(new ResourceAccountVo(resourceId, accountVo.getId()));
                successCount++;
                if (resourceAccountVoList.size() > 100) {
                    resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
                    resourceAccountVoList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(resourceAccountVoList)) {
            resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureReasonList.size() - 1);
        if (failureReasonList.size() > 1) {
            resultObj.put("failureReasonList", failureReasonList);
        } else {
            resultObj.put("failureReasonList", new ArrayList<>());
        }
        return resultObj;
    }
}
