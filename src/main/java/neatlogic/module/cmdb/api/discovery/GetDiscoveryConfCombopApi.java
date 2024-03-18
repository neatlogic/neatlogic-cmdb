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

package neatlogic.module.cmdb.api.discovery;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.discovery.DiscoverConfCombopVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDiscoveryConfCombopApi extends PrivateApiComponentBase {

    @Resource
    private DiscoveryMapper discoveryMapper;


    @Override
    public String getToken() {
        return "/cmdb/discovery/combop/get";
    }

    @Override
    public String getName() {
        return "获取自动发现配置组合工具关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "confId", type = ApiParamType.LONG, isRequired = true, desc = "自动发现配置id")})
    @Output({@Param(explode = DiscoverConfCombopVo.class)})
    @Description(desc = "获取自动发现配置组合工具关系接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return discoveryMapper.getDiscoveryConfCombopByConfId(jsonObj.getLong("confId"));
    }

}
