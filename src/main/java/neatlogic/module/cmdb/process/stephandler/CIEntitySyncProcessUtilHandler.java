package neatlogic.module.cmdb.process.stephandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.processconfig.ActionConfigVo;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import neatlogic.framework.process.util.ProcessConfigUtil;
import neatlogic.module.cmdb.process.notifyhandler.CiEntitySyncNotifyHandler;
import neatlogic.module.cmdb.process.notifyhandler.CmdbSyncNotifyHandler;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
    @SuppressWarnings("serial")
    @Override
    public JSONObject makeupConfig(JSONObject configObj) {
        if (configObj == null) {
            configObj = new JSONObject();
        }
        JSONObject resultObj = new JSONObject();

        /** 授权 **/
        IOperationType[] stepActions = {
                ProcessTaskStepOperationType.STEP_VIEW,
                ProcessTaskStepOperationType.STEP_TRANSFER
        };
        JSONArray authorityList = configObj.getJSONArray("authorityList");
        JSONArray authorityArray = ProcessConfigUtil.regulateAuthorityList(authorityList, stepActions);
        resultObj.put("authorityList", authorityArray);

        /** 按钮映射 **/
        IOperationType[] stepButtons = {
                ProcessTaskStepOperationType.STEP_COMPLETE,
                ProcessTaskStepOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskStepOperationType.STEP_ACCEPT
        };
        JSONArray customButtonList = configObj.getJSONArray("customButtonList");
        JSONArray customButtonArray = ProcessConfigUtil.regulateCustomButtonList(customButtonList, stepButtons);
        resultObj.put("customButtonList", customButtonArray);

        /** 状态映射列表 **/
        JSONArray customStatusList = configObj.getJSONArray("customStatusList");
        JSONArray customStatusArray = ProcessConfigUtil.regulateCustomStatusList(customStatusList);
        resultObj.put("customStatusList", customStatusArray);

        /** 可替换文本列表 **/
        resultObj.put("replaceableTextList", ProcessConfigUtil.regulateReplaceableTextList(configObj.getJSONArray("replaceableTextList")));
        return resultObj;
    }

    @Override
    public JSONObject regulateProcessStepConfig(JSONObject configObj) {
        if (configObj == null) {
            configObj = new JSONObject();
        }
        JSONObject resultObj = new JSONObject();

        /** 授权 **/
        IOperationType[] stepActions = {
                ProcessTaskStepOperationType.STEP_VIEW,
                ProcessTaskStepOperationType.STEP_TRANSFER
        };
        JSONArray authorityList = null;
        Integer enableAuthority = configObj.getInteger("enableAuthority");
        if (Objects.equals(enableAuthority, 1)) {
            authorityList = configObj.getJSONArray("authorityList");
        } else {
            enableAuthority = 0;
        }
        resultObj.put("enableAuthority", enableAuthority);
        JSONArray authorityArray = ProcessConfigUtil.regulateAuthorityList(authorityList, stepActions);
        resultObj.put("authorityList", authorityArray);

        /** 通知 **/
        JSONObject notifyPolicyConfig = configObj.getJSONObject("notifyPolicyConfig");
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(notifyPolicyConfig, CiEntitySyncNotifyHandler.class);
        resultObj.put("notifyPolicyConfig", invokeNotifyPolicyConfigVo);

        /** 动作 **/
        JSONObject actionConfig = configObj.getJSONObject("actionConfig");
        ActionConfigVo actionConfigVo = JSONObject.toJavaObject(actionConfig, ActionConfigVo.class);
        if (actionConfigVo == null) {
            actionConfigVo = new ActionConfigVo();
        }
        actionConfigVo.setHandler(CmdbSyncNotifyHandler.class.getName());
        resultObj.put("actionConfig", actionConfigVo);

        JSONArray customButtonList = configObj.getJSONArray("customButtonList");
        /** 按钮映射列表 **/
        IOperationType[] stepButtons = {
                ProcessTaskStepOperationType.STEP_COMPLETE,
                ProcessTaskStepOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskStepOperationType.STEP_ACCEPT
        };

        JSONArray customButtonArray = ProcessConfigUtil.regulateCustomButtonList(customButtonList, stepButtons);
        resultObj.put("customButtonList", customButtonArray);
        /** 状态映射列表 **/
        JSONArray customStatusList = configObj.getJSONArray("customStatusList");
        JSONArray customStatusArray = ProcessConfigUtil.regulateCustomStatusList(customStatusList);
        resultObj.put("customStatusList", customStatusArray);

        /** 可替换文本列表 **/
        resultObj.put("replaceableTextList", ProcessConfigUtil.regulateReplaceableTextList(configObj.getJSONArray("replaceableTextList")));

        /** 关联表单组件 **/
        JSONArray handlerList = configObj.getJSONArray("handlerList");
        if (handlerList == null) {
            handlerList = new JSONArray();
        }
        resultObj.put("handlerList", handlerList);

        /** 分配处理人 **/
        JSONObject workerPolicyConfig = configObj.getJSONObject("workerPolicyConfig");
        JSONObject workerPolicyObj = ProcessConfigUtil.regulateWorkerPolicyConfig(workerPolicyConfig);
        resultObj.put("workerPolicyConfig", workerPolicyObj);

        JSONObject simpleSettings = ProcessConfigUtil.regulateSimpleSettings(configObj);
        resultObj.putAll(simpleSettings);
        /** 表单场景 **/
        String formSceneUuid = configObj.getString("formSceneUuid");
        String formSceneName = configObj.getString("formSceneName");
        resultObj.put("formSceneUuid", formSceneUuid == null ? "" : formSceneUuid);
        resultObj.put("formSceneName", formSceneName == null ? "" : formSceneName);
        return resultObj;
    }
}
