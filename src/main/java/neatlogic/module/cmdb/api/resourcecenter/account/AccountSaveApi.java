/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.cmdb.api.resourcecenter.account;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountTagVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.cmdb.exception.resourcecenter.*;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AccountSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/save";
    }

    @Override
    public String getName() {
        return "保存资源中心帐号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "帐号ID"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 50, isRequired = true, desc = "名称"),
            @Param(name = "account", type = ApiParamType.STRING, maxLength = 50, desc = "用户名"),
            @Param(name = "passwordPlain", type = ApiParamType.STRING, isRequired = false, desc = "密码"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "协议id"),
            @Param(name = "port", type = ApiParamType.INTEGER, isRequired = false, desc = "端口"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = false, desc = "标签id列表"),
            @Param(name = "type", type = ApiParamType.ENUM, member = AccountType.class, isRequired = true, desc = "标签id列表"),
            @Param(name = "isDefault", type = ApiParamType.INTEGER, desc = "是否默认帐号"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产ID")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "帐号ID")
    })
    @Description(desc = "保存资源中心帐号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountVo paramAccountVo = JSON.toJavaObject(paramObj, AccountVo.class);
        Long id = paramObj.getLong("id");

        AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(paramAccountVo.getProtocolId());
        if (protocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(paramAccountVo.getProtocolId());
        }
        paramAccountVo.setProtocol(protocolVo.getName());
        if (!StringUtils.equals(protocolVo.getName(), "tagent") && StringUtils.isEmpty(paramObj.getString("account"))) {
            throw new ResourceCenterAccountNameIsNotNullException();
        }
        String type = paramAccountVo.getType();
        if (Objects.equals(type, AccountType.PUBLIC.getValue())) {
            if (resourceAccountMapper.checkAccountNameIsRepeats(paramAccountVo) > 0) {
                throw new ResourceCenterAccountNameRepeatsException(paramAccountVo.getName());
            }
        } else {
            Long resourceId = paramAccountVo.getResourceId();
            if (resourceId == null) {
                throw new ParamNotExistsException("resourceId");
            }
            List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
            for (AccountVo accountVo : accountVoList) {
                if (Objects.equals(paramAccountVo.getName(), accountVo.getName()) && !Objects.equals(paramAccountVo.getId(), accountVo.getId())) {
                    new ResourceCenterAccountNameRepeatsException(paramAccountVo.getName());
                }
            }
        }
        // 如果是私有类型帐号，需要校验该资产中所有公有和私有帐号中是否存在帐号及协议都相同的，如果存在则不能更新
        if (Objects.equals(type, AccountType.PRIVATE.getValue())) {
            Long resourceId = paramObj.getLong("resourceId");
            if (resourceId == null) {
                throw new ParamNotExistsException("资产ID（resourceId）");
            }
            List<String> failureReasonList = check(resourceId, paramAccountVo);
            if (CollectionUtils.isNotEmpty(failureReasonList)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("failureReasonList", failureReasonList);
                return resultObj;
            }
            List<ResourceAccountVo> resourceAccountVoList = new ArrayList<>();
            resourceAccountVoList.add(new ResourceAccountVo(resourceId, paramAccountVo.getId()));
            resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
        }
        List<Long> tagIdList = paramAccountVo.getTagIdList();
        List<AccountTagVo> accountTagVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagIdList)) {
            List<Long> notFoundTagIdList = new ArrayList<>();
            List<Long> searchTagIdList = null;
            List<Long> insertTagIdList = new ArrayList<>();
            insertTagIdList.addAll(tagIdList);
            List<TagVo> tagVoList = resourceTagMapper.searchTagListByIdList(tagIdList);
            searchTagIdList = tagVoList.stream().map(TagVo::getId).collect(Collectors.toList());
            insertTagIdList.removeAll(searchTagIdList);
            if (CollectionUtils.isNotEmpty(insertTagIdList)) {
                notFoundTagIdList.addAll(insertTagIdList);
                if (CollectionUtils.isNotEmpty(notFoundTagIdList)) {
                    throw new ResourceCenterTagNotFoundException(notFoundTagIdList);
                }
            }
            resourceAccountMapper.deleteAccountTagByAccountId(paramAccountVo.getId());
            for (Long tagId : tagIdList) {
                accountTagVoList.add(new AccountTagVo(paramAccountVo.getId(), tagId));
                if (accountTagVoList.size() > 100) {
                    resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
                    accountTagVoList.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(accountTagVoList)) {
                resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
            }
        }
        paramAccountVo.setLcu(UserContext.get().getUserUuid());

        //一个协议只能存一个默认帐号。例如，ssh协议当前默认帐号为app，如果在编辑root帐号时，把root设置为默认帐号，需要替换掉原有的app默认帐号表示标识。root代替app成为了新的ssh协议默认帐号。
        if (Objects.equals(type, AccountType.PUBLIC.getValue()) && paramAccountVo.getIsDefault() == 1) {
            resourceAccountMapper.resetAccountDefaultByProtocolId(paramAccountVo.getProtocolId());
        }

        if (id != null) {
            AccountVo oldVo = resourceAccountMapper.getAccountById(id);
            if (oldVo == null) {
                throw new ResourceCenterAccountNotFoundException(id);
            }
            paramAccountVo.setProtocolId(protocolVo.getId());
            resourceAccountMapper.updateAccount(paramAccountVo);
        } else {
            if (Objects.equals(protocolVo.getName(), "tagent")) {
                throw new ResourceCenterAccountNotCreateTagentAccountException();
            }
            paramAccountVo.setFcu(UserContext.get().getUserUuid());
            resourceAccountMapper.insertAccount(paramAccountVo);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("id", paramAccountVo.getId());
        return resultObj;
    }

    public IValid name() {
        return value -> {
            AccountVo vo = JSON.toJavaObject(value, AccountVo.class);
            if (Objects.equals(AccountType.PUBLIC.getValue(), vo.getType())) {
                if (resourceAccountMapper.checkAccountNameIsRepeats(vo) > 0) {
                    return new FieldValidResultVo(new ResourceCenterAccountNameRepeatsException(vo.getName()));
                }
            } else {
                Long resourceId = vo.getResourceId();
                if (resourceId == null) {
                    throw new ParamNotExistsException("资产ID（resourceId）");
                }
                List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
                for (AccountVo accountVo : accountVoList) {
                    if (Objects.equals(vo.getName(), accountVo.getName()) && !Objects.equals(vo.getId(), accountVo.getId())) {
                        return new FieldValidResultVo(new ResourceCenterAccountNameRepeatsException(vo.getName()));
                    }
                }
            }
            return new FieldValidResultVo();
        };
    }

    /**
     * 校验该资产中所有公有和私有帐号中是否存在帐号及协议都相同的
     * @param resourceId 资产ID
     * @param newAccountVo 新帐号信息
     * @return
     */
    private List<String> check(Long resourceId, AccountVo newAccountVo) {
        List<String> failureReasonList = new ArrayList<>();
        Map<String, AccountVo> accountVoMap = new HashMap<>();
        List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
        Iterator<AccountVo> iterator = accountVoList.iterator();
        while(iterator.hasNext()) {
            AccountVo accountVo = iterator.next();
            if (Objects.equals(accountVo.getId(), newAccountVo.getId())) {
                iterator.remove();
            }
        }
        accountVoList.add(newAccountVo);
        for (AccountVo accountVo : accountVoList) {
            String key = accountVo.getProtocol() + "#" + accountVo.getAccount();
            AccountVo account = accountVoMap.get(key);
            if (account == null) {
                accountVoMap.put(key, accountVo);
            } else {
                failureReasonList.add("选中项中\"" + accountVo.getName() + "（" + accountVo.getProtocol() + "/" + accountVo.getAccount() + "）\"与\"" + account.getName() + "（" + account.getProtocol() + "/" + account.getAccount() + "）\"");
            }
        }
        return failureReasonList;
    }
}
