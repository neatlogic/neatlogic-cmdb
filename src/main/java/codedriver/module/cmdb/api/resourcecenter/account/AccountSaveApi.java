/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountTagVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.enums.resourcecenter.AccountType;
import codedriver.framework.cmdb.exception.resourcecenter.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
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
            @Param(name = "passwordPlain", type = ApiParamType.STRING, maxLength = 50, isRequired = false, desc = "密码"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "协议id"),
            @Param(name = "port", type = ApiParamType.INTEGER, isRequired = false, desc = "端口"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = false, desc = "标签id列表"),
            @Param(name = "type", type = ApiParamType.ENUM, member = AccountType.class, isRequired = true, desc = "标签id列表"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产ID")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "账号ID")
    })
    @Description(desc = "保存资源中心账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountVo vo = JSON.toJavaObject(paramObj, AccountVo.class);
        Long id = paramObj.getLong("id");

        AccountProtocolVo protocolVo = resourceAccountMapper.getAccountProtocolVoByProtocolId(vo.getProtocolId());
        if (protocolVo == null) {
            throw new ResourceCenterAccountProtocolNotFoundException(vo.getProtocolId());
        }
        vo.setProtocol(protocolVo.getName());
        if (!StringUtils.equals(protocolVo.getName(), "tagent") && StringUtils.isEmpty(paramObj.getString("account"))) {
            throw new ResourceCenterAccountNameIsNotNullException();
        }
        String type = vo.getType();
        if (Objects.equals(type, AccountType.PUBLIC.getValue())) {
            if (resourceAccountMapper.checkAccountNameIsRepeats(vo) > 0) {
                throw new ResourceCenterAccountNameRepeatsException(vo.getName());
            }
        } else {
            Long resourceId = vo.getResourceId();
            if (resourceId == null) {
                throw new ParamNotExistsException("资产ID（resourceId）");
            }
            List<AccountVo> accountVoList = resourceAccountMapper.getResourceAccountListByResourceId(resourceId);
            for (AccountVo accountVo : accountVoList) {
                if (Objects.equals(vo.getName(), accountVo.getName()) && !Objects.equals(vo.getId(), accountVo.getId())) {
                    new ResourceCenterAccountNameRepeatsException(vo.getName());
                }
            }
        }
        // 如果是私有类型账号，需要校验该资产中所有公有和私有账号中是否存在账号及协议都相同的，如果存在则不能更新
        if (Objects.equals(type, AccountType.PRIVATE.getValue())) {
            Long resourceId = paramObj.getLong("resourceId");
            if (resourceId == null) {
                throw new ParamNotExistsException("资产ID（resourceId）");
            }
            List<String> failureReasonList = check(resourceId, vo);
            if (CollectionUtils.isNotEmpty(failureReasonList)) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("failureReasonList", failureReasonList);
                return resultObj;
            }
            List<ResourceAccountVo> resourceAccountVoList = new ArrayList<>();
            resourceAccountVoList.add(new ResourceAccountVo(resourceId, vo.getId()));
            resourceAccountMapper.insertIgnoreResourceAccount(resourceAccountVoList);
        }
        List<Long> tagIdList = vo.getTagIdList();
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
            resourceAccountMapper.deleteAccountTagByAccountId(vo.getId());
            for (Long tagId : tagIdList) {
                accountTagVoList.add(new AccountTagVo(vo.getId(), tagId));
                if (accountTagVoList.size() > 100) {
                    resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
                    accountTagVoList.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(accountTagVoList)) {
                resourceAccountMapper.insertIgnoreAccountTag(accountTagVoList);
            }
        }
        vo.setLcu(UserContext.get().getUserUuid());
        if (id != null) {
            AccountVo oldVo = resourceAccountMapper.getAccountById(id);
            if (oldVo == null) {
                throw new ResourceCenterAccountNotFoundException(id);
            }
            vo.setProtocolId(protocolVo.getId());
            resourceAccountMapper.updateAccount(vo);
        } else {
            if (Objects.equals(protocolVo.getName(), "tagent")) {
                throw new ResourceCenterAccountNotCreateTagentAccountException();
            }
            vo.setFcu(UserContext.get().getUserUuid());
            resourceAccountMapper.insertAccount(vo);
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("id", vo.getId());
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
     * @param resourceId 资产ID
     * @param newAccountVo 新账号信息
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
