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

package neatlogic.module.cmdb.api.resourcecenter.account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
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
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
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
        return "保存资源中心账号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "账号ID"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 50, isRequired = true, desc = "名称"),
            @Param(name = "account", type = ApiParamType.STRING, maxLength = 50, desc = "用户名"),
            @Param(name = "passwordPlain", type = ApiParamType.STRING, isRequired = false, desc = "密码"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "协议id"),
            @Param(name = "port", type = ApiParamType.INTEGER, isRequired = false, desc = "端口"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = false, desc = "标签id列表"),
            @Param(name = "type", type = ApiParamType.ENUM, member = AccountType.class, isRequired = true, desc = "标签id列表"),
            @Param(name = "isDefault", type = ApiParamType.INTEGER, desc = "是否默认账号"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产ID")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "账号ID")
    })
    @Description(desc = "保存资源中心账号")
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
                    throw new ResourceCenterAccountNameRepeatsException(paramAccountVo.getName());
                }
            }
        }
        // 如果是私有类型账号，需要校验该资产中所有公有和私有账号中是否存在账号及协议都相同的，如果存在则不能更新
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
            List<Long> searchTagIdList = null;
            List<Long> insertTagIdList = new ArrayList<>(tagIdList);
            List<TagVo> tagVoList = resourceTagMapper.searchTagListByIdList(tagIdList);
            searchTagIdList = tagVoList.stream().map(TagVo::getId).collect(Collectors.toList());
            insertTagIdList.removeAll(searchTagIdList);
            if (CollectionUtils.isNotEmpty(insertTagIdList)) {
                List<Long> notFoundTagIdList = new ArrayList<>(insertTagIdList);
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

        //一个协议只能存一个默认账号。例如，ssh协议当前默认账号为app，如果在编辑root账号时，把root设置为默认账号，需要替换掉原有的app默认账号表示标识。root代替app成为了新的ssh协议默认账号。
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
     * 校验该资产中所有公有和私有账号中是否存在账号及协议都相同的
     *
     * @param resourceId   资产ID
     * @param newAccountVo 新账号信息
     */
    private List<String> check(Long resourceId, AccountVo newAccountVo) {
        List<String> failureReasonList = new ArrayList<>();
        Map<String, AccountVo> accountVoMap = new HashMap<>();
        List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
        accountVoList.removeIf(accountVo -> Objects.equals(accountVo.getId(), newAccountVo.getId()));
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
