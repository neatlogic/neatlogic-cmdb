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
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class SaveCiTopoTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/topotemplate/save";
    }

    @Override
    public String getName() {
        return "保存拓扑场景";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid", isRequired = true),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name", isRequired = true),
            @Param(name = "isDefault", type = ApiParamType.INTEGER, desc = "common.isdefault", rule = "0,1", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive", rule = "0,1", isRequired = true),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "common.config")
    })
    @Output({@Param(explode = CiTopoTemplateVo.class)})
    @Description(desc = "保存拓扑场景")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTopoTemplateVo ciTopoTemplateVo = JSONObject.toJavaObject(jsonObj, CiTopoTemplateVo.class);
        Long id = jsonObj.getLong("id");
        if (ciTopoTemplateVo.getIsDefault().equals(1)) {
            ciTopoTemplateMapper.resetCiTopoTemplateIsDefault(ciTopoTemplateVo);
        }
        if (id != null) {
            ciTopoTemplateMapper.updateCiTopoTemplate(ciTopoTemplateVo);
        } else {
            ciTopoTemplateMapper.insertCiTopoTemplate(ciTopoTemplateVo);

        }
        return null;
    }
}
