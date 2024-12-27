package neatlogic.module.cmdb.process.stephandler;

import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import org.springframework.stereotype.Service;

@Service
//@Deprecated
public class CIEntitySyncProcessUtilHandler extends ProcessStepInternalHandlerBase {

    @Override
    public String getHandler() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getHandler();

    }

    @Override
    public Object getStartStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return null;
    }

    @Override
    public Object getNonStartStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return null;
    }

    @Override
    public void updateProcessTaskStepUserAndWorker(Long processTaskId, Long processTaskStepId) {

    }

    public IOperationType[] getStepActions() {
        return new IOperationType[]{
                ProcessTaskStepOperationType.STEP_VIEW,
                ProcessTaskStepOperationType.STEP_TRANSFER
        };
    }

    @Override
    public IOperationType[] getStepButtons() {
        return new IOperationType[]{
                ProcessTaskStepOperationType.STEP_COMPLETE,
                ProcessTaskStepOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskStepOperationType.STEP_ACCEPT
        };
    }

    @Override
    public String[] getRegulateKeyList() {
        return new String[]{"authorityList", "notifyPolicyConfig", "actionConfig", "customButtonList", "customStatusList", "replaceableTextList", "workerPolicyConfig", "formSceneUuid", "formSceneName", "autoStart", "isNeedUploadFile", "isNeedContent", "isRequired", "commentTemplateId", "tagList", "handlerList"};
    }

}
