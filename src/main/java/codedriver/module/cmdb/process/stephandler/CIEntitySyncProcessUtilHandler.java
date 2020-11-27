package codedriver.module.cmdb.process.stephandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.dto.ProcessStepVo;
import codedriver.framework.process.dto.ProcessStepWorkerPolicyVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.operationauth.core.IOperationAuthHandlerType;
import codedriver.framework.process.stephandler.core.ProcessStepUtilHandlerBase;
import codedriver.module.cmdb.process.notifyhandler.CiEntitySyncNotifyHandler;

@Service
public class CIEntitySyncProcessUtilHandler extends ProcessStepUtilHandlerBase {

    @Override
    public String getHandler() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getHandler();

    }

    @Override
    public Object getHandlerStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getHandlerStepInitInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void makeupProcessStep(ProcessStepVo processStepVo, JSONObject stepConfigObj) {
        /** 组装通知策略id **/
        JSONObject notifyPolicyConfig = stepConfigObj.getJSONObject("notifyPolicyConfig");
        if (MapUtils.isNotEmpty(notifyPolicyConfig)) {
            Long policyId = notifyPolicyConfig.getLong("policyId");
            if (policyId != null) {
                processStepVo.setNotifyPolicyId(policyId);
            }
        }
        /** 组装分配策略 **/
        JSONObject workerPolicyConfig = stepConfigObj.getJSONObject("workerPolicyConfig");
        if (MapUtils.isNotEmpty(workerPolicyConfig)) {
            JSONArray policyList = workerPolicyConfig.getJSONArray("policyList");
            if (CollectionUtils.isNotEmpty(policyList)) {
                List<ProcessStepWorkerPolicyVo> workerPolicyList = new ArrayList<>();
                for (int k = 0; k < policyList.size(); k++) {
                    JSONObject policyObj = policyList.getJSONObject(k);
                    if (!"1".equals(policyObj.getString("isChecked"))) {
                        continue;
                    }
                    ProcessStepWorkerPolicyVo processStepWorkerPolicyVo = new ProcessStepWorkerPolicyVo();
                    processStepWorkerPolicyVo.setProcessUuid(processStepVo.getProcessUuid());
                    processStepWorkerPolicyVo.setProcessStepUuid(processStepVo.getUuid());
                    processStepWorkerPolicyVo.setPolicy(policyObj.getString("type"));
                    processStepWorkerPolicyVo.setSort(k + 1);
                    processStepWorkerPolicyVo.setConfig(policyObj.getString("config"));
                    workerPolicyList.add(processStepWorkerPolicyVo);
                }
                processStepVo.setWorkerPolicyList(workerPolicyList);
            }
        }
        /**
         * FIXME 设置CMDB节点设置
         */
        JSONObject ciEntitySyncConfig = stepConfigObj.getJSONObject("ciEntitySyncConfig");
        if (ciEntitySyncConfig != null) {
            processStepVo.setConfig(ciEntitySyncConfig.toJSONString());
        }
    }

    @Override
    public void updateProcessTaskStepUserAndWorker(Long processTaskId, Long processTaskStepId) {
        // TODO Auto-generated method stub

    }
    @SuppressWarnings("serial")
    @Override
    public JSONObject makeupConfig(JSONObject configObj) {
        if (configObj == null) {
            configObj = new JSONObject();
        }
        JSONObject resultObj = new JSONObject();

        /** 授权 **/
        JSONArray authorityArray = new JSONArray();
        ProcessTaskOperationType[] stepActions = {ProcessTaskOperationType.VIEW, ProcessTaskOperationType.TRANSFERCURRENTSTEP};
        for (ProcessTaskOperationType stepAction : stepActions) {
            authorityArray.add(new JSONObject() {
                {
                    this.put("action", stepAction.getValue());
                    this.put("text", stepAction.getText());
                    this.put("acceptList", stepAction.getAcceptList());
                    this.put("groupList", stepAction.getGroupList());
                }
            });
        }
        JSONArray authorityList = configObj.getJSONArray("authorityList");
        if (CollectionUtils.isNotEmpty(authorityList)) {
            Map<String, JSONArray> authorityMap = new HashMap<>();
            for (int i = 0; i < authorityList.size(); i++) {
                JSONObject authority = authorityList.getJSONObject(i);
                authorityMap.put(authority.getString("action"), authority.getJSONArray("acceptList"));
            }
            for (int i = 0; i < authorityArray.size(); i++) {
                JSONObject authority = authorityArray.getJSONObject(i);
                JSONArray acceptList = authorityMap.get(authority.getString("action"));
                if (acceptList != null) {
                    authority.put("acceptList", acceptList);
                }
            }
        }
        resultObj.put("authorityList", authorityArray);

        /** 按钮映射 **/
        JSONArray customButtonArray = new JSONArray();
        ProcessTaskOperationType[] stepButtons = {ProcessTaskOperationType.COMPLETE, ProcessTaskOperationType.BACK,
            ProcessTaskOperationType.TRANSFER, ProcessTaskOperationType.START};
        for (ProcessTaskOperationType stepButton : stepButtons) {
            customButtonArray.add(new JSONObject() {
                {
                    this.put("name", stepButton.getValue());
                    this.put("customText", stepButton.getText());
                    this.put("value", "");
                }
            });
        }

        JSONArray customButtonList = configObj.getJSONArray("customButtonList");
        if (CollectionUtils.isNotEmpty(customButtonList)) {
            Map<String, String> customButtonMap = new HashMap<>();
            for (int i = 0; i < customButtonList.size(); i++) {
                JSONObject customButton = customButtonList.getJSONObject(i);
                customButtonMap.put(customButton.getString("name"), customButton.getString("value"));
            }
            for (int i = 0; i < customButtonArray.size(); i++) {
                JSONObject customButton = customButtonArray.getJSONObject(i);
                String value = customButtonMap.get(customButton.getString("name"));
                if (StringUtils.isNotBlank(value)) {
                    customButton.put("value", value);
                }
            }
        }
        resultObj.put("customButtonList", customButtonArray);

        /** 通知 **/
        JSONObject notifyPolicyObj = new JSONObject();
        JSONObject notifyPolicyConfig = configObj.getJSONObject("notifyPolicyConfig");
        if (MapUtils.isNotEmpty(notifyPolicyConfig)) {
            notifyPolicyObj.putAll(notifyPolicyConfig);
        }
        notifyPolicyObj.put("handler", CiEntitySyncNotifyHandler.class.getName());
        resultObj.put("notifyPolicyConfig", notifyPolicyObj);

        return resultObj;
    }

    @Override
    protected IOperationAuthHandlerType MyOperationAuthHandlerType() {
        // TODO Auto-generated method stub
        return null;
    }

}
