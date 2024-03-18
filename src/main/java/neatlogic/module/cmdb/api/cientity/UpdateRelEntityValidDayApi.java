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

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateRelEntityValidDayApi extends PrivateApiComponentBase {

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/relentity/updatevalidday";
    }

    @Override
    public String getName() {
        return "nmcac.updaterelentityvaliddayapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "nmcac.searchcientityapi.input.param.desc.relid"),
            @Param(name = "validDay", type = ApiParamType.INTEGER, isRequired = true, desc = "nmcac.updaterelentityvaliddayapi.input.param.desc"),
    })
    @Description(desc = "nmcac.updaterelentityvaliddayapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer validDay = jsonObj.getInteger("validDay");
        RelEntityVo relEntityVo = relEntityMapper.getRelEntityById(id);
        relEntityVo.setValidDay(Math.max(0, validDay));
        //关系两端随便一个有权限即可进行编辑
        if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(relEntityVo.getFromCiId()).checkCiEntityIsInGroup(relEntityVo.getFromCiEntityId(), GroupType.MAINTAIN).check()
                && !CiAuthChecker.chain().checkCiEntityUpdatePrivilege(relEntityVo.getToCiId()).checkCiEntityIsInGroup(relEntityVo.getToCiEntityId(), GroupType.MAINTAIN).check()) {
            throw new CiEntityAuthException(TransactionActionType.UPDATE.getText());
        }
        relEntityMapper.updateRelEntityValidDay(relEntityVo);
        return null;
    }

}
