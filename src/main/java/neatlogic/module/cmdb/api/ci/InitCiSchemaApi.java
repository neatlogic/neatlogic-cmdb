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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.cmdb.service.ci.CiService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class InitCiSchemaApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    private CiService ciService;


    @Override
    public String getToken() {
        return "/cmdb/ci/schema/init";
    }

    @Override
    public String getName() {
        return "nmcac.initcischemaapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Description(desc = "nmcac.initcischemaapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ciService.initCiTableView();
        return null;
    }

}
