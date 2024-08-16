package neatlogic.module.cmdb.process.notifyhandler;

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
    public String getAuthName() {
        return CIENTITY_MODIFY.class.getSimpleName();
    }

}
