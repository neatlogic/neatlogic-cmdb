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
