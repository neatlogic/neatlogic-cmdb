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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.cientity.AttrEntityNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPasswordAttrPlaintextApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getName() {
        return "nmcac.getpasswordattrplaintextapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "nmcaa.getattrapi.input.param.desc.id")})
    @Output({@Param(type = ApiParamType.STRING, desc = "nmcac.getpasswordattrplaintextapi.output.param.desc")})
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long attrId = jsonObj.getLong("attrId");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(attrVo.getCiId(), ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        if (!CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityId, GroupType.MAINTAIN).check()) {
            throw new CiAuthException();
        }

        AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrId);
        if (attrEntityVo == null) {
            throw new AttrEntityNotFoundException(attrId);
        }
        if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
            IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrEntityVo.getAttrType());
            handler.transferValueListToDisplay(attrVo, attrEntityVo.getValueList());
            return attrEntityVo.getValueList().getString(0);
        }
        return "";
    }


    @Override
    public String getToken() {
        return "/cmdb/attrentity/getplaintext";
    }
}
