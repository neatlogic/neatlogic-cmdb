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

package neatlogic.module.cmdb.notify.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.core.INotifyTriggerType;
import neatlogic.framework.notify.core.NotifyPolicyHandlerBase;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import neatlogic.module.cmdb.notify.enums.CmdbNotifyParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CmdbNotifyPolicyHandler extends NotifyPolicyHandlerBase {
    @Override
    public String getName() {
        return "cmdb.ci";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return CI_MODIFY.class.getSimpleName();
    }

//    @Override
//    public INotifyPolicyHandlerGroup getGroup() {
//        return CmdbNotifyGroup.CMDB;
//    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        List<NotifyTriggerVo> returnList = new ArrayList<>();
        for (CmdbNotifyTriggerType triggerType : CmdbNotifyTriggerType.values()) {
            returnList.add(new NotifyTriggerVo(triggerType));
        }
        return returnList;
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        List<ConditionParamVo> notifyPolicyParamList = new ArrayList<>();
        for (CmdbNotifyParam param : CmdbNotifyParam.values()) {
            notifyPolicyParamList.add(createConditionParam(param));
        }
        return notifyPolicyParamList;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return new ArrayList<>();
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {

    }

    @Override
    public JSONObject convertData(Object object, INotifyTriggerType notifyTriggerType) {
        JSONObject returnData = super.convertData(object, notifyTriggerType);
        //JSONObject returnData = new JSONObject();
        CiVo ciVo = null;
        CiEntityVo ciEntityVo = null;
        List<CiEntityVo> ciEntityList = null;
        if (object instanceof CiVo) {
            ciVo = (CiVo) object;
        } else if (object instanceof CiEntityVo) {
            ciEntityVo = (CiEntityVo) object;
        } else if (object instanceof List<?>) {
            ciEntityList = (List<CiEntityVo>) object;
        } else if (object instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) object;
            if (dataMap.containsKey("ciVo")) {
                ciVo = (CiVo) dataMap.get("ciVo");
            }
            if (dataMap.containsKey("ciEntityVo")) {
                ciEntityVo = (CiEntityVo) dataMap.get("ciEntityVo");
            }
            if (dataMap.containsKey("ciEntityList")) {
                ciEntityList = (List<CiEntityVo>) dataMap.get("ciEntityList");
            }
        }
        if (ciVo != null) {
            returnData.put("ciId", ciVo.getId());
            returnData.put("ciName", ciVo.getName());
            returnData.put("ciLabel", ciVo.getLabel());
        }
        if (ciEntityVo != null) {
            returnData.put("ciEntityId", ciEntityVo.getId());
            returnData.put("ciEntityName", ciEntityVo.getName());
        }
        if (ciEntityList != null) {
            returnData.put("invalidCiEntityList", ciEntityList);
        }
        return returnData;
    }
}
