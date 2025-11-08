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

package neatlogic.module.cmdb.api.resourcecenter.account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.enums.resourcecenter.AccountType;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNameRepeatsException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.service.resourcecenter.account.ResourceCenterAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AccountSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterAccountService resourceCenterAccountService;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/account/save";
    }

    @Override
    public String getName() {
        return "nmcara.accountsaveapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 200, isRequired = true, desc = "common.name"),
            @Param(name = "account", type = ApiParamType.STRING, maxLength = 80, desc = "common.username"),
            @Param(name = "passwordPlain", type = ApiParamType.STRING, isRequired = false, desc = "common.password"),
            @Param(name = "protocolId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.protocol"),
            @Param(name = "port", type = ApiParamType.INTEGER, isRequired = false, desc = "term.cmdb.port"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = false, desc = "common.tagidlist"),
            @Param(name = "type", type = ApiParamType.ENUM, member = AccountType.class, isRequired = true, desc = "common.type"),
            @Param(name = "isDefault", type = ApiParamType.INTEGER, desc = "common.isdefault"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "term.cmdb.resourceid")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id")
    })
    @Description(desc = "nmcara.accountsaveapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountVo paramAccountVo = JSON.toJavaObject(paramObj, AccountVo.class);
        Long id = paramObj.getLong("id");
        return resourceCenterAccountService.saveAccount(id, paramAccountVo);
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
}
