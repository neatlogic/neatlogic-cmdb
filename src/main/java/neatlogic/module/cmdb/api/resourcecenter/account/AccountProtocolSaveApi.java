/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.resourcecenter.account;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
        return "保存账号管理协议";
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
            @Param(name = "id", type = ApiParamType.LONG,  desc = "协议ID"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "协议名称"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "协议端口"),

    })
    @Output({
    })
    @Description(desc = "保存账号管理协议")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountProtocolVo accountProtocolVo = JSON.toJavaObject(paramObj, AccountProtocolVo.class);
        if (resourceAccountMapper.checkAccountProtocolIsRepeats(accountProtocolVo) > 0) {
            throw new ResourceCenterAccountProtocolRepeatException(accountProtocolVo.getName());
        }
        Long id = paramObj.getLong("id");
        if (id != null) {
            resourceAccountMapper.updateAccountProtocol(accountProtocolVo);
        } else {
            resourceAccountMapper.insertAccountProtocol(accountProtocolVo);
        }
        return null;
    }


}
