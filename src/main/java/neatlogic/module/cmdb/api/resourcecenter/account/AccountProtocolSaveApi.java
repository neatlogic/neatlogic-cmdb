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
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = RESOURCECENTER_ACCOUNT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AccountProtocolSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "nmcara.accountprotocolsaveapi.getname";
    }

    @Override
    public String getToken() {
        return "resourcecenter/account/protocol/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG,  desc = "common.id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, maxLength = 50, desc = "common.name"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "term.cmdb.port"),
    })
    @Output({
    })
    @Description(desc = "nmcara.accountprotocolsaveapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountProtocolVo accountProtocolVo = JSON.toJavaObject(paramObj, AccountProtocolVo.class);
        if (resourceAccountMapper.checkAccountProtocolIsRepeats(accountProtocolVo) > 0) {
            throw new ResourceCenterAccountProtocolRepeatException(accountProtocolVo.getName());
        }
        Long id = paramObj.getLong("id");
        if (id != null && resourceAccountMapper.getAccountProtocolVoByProtocolId(id) == null) {
                throw new ResourceCenterAccountProtocolNotFoundException(id);

        }
        resourceAccountMapper.insertAccountProtocol(accountProtocolVo);
        return null;
    }


}
