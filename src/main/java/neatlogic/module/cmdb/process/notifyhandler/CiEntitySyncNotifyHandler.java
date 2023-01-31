package neatlogic.module.cmdb.process.notifyhandler;

import java.util.List;

import neatlogic.framework.auth.core.AuthFactory;
import neatlogic.framework.notify.core.INotifyPolicyHandlerGroup;
import neatlogic.framework.notify.core.NotifyHandlerType;
import neatlogic.framework.notify.dto.NotifyTriggerTemplateVo;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import neatlogic.framework.process.notify.constvalue.ProcessNotifyPolicyHandlerGroup;
import neatlogic.module.cmdb.process.stephandler.CmdbProcessStepHandlerType;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.core.NotifyPolicyHandlerBase;

@Component
public class CiEntitySyncNotifyHandler extends NotifyPolicyHandlerBase {

    @Override
    public String getName() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getName();
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
        return AuthFactory.getAuthInstance("CIENTITY_MODIFY").getAuthName();
    }

    @Override
    public INotifyPolicyHandlerGroup getGroup() {
        return ProcessNotifyPolicyHandlerGroup.TASKSTEP;
    }
}
