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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.attr.AttrNotAllowBeNameException;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.ci.CiService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiNameAttrIdApi extends PrivateApiComponentBase {

    @Resource
    private CiService ciService;

    @Resource
    private AttrMapper attrMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/savenameattr";
    }

    @Override
    public String getName() {
        return "nmcac.savecinameattridapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", isRequired = true, type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "nmcaa.getattrapi.input.param.desc.id")
    })
    @Description(desc = "nmcac.savecinameattridapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long attrId = jsonObj.getLong("attrId");
        CiVo ciVo = ciService.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        } else {
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CiAuthException();
            }
            AttrVo attrVo = attrMapper.getAttrById(attrId);
            if (attrVo == null) {
                throw new AttrNotFoundException(attrId);
            }
            if (attrVo.getAllowBeName()) {
                ciVo.setNameAttrId(attrId);
                ciService.updateCiNameAttrId(ciVo);
            } else {
                throw new AttrNotAllowBeNameException();
            }
        }
        return null;
    }

}
