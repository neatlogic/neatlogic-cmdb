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

package neatlogic.module.cmdb.api.ci;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiAuthApi extends PrivateApiComponentBase {

    @Autowired
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
                CiAuthVo ciAuthVo = JSONObject.toJavaObject(authObj, CiAuthVo.class);
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
