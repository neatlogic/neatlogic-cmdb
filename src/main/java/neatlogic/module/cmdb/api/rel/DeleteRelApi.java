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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.rel.RelNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.rel.RelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteRelApi extends PrivateApiComponentBase {

    @Resource
    private RelService relService;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/delete";
    }

    @Override
    public String getName() {
        return "删除模型关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "关系id")})
    @Description(desc = "删除模型关系")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long relId = jsonObj.getLong("id");
        RelVo relVo = relMapper.getRelById(relId);
        if (relVo == null) {
            throw new RelNotFoundException(relId);
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(relVo.getFromCiId()).check()) {
            CiVo ciVo = ciMapper.getCiById(relVo.getFromCiId());
            throw new CiAuthException(ciVo.getLabel());
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(relVo.getToCiId()).check()) {
            CiVo ciVo = ciMapper.getCiById(relVo.getToCiId());
            throw new CiAuthException(ciVo.getLabel());
        }
        relService.deleteRel(relVo);
        return null;
    }

}
