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
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import neatlogic.framework.process.dto.ProcessStepVo;
import neatlogic.framework.process.dto.ProcessStepWorkerPolicyVo;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.processconfig.ActionConfigActionVo;
import neatlogic.framework.process.dto.processconfig.ActionConfigVo;
import neatlogic.framework.process.stephandler.core.ProcessStepInternalHandlerBase;
import neatlogic.framework.process.util.ProcessConfigUtil;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.process.dto.*;
import neatlogic.module.cmdb.process.exception.CiEntityConfigIllegalException;
import neatlogic.module.cmdb.process.notifyhandler.CmdbSyncNotifyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
public class CmdbSyncProcessUtilHandler extends ProcessStepInternalHandlerBase {

    private Logger logger = LoggerFactory.getLogger(CmdbSyncProcessUtilHandler.class);

    @Resource
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public String getHandler() {
        return CmdbProcessStepHandlerType.CMDBSYNC.getHandler();
    }

    @Override
    public Object getHandlerStepInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return getHandlerStepInitInfo(currentProcessTaskStepVo);
    }

    @Override
    public Object getHandlerStepInitInfo(ProcessTaskStepVo currentProcessTaskStepVo) {
        JSONObject resultObj = new JSONObject();
        /** 事务审计列表 **/
        ProcessTaskStepDataVo search = new ProcessTaskStepDataVo();
        search.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
        search.setProcessTaskStepId(currentProcessTaskStepVo.getId());
        search.setType("ciEntitySyncResult");
        ProcessTaskStepDataVo processTaskStepData = processTaskStepDataMapper.getProcessTaskStepData(search);
        if (processTaskStepData != null) {
            JSONObject dataObj = processTaskStepData.getData();
            JSONArray transactionGroupArray = dataObj.getJSONArray("transactionGroupList");
            if (CollectionUtils.isNotEmpty(transactionGroupArray)) {
                List<JSONObject> tableList = new ArrayList<>();
                for (int i = transactionGroupArray.size() - 1; i >= 0; i--) {
                    JSONObject transactionGroupObj = transactionGroupArray.getJSONObject(i);
                    Long time = transactionGroupObj.getLong("time");
                    Long transactionGroupId = transactionGroupObj.getLong("transactionGroupId");
                    TransactionVo transactionVo = new TransactionVo();
                    transactionVo.setTransactionGroupId(transactionGroupId);
                    List<TransactionVo> tbodyList = transactionMapper.searchTransaction(transactionVo);
                    if (CollectionUtils.isNotEmpty(tbodyList)) {
                        JSONObject tableObj = TableResultUtil.getResult(tbodyList);
                        tableObj.put("time", time);
                        tableList.add(tableObj);
                    }
                }
                resultObj.put("tableList", tableList);
            }
        }
        /** 错误信息列表 **/
        ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
        searchVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
        searchVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
        searchVo.setType("ciEntitySyncError");
        ProcessTaskStepDataVo processTaskStepDataVo = processTaskStepDataMapper.getProcessTaskStepData(searchVo);
        if (processTaskStepDataVo != null) {
            JSONObject dataObj = processTaskStepDataVo.getData();
            if (MapUtils.isNotEmpty(dataObj)) {
                resultObj.putAll(dataObj);
//                JSONArray errorList = dataObj.getJSONArray("errorList");
//                if (CollectionUtils.isNotEmpty(errorList)) {
//                    resultObj.put("errorList", errorList);
//                }
            }
        }
        return resultObj;
    }

    @Override
    public void makeupProcessStep(ProcessStepVo processStepVo, JSONObject stepConfigObj) {
        /** 组装通知策略id **/
        JSONObject notifyPolicyConfig = stepConfigObj.getJSONObject("notifyPolicyConfig");
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.toJavaObject(notifyPolicyConfig, InvokeNotifyPolicyConfigVo.class);
        if (invokeNotifyPolicyConfigVo != null) {
            processStepVo.setNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
        }

        JSONObject actionConfig = stepConfigObj.getJSONObject("actionConfig");
        ActionConfigVo actionConfigVo = JSONObject.toJavaObject(actionConfig, ActionConfigVo.class);
        if (actionConfigVo != null) {
            List<ActionConfigActionVo> actionList = actionConfigVo.getActionList();
            if (CollectionUtils.isNotEmpty(actionList)) {
                List<String> integrationUuidList = new ArrayList<>();
                for (ActionConfigActionVo actionVo : actionList) {
                    String integrationUuid = actionVo.getIntegrationUuid();
                    if (StringUtils.isNotBlank(integrationUuid)) {
                        integrationUuidList.add(integrationUuid);
                    }
                }
                processStepVo.setIntegrationUuidList(integrationUuidList);
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

        JSONArray tagList = stepConfigObj.getJSONArray("tagList");
        if (CollectionUtils.isNotEmpty(tagList)) {
            processStepVo.setTagList(tagList.toJavaList(String.class));
        }
        // 保存表单场景
        String formSceneUuid = stepConfigObj.getString("formSceneUuid");
        if (StringUtils.isNotBlank(formSceneUuid)) {
            processStepVo.setFormSceneUuid(formSceneUuid);
        }
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
        ProcessTaskOperationType[] stepActions = {
                ProcessTaskOperationType.STEP_VIEW,
                ProcessTaskOperationType.STEP_TRANSFER
        };
        JSONArray authorityList = configObj.getJSONArray("authorityList");
        JSONArray authorityArray = ProcessConfigUtil.regulateAuthorityList(authorityList, stepActions);
        resultObj.put("authorityList", authorityArray);

        /** 按钮映射 **/
        ProcessTaskOperationType[] stepButtons = {
                ProcessTaskOperationType.STEP_COMPLETE,
                ProcessTaskOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskOperationType.STEP_ACCEPT
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
        ProcessTaskOperationType[] stepActions = {
                ProcessTaskOperationType.STEP_VIEW,
                ProcessTaskOperationType.STEP_TRANSFER
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
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(notifyPolicyConfig, CmdbSyncNotifyHandler.class);
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
        ProcessTaskOperationType[] stepButtons = {
                ProcessTaskOperationType.STEP_COMPLETE,
                ProcessTaskOperationType.STEP_BACK,
                ProcessTaskOperationType.PROCESSTASK_TRANSFER,
                ProcessTaskOperationType.STEP_ACCEPT
        };

        JSONArray customButtonArray = ProcessConfigUtil.regulateCustomButtonList(customButtonList, stepButtons);
        resultObj.put("customButtonList", customButtonArray);
        /** 状态映射列表 **/
        JSONArray customStatusList = configObj.getJSONArray("customStatusList");
        JSONArray customStatusArray = ProcessConfigUtil.regulateCustomStatusList(customStatusList);
        resultObj.put("customStatusList", customStatusArray);

        /** 可替换文本列表 **/
        resultObj.put("replaceableTextList", ProcessConfigUtil.regulateReplaceableTextList(configObj.getJSONArray("replaceableTextList")));

        /** 自动化配置 **/
        JSONObject ciEntityConfig = configObj.getJSONObject("ciEntityConfig");
        CiEntitySyncVo ciEntitySyncVo = regulateCiEntityConfig(ciEntityConfig);
        resultObj.put("ciEntityConfig", ciEntitySyncVo);

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

    private CiEntitySyncVo regulateCiEntityConfig(JSONObject ciEntityConfig) {
        CiEntitySyncVo ciEntitySyncVo = new CiEntitySyncVo();
        if (ciEntityConfig != null) {
            ciEntitySyncVo = ciEntityConfig.toJavaObject(CiEntitySyncVo.class);
        }
        // 失败策略
        String failPolicy = ciEntitySyncVo.getFailPolicy();
        if (failPolicy == null) {
            if (ciEntityConfig != null) {
                logger.warn("ciEntityConfig.failPolicy is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.failPolicy is null");
            }
            ciEntitySyncVo.setFailPolicy(StringUtils.EMPTY);
        }
        // 回退步骤重新同步
        Integer rerunStepToSync = ciEntitySyncVo.getRerunStepToSync();
        if (rerunStepToSync == null) {
            if (ciEntityConfig != null) {
                logger.warn("ciEntityConfig.rerunStepToSync is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.rerunStepToSync is null");
            }
            ciEntitySyncVo.setRerunStepToSync(0);
        }
        List<CiEntitySyncConfigVo> configList = ciEntitySyncVo.getConfigList();
        if (CollectionUtils.isEmpty(configList)) {
            if (ciEntityConfig != null) {
                logger.warn("ciEntityConfig.configList is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList is null");
            }
            return ciEntitySyncVo;
        }
        Iterator<CiEntitySyncConfigVo> iterator = configList.iterator();
        while (iterator.hasNext()) {
            CiEntitySyncConfigVo configObj = iterator.next();
            if (configObj == null) {
                iterator.remove();
                continue;
            }
            if (configObj.getId() != null) {
                logger.warn("ciEntityConfig.configList[x].id is not null");
                configObj.setId(null);
            }
            String ciName = configObj.getCiName();
            if (StringUtils.isBlank(ciName)) {
                logger.warn("ciEntityConfig.configList[x].ciName is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[x].ciName is null");
            }
            String name = ciName;
            String ciLabel = configObj.getCiLabel();
            if (StringUtils.isBlank(ciLabel)) {
                logger.warn("ciEntityConfig.configList[" + name + "].ciLabel is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].ciLabel is null");
            }
            name += "(" + ciLabel + ")";
            if (StringUtils.isBlank(configObj.getUuid())) {
                logger.warn("ciEntityConfig.configList[" + name + "].uuid is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].uuid is null");
            }
            if (configObj.getCiId() == null) {
                logger.warn("ciEntityConfig.configList[" + name + "].ciId is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].ciId is null");
            }
            if (StringUtils.isBlank(configObj.getCiIcon())) {
                logger.warn("ciEntityConfig.configList[" + name + "].ciIcon is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].ciIcon is null");
            }
            String createPolicy = configObj.getCreatePolicy();
            if (StringUtils.isBlank(createPolicy)) {
                logger.warn("ciEntityConfig.configList[" + name + "].createPolicy is null");
                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].createPolicy is null");
            }
            CiEntitySyncBatchDataSourceVo batchDataSource = configObj.getBatchDataSource();
            if (Objects.equals(createPolicy, "single")) {
                if (batchDataSource != null) {
                    if (StringUtils.isNotBlank(batchDataSource.getAttributeUuid())) {
                        logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.attributeUuid is not null");
                    }
                    List<CiEntitySyncFilterVo> filterList = batchDataSource.getFilterList();
                    if (CollectionUtils.isNotEmpty(filterList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.filterList is not null");
                    }
                }
            } else if (Objects.equals(createPolicy, "batch")) {
                if (batchDataSource == null) {
                    logger.warn("createPolicy = batch, ciEntityConfig.configList[" + name + "].batchDataSource is null");
                    throw new CiEntityConfigIllegalException("createPolicy = batch, ciEntityConfig.configList[" + name + "].batchDataSource is null");
                }
                if (StringUtils.isBlank(batchDataSource.getAttributeUuid())) {
                    logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.attributeUuid is null");
                    throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].batchDataSource.attributeUuid is null");
                }
                String type = batchDataSource.getType();
                if (StringUtils.isBlank(type)) {
                    logger.warn("createPolicy = batch, ciEntityConfig.configList[" + name + "].batchDataSource.type is null");
                    throw new CiEntityConfigIllegalException("createPolicy = batch, ciEntityConfig.configList[" + name + "].batchDataSource.type is null");
                } else if (!Objects.equals(type, "formSubassemblyComponent") && !Objects.equals(type, "formTableComponent")) {
                    logger.warn("createPolicy = batch, ciEntityConfig.configList[" + name + "].batchDataSource.type = " + type + " is not valid");
                    throw new CiEntityConfigIllegalException("createPolicy = batch, ciEntityConfig.configList[" + name + "].batchDataSource.type = " + type + " is not valid");
                }
                List<CiEntitySyncFilterVo> filterList = batchDataSource.getFilterList();
                if (CollectionUtils.isNotEmpty(filterList)) {
                    Iterator<CiEntitySyncFilterVo> filterIterator = filterList.iterator();
                    while (filterIterator.hasNext()) {
                        CiEntitySyncFilterVo filterVo = filterIterator.next();
                        if (filterVo == null) {
                            logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y] is null");
                            filterIterator.remove();
                            continue;
                        }
                        if (StringUtils.isBlank(filterVo.getColumn())) {
                            logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y].column is null");
                            throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y].column is null");
                        }
                        if (StringUtils.isBlank(filterVo.getExpression())) {
                            logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y].expression is null");
                            throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y].expression is null");
                        }
                        if (StringUtils.isBlank(filterVo.getValue())) {
                            logger.warn("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y].value is null");
                            throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].batchDataSource.filterList[y].value is null");
                        }
                    }
                }
            }

            List<CiEntitySyncMappingVo> mappingList = configObj.getMappingList();
            if (CollectionUtils.isEmpty(mappingList)) {
                logger.warn("ciEntityConfig.configList[" + name + "].mappingList is null");
                continue;
            }
            Iterator<CiEntitySyncMappingVo> mappingIterator = mappingList.iterator();
            while (mappingIterator.hasNext()) {
                CiEntitySyncMappingVo mappingVo = mappingIterator.next();
                if (mappingVo == null) {
                    logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y] is null");
                    mappingIterator.remove();
                    continue;
                }
                if (StringUtils.isBlank(mappingVo.getKey())) {
                    logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].key is null");
                    throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].key is null");
                }
                String mappingMode = mappingVo.getMappingMode();
                if (StringUtils.isBlank(mappingMode)) {
                    logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].mappingMode is null");
                    throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].mappingMode is null");
                }
                JSONArray valueList = mappingVo.getValueList();
                List<CiEntitySyncFilterVo> filterList = mappingVo.getFilterList();
                if (Objects.equals(mappingMode, "formSubassemblyComponent")) {
                    if (CollectionUtils.isEmpty(valueList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                        throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                    }
                    if (valueList.get(0) == null) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                        throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                    }
                    if (CollectionUtils.isNotEmpty(filterList)) {
                        Iterator<CiEntitySyncFilterVo> filterIterator = filterList.iterator();
                        while (filterIterator.hasNext()) {
                            CiEntitySyncFilterVo filterVo = filterIterator.next();
                            if (filterVo == null) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z] is null");
                                filterIterator.remove();
                                continue;
                            }
                            if (StringUtils.isBlank(filterVo.getColumn())) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].column is null");
                                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].column is null");
                            }
                            if (StringUtils.isBlank(filterVo.getExpression())) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].expression is null");
                                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].expression is null");
                            }
                            if (StringUtils.isBlank(filterVo.getValue())) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].value is null");
                                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].value is null");
                            }
                        }
                    }
                } else if (Objects.equals(mappingMode, "formTableComponent")) {
                    if (CollectionUtils.isEmpty(valueList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                        throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                    }
                    if (valueList.get(0) == null) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                        throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                    }
                    if (CollectionUtils.isNotEmpty(filterList)) {
                        Iterator<CiEntitySyncFilterVo> filterIterator = filterList.iterator();
                        while (filterIterator.hasNext()) {
                            CiEntitySyncFilterVo filterVo = filterIterator.next();
                            if (filterVo == null) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z] is null");
                                filterIterator.remove();
                                continue;
                            }
                            if (StringUtils.isBlank(filterVo.getColumn())) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].column is null");
                                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].column is null");
                            }
                            if (StringUtils.isBlank(filterVo.getExpression())) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].expression is null");
                                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].expression is null");
                            }
                            if (StringUtils.isBlank(filterVo.getValue())) {
                                logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].value is null");
                                throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].filterList[z].value is null");
                            }
                        }
                    }
                } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                    if (CollectionUtils.isEmpty(valueList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                        throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                    }
                    for (int i = 0; i < valueList.size(); i++) {
                        if (valueList.get(i) == null) {
                            logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[z] is null");
                            throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                        }
                    }
                    if (CollectionUtils.isNotEmpty(filterList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList is not null");
                        mappingVo.setFilterList(null);
                    }
                } else if (Objects.equals(mappingMode, "constant")) {
                    if (CollectionUtils.isNotEmpty(filterList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList is not null");
                        mappingVo.setFilterList(null);
                    }
                } else if (Objects.equals(mappingMode, "new")) {
                    if (CollectionUtils.isEmpty(valueList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList is null");
                    } else {
                        if (valueList.get(0) == null) {
                            logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                            throw new CiEntityConfigIllegalException("ciEntityConfig.configList[" + name + "].mappingList[y].valueList[0] is null");
                        }
                    }
                    if (CollectionUtils.isNotEmpty(filterList)) {
                        logger.warn("ciEntityConfig.configList[" + name + "].mappingList[y].filterList is not null");
                        mappingVo.setFilterList(null);
                    }
                }
            }
        }
        return ciEntitySyncVo;
    }
}
