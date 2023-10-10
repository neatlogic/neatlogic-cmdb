/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

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
