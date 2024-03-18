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
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityAlertApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/alert/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchcientityalertapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityIdList", type = ApiParamType.JSONARRAY, desc = "nmcac.getcientitybaseinfolistapi.input.param.desc.idlist", help = "nmcac.searchcientityalertapi.input.param.help.idlist"),
            @Param(name = "groupIdList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityalertapi.input.param.desc.groupidlist", help = "nmcac.searchcientityalertapi.input.param.help.groupidlist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage")
    })
    @Output({@Param(explode = BasePageVo[].class)})
    @Description(desc = "nmcac.searchcientityalertapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityAlertVo ciEntityAlertVo = JSONObject.toJavaObject(jsonObj, CiEntityAlertVo.class);
        int rowNum = ciEntityAlertMapper.searchCiEntityAlertCount(ciEntityAlertVo);
        List<CiEntityAlertVo> ciEntityAlertVoList = null;
        if (rowNum > 0) {
            ciEntityAlertVoList = ciEntityAlertMapper.searchCiEntityAlert(ciEntityAlertVo);
            ciEntityAlertVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(ciEntityAlertVoList, ciEntityAlertVo);
    }


}
