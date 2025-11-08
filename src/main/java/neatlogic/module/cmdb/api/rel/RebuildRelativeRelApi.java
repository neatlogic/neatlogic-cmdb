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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.relativerel.RelativeRelManager;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class RebuildRelativeRelApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/rel/relative/rebuild";
    }

    @Override
    public String getName() {
        return "重建级联关系数据";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "relId", type = ApiParamType.LONG, isRequired = true, desc = "关系id")})
    @Description(desc = "重建级联关系数据接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long relId = jsonObj.getLong("relId");
        RelativeRelManager.rebuild(relId);
        return null;
    }

}
