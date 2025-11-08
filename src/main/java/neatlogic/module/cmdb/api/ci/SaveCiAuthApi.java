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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.ci.CiAuthInvalidException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiAuthApi extends PrivateApiComponentBase {

    @Resource
    private CiAuthMapper ciAuthMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/auth/save";
    }

    @Override
    public String getName() {
        return "nmcac.saveciauthapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY,
                    desc = "nmcac.saveciauthapi.input.param.desc.authlist")})
    @Description(desc = "nmcac.saveciauthapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        boolean hasAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
        if (!hasAuth) {
            throw new CiAuthException();
        }

        ciAuthMapper.deleteCiAuthByCiId(ciId);
        JSONArray authList = jsonObj.getJSONArray("authList");
        if (!CollectionUtils.isEmpty(authList)) {
            for (int i = 0; i < authList.size(); i++) {
                JSONObject authObj = authList.getJSONObject(i);
                CiAuthVo ciAuthVo = JSON.toJavaObject(authObj, CiAuthVo.class);
                ciAuthVo.setCiId(ciId);
                if (StringUtils.isBlank(ciAuthVo.getAction()) || StringUtils.isBlank(ciAuthVo.getAuthType())
                        || StringUtils.isBlank(ciAuthVo.getAuthUuid())) {
                    throw new CiAuthInvalidException();
                }
                ciAuthMapper.insertCiAuth(ciAuthVo);
            }
        }
        return null;
    }

}
