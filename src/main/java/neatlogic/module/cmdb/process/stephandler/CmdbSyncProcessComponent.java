/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.process.stephandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.process.constvalue.ProcessStepMode;
import neatlogic.framework.process.constvalue.automatic.FailPolicy;
import neatlogic.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class CmdbSyncProcessComponent extends ProcessStepHandlerBase {

    private final Logger logger = LoggerFactory.getLogger(CmdbSyncProcessComponent.class);

    @Resource
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Override
    public String getHandler() {
        return CmdbProcessStepHandlerType.CMDBSYNC.getHandler();
    }

    @Override
    public JSONObject getChartConfig() {
        return new JSONObject() {
            {
                this.put("icon", "ts-m-cmdb");
                this.put("shape", "L-rectangle-50%:R-rectangle-50%");
                this.put("width", 68);
                this.put("height", 40);
            }
        };
    }

    @Override
    public String getType() {
        return CmdbProcessStepHandlerType.CMDBSYNC.getType();
    }

    @Override
    public ProcessStepMode getMode() {
        return ProcessStepMode.AT;
    }

    @Override
    public String getName() {
        return CmdbProcessStepHandlerType.CMDBSYNC.getName();
    }

    @Override
    public int getSort() {
        return 12;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Boolean isAllowStart() {
        return null;
    }

    @Override
    protected int myActive(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        try {
            String configHash = currentProcessTaskStepVo.getConfigHash();
            if (StringUtils.isBlank(configHash)) {
                ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
                configHash = processTaskStepVo.getConfigHash();
                currentProcessTaskStepVo.setProcessStepUuid(processTaskStepVo.getProcessStepUuid());
            }
            // 获取工单当前步骤配置信息
            String config = selectContentByHashMapper.getProcessTaskStepConfigByHash(configHash);

            if (StringUtils.isBlank(config)) {
                return 0;
            }
            JSONObject ciEntityConfig = (JSONObject) JSONPath.read(config, "ciEntityConfig");
            if (MapUtils.isEmpty(ciEntityConfig)) {
                return 0;
            }
            // rerunStepToCreateNewJob为1时表示重新激活自动化步骤时创建新作业，rerunStepToCreateNewJob为0时表示重新激活自动化步骤时不创建新作业，也不重跑旧作业，即什么都不做
            Integer rerunStepToSync = ciEntityConfig.getInteger("rerunStepToSync");
            if (!Objects.equals(rerunStepToSync, 1)) {
//                Long autoexecJobId = autoexecJobMapper.getJobIdByInvokeIdLimitOne(currentProcessTaskStepVo.getId());
//                if (autoexecJobId != null) {
//                    return 1;
//                }
            }
            JSONArray configList = ciEntityConfig.getJSONArray("configList");
            if (CollectionUtils.isEmpty(configList)) {
                return 1;
            }
            JSONObject mainConfigObj = configList.getJSONObject(0);
            Long ciId = mainConfigObj.getLong("ciId");
            if (ciId == null) {
                return 0;
            }
            Map<String, JSONObject> dependencyConfigMap = new HashMap<>();
            for (int i = 1; i < configList.size(); i++) {
                JSONObject configObject = configList.getJSONObject(i);
                String uuid = configObject.getString("uuid");
                if (StringUtils.isBlank(uuid)) {
                    continue;
                }
                dependencyConfigMap.put(uuid, configObject);
            }
//            JSONArray mappingList = configObj.getJSONArray("mappingList");
//            if (CollectionUtils.isEmpty(mappingList)) {
//                return 0;
//            }
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            String createPolicy = mainConfigObj.getString("createPolicy");
            if (Objects.equals(createPolicy, "single")) {
                ciEntityTransactionList = createSingleCiEntityVo(mainConfigObj, dependencyConfigMap);
                if (CollectionUtils.isEmpty(ciEntityTransactionList)) {
                    return 0;
                }
//                ciEntityList.add(ciEntityVo);
            } else if (Objects.equals(createPolicy, "batch")) {
                ciEntityTransactionList = createBatchCiEntityVo(mainConfigObj, dependencyConfigMap);
                if (CollectionUtils.isEmpty(ciEntityTransactionList)) {
                    return 0;
                }
//                ciEntityList.addAll(ciEntityVoList);
            } else {
                return 0;
            }
            boolean flag = false;
            JSONArray errorMessageList = new JSONArray();
            for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
                try {
//                    autoexecJobActionService.validateCreateJob(jobVo);
//                    autoexecJobMapper.insertAutoexecJobProcessTaskStep(jobVo.getId(), currentProcessTaskStepVo.getId());
//                    jobIdList.add(jobVo.getId());
                } catch (Exception e) {
                    // 增加提醒
                    logger.error(e.getMessage(), e);
                    JSONObject errorMessageObj = new JSONObject();
//                    errorMessageObj.put("jobId", ciEntityVo.getId());
//                    errorMessageObj.put("jobName", ciEntityVo.getName());
                    errorMessageObj.put("error", e.getMessage());
                    errorMessageList.add(errorMessageObj);
                    flag = true;
                }
            }
            // 如果有一个作业创建有异常，则根据失败策略执行操作
            if (flag) {
                ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo();
                processTaskStepDataVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                processTaskStepDataVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                processTaskStepDataVo.setType("ciEntitySyncError");
                JSONObject dataObj = new JSONObject();
                dataObj.put("errorList", errorMessageList);
                processTaskStepDataVo.setData(dataObj.toJSONString());
                processTaskStepDataVo.setFcu(UserContext.get().getUserUuid());
                processTaskStepDataMapper.replaceProcessTaskStepData(processTaskStepDataVo);
                String failPolicy = ciEntityConfig.getString("failPolicy");
                if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                    // TODO
                }
            }
            // TODO 根据配置数据修改配置项数据逻辑

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ProcessTaskException(e.getMessage());
        }
        return 1;
    }

    private List<CiEntityTransactionVo> createSingleCiEntityVo(JSONObject mainConfigObj, Map<String, JSONObject> dependencyConfigMap) {
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
//        cmdb/ci/get
//        cmdb/ciview/get
//        cmdb/ci/listattr
//        cmdb/ci/479609502048256/listrel
//        cmdb/globalattr/search
//        cmdb/ci/unique/get
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
//        ciEntityTransactionVo.setCiEntityId(id);
//        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
//        ciEntityTransactionVo.setCiEntityUuid(uuid);
//        ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
//        ciEntityTransactionVo.setCiId(ciId);
//        ciEntityTransactionVo.setDescription(description);


        JSONObject attrEntityData = new JSONObject();
        ciEntityTransactionVo.setAttrEntityData(attrEntityData);
        JSONObject relEntityData = new JSONObject();
        ciEntityTransactionVo.setRelEntityData(relEntityData);
        JSONObject globalAttrEntityData = new JSONObject();
        ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrEntityData);

        ciEntityTransactionList.add(ciEntityTransactionVo);
        return ciEntityTransactionList;
    }

    private List<CiEntityTransactionVo> createBatchCiEntityVo(JSONObject mainConfigObj, Map<String, JSONObject> dependencyConfigMap) {
        return null;
    }

    @Override
    protected int myAssign(ProcessTaskStepVo currentProcessTaskStepVo, Set<ProcessTaskStepWorkerVo> workerSet) throws ProcessTaskException {
        return defaultAssign(currentProcessTaskStepVo, workerSet);
    }

    @Override
    protected int myHang(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myHandle(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        currentProcessTaskStepVo.setIsAllDone(true);
        return 0;
    }

    @Override
    protected int myStart(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myComplete(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myCompleteAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myReapproval(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myReapprovalAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRetreat(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myAbort(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRecover(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myPause(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myTransfer(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myBack(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int mySaveDraft(ProcessTaskStepVo processTaskStepVo) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myStartProcess(ProcessTaskStepVo processTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected Set<Long> myGetNext(ProcessTaskStepVo currentProcessTaskStepVo, List<Long> nextStepIdList, Long nextStepId) throws ProcessTaskException {
        return defaultGetNext(nextStepIdList, nextStepId);
    }

    @Override
    protected int myRedo(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }
}
