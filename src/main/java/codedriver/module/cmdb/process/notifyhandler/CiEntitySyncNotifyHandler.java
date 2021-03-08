package codedriver.module.cmdb.process.notifyhandler;

import java.util.List;

import codedriver.framework.auth.core.AuthFactory;
import codedriver.framework.notify.core.INotifyPolicyHandlerGroup;
import codedriver.framework.notify.core.NotifyHandlerType;
import codedriver.framework.notify.dto.NotifyTriggerTemplateVo;
import codedriver.framework.notify.dto.NotifyTriggerVo;
import codedriver.framework.process.constvalue.ProcessNotifyPolicyHandlerGroup;
import codedriver.framework.process.constvalue.ProcessStepHandlerType;
import codedriver.module.cmdb.process.stephandler.CmdbProcessStepHandlerType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.dto.ConditionParamVo;
import codedriver.framework.notify.core.NotifyPolicyHandlerBase;

@Component
public class CiEntitySyncNotifyHandler extends NotifyPolicyHandlerBase {

    @Override
    public String getName() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getName();
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<NotifyTriggerTemplateVo> myNotifyTriggerTemplateList(NotifyHandlerType type) {
        return null;
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAuthName() {
        return AuthFactory.getAuthInstance("CIENTITY_MODIFY").getAuthName();
    }

    @Override
    public INotifyPolicyHandlerGroup getGroup() {
        return ProcessNotifyPolicyHandlerGroup.TASKSTEP;
    }
}
