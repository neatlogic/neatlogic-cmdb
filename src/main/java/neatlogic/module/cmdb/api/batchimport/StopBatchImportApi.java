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

package neatlogic.module.cmdb.api.batchimport;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.plugin.BatchImportHandler;
import org.springframework.stereotype.Service;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class StopBatchImportApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/import/stop";
    }

    @Override
    public String getName() {
        return "nmcab.stopbatchimportapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.importauditid")})
    @Output({})
    @Description(desc = "nmcab.stopbatchimportapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        BatchImportHandler.stopImportById(id);
        return null;
    }
}
