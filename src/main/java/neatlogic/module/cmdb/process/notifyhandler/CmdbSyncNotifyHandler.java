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
