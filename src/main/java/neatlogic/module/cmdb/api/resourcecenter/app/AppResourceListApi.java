/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.resourcecenter.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/17 11:54
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppResourceListApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "resourcecenter/app/resource/list";
    }

    @Override
    public String getName() {
        return "查询应用中资源列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id,envId=-2表示无配置环境"),
            @Param(name = "viewName", type = ApiParamType.STRING, desc = "视图名称"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, defaultValue = "1", desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER,  defaultValue = "20", desc = "每页数据条目")
    })
    @Output({
            @Param(name = "tableList", type = ApiParamType.JSONARRAY, desc = "资源环境列表")
    })
    @Description(desc = "查询资源环境列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        if (appSystemId == null && appModuleId == null) {
            throw new ParamNotExistsException("应用id（appSystemId）", "应用模块id（appModuleId）");
        }
        Long envId = paramObj.getLong("envId");
        Integer currentPage = paramObj.getInteger("currentPage");
        Integer pageSize = paramObj.getInteger("pageSize");
        String viewName = paramObj.getString("viewName");
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        JSONArray tableList = resourceCenterDataSource.getAppResourceList(appSystemId, appModuleId, envId, null, viewName, currentPage, pageSize);
        resultObj.put("tableList", tableList);
        return resultObj;
    }
}
