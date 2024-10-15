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


package neatlogic.module.cmdb.publicapi;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;

import javax.annotation.Resource;
import java.util.Date;

//暂时没用
@Deprecated
//@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateCiEntityMonitorStatusApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityMapper ciEntityMapper;


    @Override
    public String getToken() {
        return "cmdb/cientity/updatemonitorstatus";
    }

    @Override
    public String getName() {
        return "修改配置项监控状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "monitorStatus", type = ApiParamType.ENUM, rule = "normal,warn,critical,fatal", isRequired = true, desc = "监控状态"),
            @Param(name = "monitorTime", type = ApiParamType.LONG, isRequired = true, desc = "监控时间，格式：距1970年1月1日0时0分0秒的毫秒数")})
    @Output({})
    @Description(desc = "修改配置项监控接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(paramObj.getLong("ciEntityId"));
        ciEntityVo.setMonitorTime(new Date(paramObj.getLong("monitorTime")));
        ciEntityVo.setMonitorStatus(paramObj.getString("monitorStatus"));
        ciEntityMapper.updateCiEntityMonitorStatus(ciEntityVo);
        return null;
    }


}
