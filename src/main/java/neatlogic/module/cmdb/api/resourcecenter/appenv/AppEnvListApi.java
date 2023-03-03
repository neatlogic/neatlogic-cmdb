/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.cmdb.api.resourcecenter.appenv;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.AppSystemMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/16 15:04
 **/
//@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Deprecated
public class AppEnvListApi extends PrivateApiComponentBase {

    @Resource
    AppSystemMapper appSystemMapper;

    @Override
    public String getToken() {
        return "resourcecenter/app/env/list";
    }

    @Override
    public String getName() {
        return "查询应用系统环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
    })
    @Output({
            @Param(explode = AppEnvironmentVo[].class, desc = "应用模块环境列表"),
    })
    @Description(desc = "查询应用系统环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) {
        JSONArray appModuleIdArray = paramObj.getJSONArray("appModuleIdList");
        List<Long> appModuleIdList = null;
        if (CollectionUtils.isNotEmpty(appModuleIdArray)) {
            appModuleIdList = appModuleIdArray.toJavaList(Long.class);
        }
        Long appSystemId = paramObj.getLong("appSystemId");
        return appSystemMapper.getAppEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleIdList);
    }
}
