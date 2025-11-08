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

package neatlogic.module.cmdb.process.notifyhandler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.process.constvalue.ProcessTaskGroupSearch;
import neatlogic.framework.process.constvalue.ProcessUserType;
import neatlogic.framework.process.notify.core.ProcessTaskNotifyHandlerBase;
import neatlogic.module.cmdb.process.stephandler.CmdbProcessStepHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CmdbSyncNotifyHandler extends ProcessTaskNotifyHandlerBase {

    @Override
    public String getName() {
        return CmdbProcessStepHandlerType.CMDBSYNC.getName();
    }

    @Override
    protected void myCustomAuthorityConfig(JSONObject config) {
        List<String> excludeList = config.getJSONArray("excludeList").toJavaList(String.class);
        excludeList.add(ProcessTaskGroupSearch.PROCESSUSERTYPE.getValue() + "#" + ProcessUserType.MINOR.getValue());
        config.put("excludeList", excludeList);
    }

    @Override
    public String getAuthName() {
        return CIENTITY_MODIFY.class.getSimpleName();
    }

    @Override
    public String getModuleGroup() {
        return "process";
    }

}
