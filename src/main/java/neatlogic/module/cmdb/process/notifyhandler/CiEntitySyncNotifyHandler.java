package neatlogic.module.cmdb.process.notifyhandler;

import neatlogic.framework.auth.core.AuthBase;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.process.notify.core.ProcessTaskNotifyHandlerBase;
import neatlogic.module.cmdb.process.stephandler.CmdbProcessStepHandlerType;
import org.springframework.stereotype.Component;

@Component
public class CiEntitySyncNotifyHandler extends ProcessTaskNotifyHandlerBase {

    @Override
    public String getName() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getName();
    }

    @Override
    public Class<? extends AuthBase> getAuthClass() {
        return CIENTITY_MODIFY.class;
    }

    @Override
    public String getModuleGroup() {
        return "process";
    }

}
