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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityStatusVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListGraphStatusApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Override
    public String getToken() {
        return "/cmdb/graph/status/list";
    }

    @Override
    public String getName() {
        return "获取视图告警状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "graphIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项id列表"),
    })
    @Output({@Param(explode = CiEntityStatusVo[].class)})
    @Description(desc = "获取视图告警状态接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciEntityIdObjectList = jsonObj.getJSONArray("ciEntityIdList");
        List<Long> ciEntityIdList = new ArrayList<>();
        for (int i = 0; i < ciEntityIdObjectList.size(); i++) {
            ciEntityIdList.add(ciEntityIdObjectList.getLong(i));
        }
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            return ciEntityAlertMapper.listCiEntityStatus(ciEntityIdList);
        }
        return null;
    }


}
