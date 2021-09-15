/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.account;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountTagVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.resourcecenter.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AccountSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "account", type = ApiParamType.STRING, maxLength = 50, isRequired = true, desc = "用户名"),
            @Param(name = "passwordPlain", type = ApiParamType.STRING, maxLength = 50, isRequired = false, desc = "密码"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "协议id"),
            @Param(name = "port", type = ApiParamType.INTEGER,isRequired = false, desc = "端口"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = false, desc = "标签id列表"),
    })
    @Output({
    })
    @Description(desc = "保存资源中心账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountVo vo = JSON.toJavaObject(paramObj, AccountVo.class);
        Long id = paramObj.getLong("id");
        Long protocolId = paramObj.getLong("protocolId");
        if (resourceCenterMapper.checkAccountNameIsRepeats(vo) > 0) {
            throw new ResourceCenterAccountNameRepeatsException(vo.getName());
        }
        List<Long> tagIdList = vo.getTagIdList();
        List<AccountTagVo> accountTagVoList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(tagIdList)) {
            List<Long> notFoundTagIdList = new ArrayList<>();
            List<Long> searchTagIdList = null;
            List<Long> insertTagIdList = new ArrayList<>();
            insertTagIdList.addAll(tagIdList);
            List<TagVo> tagVoList = resourceCenterMapper.searchTagListByIdList(tagIdList);
            searchTagIdList = tagVoList.stream().map(TagVo::getId).collect(Collectors.toList());
            insertTagIdList.removeAll(searchTagIdList);
            if (!CollectionUtils.isEmpty(insertTagIdList)) {
                notFoundTagIdList.addAll(insertTagIdList);
                if (!CollectionUtils.isEmpty(notFoundTagIdList)) {
                    throw new ResourceCenterTagNotFoundException(notFoundTagIdList);
                }
            }
            resourceCenterMapper.deleteAccountTagByAccountId(vo.getId());
            for (Long tagId : tagIdList) {
                accountTagVoList.add(new AccountTagVo(vo.getId(), tagId));
                if (accountTagVoList.size() > 100) {
                    resourceCenterMapper.insertIgnoreAccountTag(accountTagVoList);
                    accountTagVoList.clear();
                }
            }
            if (!CollectionUtils.isEmpty(accountTagVoList)) {
                resourceCenterMapper.insertIgnoreAccountTag(accountTagVoList);
            }
        }
        if (resourceCenterMapper.checkAccountProtocolIsExistsById(protocolId) == 0) {
            throw new ResourceCenterAccountProtocolNotFoundException(protocolId);
        }
        vo.setLcu(UserContext.get().getUserUuid());
        if (id != null) {
            AccountVo oldVo = resourceCenterMapper.getAccountById(id);
            if (oldVo == null) {
                throw new ResourceCenterAccountNotFoundException(id);
            }
            if (StringUtils.isNotEmpty(vo.getPasswordPlain())) {
                if (!Objects.equals(vo.getPasswordCipher(), oldVo.getPasswordCipher())) {
                    vo.setPasswordCipher(vo.getPasswordPlain());
                }
            }
            AccountProtocolVo protocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolId(vo.getProtocolId());
            vo.setProtocolId(protocolVo.getId());
            resourceCenterMapper.updateAccount(vo);
        } else {
            AccountProtocolVo protocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolId(protocolId);
            if (protocolVo==null) {
                throw new ResourceCenterAccountProtocolNotFoundByNameException(protocolVo.getName());
            }
            if (Objects.equals(protocolVo.getName(), "tagent")) {
                throw new ResourceCenterAccountNotCreateTagentAccount();
            }
            vo.setFcu(UserContext.get().getUserUuid());
            resourceCenterMapper.insertAccount(vo);
        }
        return null;
    }

    public IValid name() {
        return value -> {
            AccountVo vo = JSON.toJavaObject(value, AccountVo.class);
            if (resourceCenterMapper.checkAccountNameIsRepeats(vo) > 0) {
                return new FieldValidResultVo(new ResourceCenterAccountNameRepeatsException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
