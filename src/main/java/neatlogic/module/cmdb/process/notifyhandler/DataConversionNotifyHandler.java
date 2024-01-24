/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.process.notifyhandler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.core.NotifyHandlerType;
import neatlogic.framework.notify.core.NotifyPolicyHandlerBase;
import neatlogic.framework.notify.dto.NotifyTriggerTemplateVo;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import neatlogic.module.cmdb.process.stephandler.CmdbProcessStepHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataConversionNotifyHandler extends NotifyPolicyHandlerBase {

    @Override
    public String getName() {
        return CmdbProcessStepHandlerType.DATACONVERSION.getName();
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        return null;
    }

    @Override
    protected List<NotifyTriggerTemplateVo> myNotifyTriggerTemplateList(NotifyHandlerType type) {
        return null;
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        return null;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return null;
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {

    }

    @Override
    public String getAuthName() {
        return CIENTITY_MODIFY.class.getSimpleName();
    }

}
