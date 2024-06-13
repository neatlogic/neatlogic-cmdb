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
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.utils.RelPathBuilder;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiTopoTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/topotemplate/get";
    }

    @Override
    public String getName() {
        return "nmcac.getcitopotemplateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
            @Param(name = "needRelPath", desc = "term.cmdb.needcirel", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = CiTopoTemplateVo.class)})
    @Description(desc = "nmcac.getcitopotemplateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTopoTemplateVo ciTopoTemplateVo = ciTopoTemplateMapper.getCiTopoTemplateById(jsonObj.getLong("id"));
        Integer needRelPath = jsonObj.getInteger("needRelPath");
        if (needRelPath != null && needRelPath.equals(1)) {
            //以下逻辑是为了可以正确回显已经选择的关系路径
            JSONArray ciRelList = ciTopoTemplateVo.getConfig().getJSONArray("ciRelList");
            Long ciId = ciTopoTemplateVo.getCiId();
            if (ciId != null) {
                JSONObject root = new JSONObject();
                RelPathBuilder.build(root, 0, ciRelList, ciId);
                if (MapUtils.isNotEmpty(root)) {
                    ciTopoTemplateVo.setRelPath(root.getJSONArray("children"));
                }
            }
        }
        return ciTopoTemplateVo;
    }
}
