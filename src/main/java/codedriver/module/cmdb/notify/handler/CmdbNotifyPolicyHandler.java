/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.notify.handler;

import codedriver.framework.dto.ConditionParamVo;
import codedriver.framework.notify.core.INotifyPolicyHandlerGroup;
import codedriver.framework.notify.core.NotifyHandlerType;
import codedriver.framework.notify.core.NotifyPolicyHandlerBase;
import codedriver.framework.notify.dto.NotifyTriggerTemplateVo;
import codedriver.framework.notify.dto.NotifyTriggerVo;
import codedriver.framework.cmdb.auth.label.CI_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CmdbNotifyPolicyHandler extends NotifyPolicyHandlerBase {
    @Override
    public String getName() {
        return "模型";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return CI_MODIFY.class.getSimpleName();
    }

    @Override
    public INotifyPolicyHandlerGroup getGroup() {
        return CmdbNotifyGroup.CMDB;
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        List<NotifyTriggerVo> returnList = new ArrayList<>();
        for (CmdbNotifyTriggerType triggerType : CmdbNotifyTriggerType.values()) {
            returnList.add(new NotifyTriggerVo(triggerType.getTrigger(), triggerType.getText(), triggerType.getDescription()));
        }
        return returnList;
    }

    @Override
    protected List<NotifyTriggerTemplateVo> myNotifyTriggerTemplateList(NotifyHandlerType type) {
        return new ArrayList<>();
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        return new ArrayList<>();
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return new ArrayList<>();
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {

    }
}
