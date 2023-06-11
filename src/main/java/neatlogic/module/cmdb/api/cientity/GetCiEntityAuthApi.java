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

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityAuthApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;


    @Override
    public String getToken() {
        return "/cmdb/cientity/auth/get";
    }

    @Override
    public String getName() {
        return "nmcac.getcientityauthapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, rule = "cientityinsert,cientitydelete,cimanage,transactionmanage,cientityrecover,passwordview", desc = "nmcac.getcientityauthapi.input.param.desc.authlist")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "nmcac.getcientityauthapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityBaseInfoById(jsonObj.getLong("ciEntityId"));
        //如果需要获取恢复权限，配置项已经被删除，需要改用模型id获取权限
        Long ciId = (ciEntityVo != null ? ciEntityVo.getCiId() : jsonObj.getLong("ciId"));
        Map<String, Boolean> authMap = new HashMap<>();
        if (ciId != null) {
            JSONArray authList = jsonObj.getJSONArray("authList");
            if (CollectionUtils.isNotEmpty(authList)) {
                for (int i = 0; i < authList.size(); i++) {
                    String authString = authList.getString(i);
                    CiAuthType auth = CiAuthType.get(authString);
                    if (auth != null) {
                        authMap.put(authString, CiAuthChecker.chain().checkAuth(ciId, auth).check());
                    }
                }
            } else {
                for (CiAuthType auth : CiAuthType.values()) {
                    authMap.put(auth.getValue(), CiAuthChecker.chain().checkAuth(ciId, auth).check());
                }
            }
        }
        return authMap;
    }


}
