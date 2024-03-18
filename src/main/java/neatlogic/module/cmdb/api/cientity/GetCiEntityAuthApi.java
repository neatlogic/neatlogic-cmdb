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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
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
