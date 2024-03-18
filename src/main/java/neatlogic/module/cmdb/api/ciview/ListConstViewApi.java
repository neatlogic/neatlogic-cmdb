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

package neatlogic.module.cmdb.api.ciview;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.view.ViewConstVo;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListConstViewApi extends PrivateApiComponentBase {

    @Resource
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ciview/listconst";
    }

    @Override
    public String getName() {
        return "获取内部属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "显示类型"),
            @Param(name = "needAlias", type = ApiParamType.INTEGER, desc = "term.cmdb.needalias", rule = "0,1")})
    @Output({@Param(explode = ViewConstVo[].class)})
    @Description(desc = "获取内部属性列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String showType = jsonObj.getString("showType");
        int needAlias = jsonObj.getIntValue("needAlias");
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        ciViewVo.addShowType(showType);
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        List<ViewConstVo> constList = ciViewMapper.getCiViewConstByCiId(ciId);
        Set<Long> constSet = new HashSet<>();
        for (CiViewVo ciView : ciViewList) {
            if (needAlias == 1 && StringUtils.isNotBlank(ciView.getAlias()) && ciView.getType().equals("const")) {
                Optional<ViewConstVo> op = constList.stream().filter(d -> d.getId().equals(ciView.getItemId())).findFirst();
                op.ifPresent(attrVo -> attrVo.setLabel(ciView.getAlias()));
            }
            if (ciView.getType().equals("const")) {
                constSet.add(ciView.getItemId());
            }
        }
        constList.removeIf(cons -> !constSet.contains(cons.getId()));
        return constList;
    }
}
