/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.cmdb.api.resourcecenter.appmodule;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAppModuleListForTreeApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appmodule/list/fortree";
    }

    @Override
    public String getName() {
        return "nmcara.listappmodulelistfortreeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid")
    })
    @Output({
            @Param(name = "tbodyList", explode = AppModuleVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmcara.listappmodulelistfortreeapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        List<AppModuleVo> tbodyList = resourceMapper.getAppModuleListByAppSystemId(appSystemId);
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            Map<Long, Long> appEnvCountMap = new HashMap<>();
            List<Map<String, Long>> appEnvCountMapList = resourceMapper.getAppEnvCountMapByAppSystemIdGroupByAppModuleId(appSystemId);
            for (Map<String, Long> map : appEnvCountMapList) {
                Long count = map.get("count");
                Long appModuleId = map.get("appModuleId");
                appEnvCountMap.put(appModuleId, count);
            }
            for (AppModuleVo appModuleVo : tbodyList) {
                Long count = appEnvCountMap.get(appModuleVo.getId());
                if (count == null) {
                    appModuleVo.setIsHasEnv(0);
                } else if (count == 0) {
                    appModuleVo.setIsHasEnv(0);
                } else {
                    appModuleVo.setIsHasEnv(1);
                }
            }
        }
        return tbodyList;
    }
}
