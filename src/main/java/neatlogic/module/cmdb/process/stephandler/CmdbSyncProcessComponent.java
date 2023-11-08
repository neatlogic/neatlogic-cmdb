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
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.NewCiEntityNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.process.constvalue.ProcessFlowDirection;
import neatlogic.framework.process.constvalue.ProcessStepMode;
import neatlogic.framework.process.constvalue.ProcessTaskAuditType;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.automatic.FailPolicy;
import neatlogic.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import neatlogic.framework.process.dto.*;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import neatlogic.framework.process.stephandler.core.IProcessStepHandler;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.process.stephandler.core.ProcessStepThread;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.process.constvalue.CmdbAuditDetailType;
import neatlogic.module.cmdb.process.dto.*;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CmdbSyncProcessComponent extends ProcessStepHandlerBase {

    private final Logger logger = LoggerFactory.getLogger(CmdbSyncProcessComponent.class);

    @Resource
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Resource
    private CiEntityService ciEntityService;

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
        return ProcessStepMode.MT;
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

            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap = new HashMap<>();
            Map<String, FormAttributeVo> formAttributeMap = new HashMap<>();
            Long processTaskId = currentProcessTaskStepVo.getProcessTaskId();
            // 如果工单有表单信息，则查询出表单配置及数据
            ProcessTaskFormVo processTaskFormVo = processTaskMapper.getProcessTaskFormByProcessTaskId(processTaskId);
            if (processTaskFormVo != null) {
                String formContent = selectContentByHashMapper.getProcessTaskFromContentByHash(processTaskFormVo.getFormContentHash());
                FormVersionVo formVersionVo = new FormVersionVo();
                formVersionVo.setFormUuid(processTaskFormVo.getFormUuid());
                formVersionVo.setFormName(processTaskFormVo.getFormName());
                formVersionVo.setFormConfig(JSONObject.parseObject(formContent));
                List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
                if (CollectionUtils.isNotEmpty(formAttributeList)) {
                    formAttributeMap = formAttributeList.stream().collect(Collectors.toMap(e -> e.getUuid(), e -> e));
                }
                List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(processTaskId);
                if (CollectionUtils.isNotEmpty(processTaskFormAttributeDataList)) {
                    processTaskFormAttributeDataMap = processTaskFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
                }
            }
            boolean flag = false;
            JSONArray errorMessageList = new JSONArray();
            try {
                CiEntitySyncVo ciEntitySyncVo = ciEntityConfig.toJavaObject(CiEntitySyncVo.class);
                // rerunStepToSync为1时表示重新激活CMDB步骤时同步配置项实例，rerunStepToSync为0时表示重新激活CMDB步骤时不同步配置项实例，即什么都不做，直接自动流转到下一阶段
//            Integer rerunStepToSync = ciEntityConfig.getInteger("rerunStepToSync");
                Integer rerunStepToSync = ciEntitySyncVo.getRerunStepToSync();
                if (!Objects.equals(rerunStepToSync, 1)) {
                    ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
                    searchVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                    searchVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                    searchVo.setType("ciEntitySyncResult");
                    ProcessTaskStepDataVo processTaskStepData = processTaskStepDataMapper.getProcessTaskStepData(searchVo);
                    if (processTaskStepData != null) {
                        autoComplete2(currentProcessTaskStepVo);
                        return 1;
                    }
                }
                ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
                searchVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                searchVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                searchVo.setType("ciEntitySyncError");
                ProcessTaskStepDataVo processTaskStepData = processTaskStepDataMapper.getProcessTaskStepData(searchVo);
                if (processTaskStepData != null) {
                    processTaskStepDataMapper.deleteProcessTaskStepDataById(processTaskStepData.getId());
                }
//            JSONArray configList = ciEntityConfig.getJSONArray("configList");
//            if (CollectionUtils.isEmpty(configList)) {
//                return 0;
//            }
                List<CiEntitySyncConfigVo> configList = ciEntitySyncVo.getConfigList();
                System.out.println("configList1 = " + JSONObject.toJSONString(configList));
                /* 重新构建configList配置信息 */
                configList = rebuildConfigList(configList, formAttributeMap, processTaskFormAttributeDataMap);
                System.out.println("configList2 = " + JSONObject.toJSONString(configList));
                if (CollectionUtils.isEmpty(configList)) {
                    return 1;
                }
                System.out.println("configList.size() = " + configList.size());
                /* 遍历configList， 找出起始模型配置列表，根据id和uuid的值判断是新增还是更新 */
                List<CiEntitySyncConfigVo> startConfigList = new ArrayList<>();
                Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
                Map<String, CiEntitySyncConfigVo> dependencyConfigMap = new HashMap<>();
                for (CiEntitySyncConfigVo configObject : configList) {
//                JSONObject configObject = configList.getJSONObject(i);
                    String uuid = configObject.getUuid();
                    System.out.println("uuid = " + uuid);
                    if (StringUtils.isBlank(uuid)) {
                        continue;
                    }
                    Long ciId = configObject.getCiId();
                    if (ciId == null) {
                        continue;
                    }
                    CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                    ciEntityTransactionVo.setCiEntityUuid(uuid);
                    ciEntityTransactionVo.setCiId(ciId);
                    Long id = configObject.getId();
                    if (id != null) {
                        System.out.println("update id = " + id);
                        ciEntityTransactionVo.setCiEntityId(id);
                        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                    } else {
                        System.out.println("insert uuid = " + uuid);
                        ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                    }
                    Integer isStart = configObject.getIsStart();
                    if (Objects.equals(isStart, 1)) {
                        startConfigList.add(configObject);
                    } else {
                        dependencyConfigMap.put(uuid, configObject);
                    }
                    ciEntityTransactionMap.put(uuid, ciEntityTransactionVo);
                }
                if (CollectionUtils.isEmpty(startConfigList)) {
                    return 0;
                }
                /* 遍历起始模型配置信息列表，生成CiEntityTransactionVo列表 */
                List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                for (CiEntitySyncConfigVo mainConfigObj : startConfigList) {
                    List<CiEntityTransactionVo> list = createCiEntityTransactionVo(ciEntityTransactionMap, mainConfigObj, dependencyConfigMap, formAttributeMap, processTaskFormAttributeDataMap);
                    for (CiEntityTransactionVo ciEntityTransactionVo : list) {
                        if (!ciEntityTransactionList.contains(ciEntityTransactionVo)) {
                            ciEntityTransactionList.add(ciEntityTransactionVo);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
                    for (CiEntityTransactionVo t : ciEntityTransactionList) {
                        t.setAllowCommit(true);
                    }
                    System.out.println("ciEntityTransactionList = " + JSONObject.toJSONString(ciEntityTransactionList));
                    System.out.println("ciEntityTransactionList.size() = " + ciEntityTransactionList.size());
                    EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                        InputFromContext.init(InputFrom.ITSM);
                        Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
//                    Long transactionGroupId = 0L;
                        currentProcessTaskStepVo.getParamObj().put(CmdbAuditDetailType.CMDBSYNCMESSAGE.getParamName(), transactionGroupId);
                        System.out.println("transactionGroupId = " + transactionGroupId);
                        ProcessTaskStepDataVo search = new ProcessTaskStepDataVo();
                        search.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                        search.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                        search.setType("ciEntitySyncResult");
                        ProcessTaskStepDataVo oldProcessTaskStepData = processTaskStepDataMapper.getProcessTaskStepData(search);
                        if (oldProcessTaskStepData != null) {
                            JSONObject dataObj = oldProcessTaskStepData.getData();
                            JSONArray transactionGroupList = dataObj.getJSONArray("transactionGroupList");
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("time", System.currentTimeMillis());
                            jsonObj.put("transactionGroupId", transactionGroupId);
                            transactionGroupList.add(jsonObj);
                        } else {
                            oldProcessTaskStepData = search;
                            oldProcessTaskStepData.setFcu(UserContext.get().getUserUuid());
                            JSONObject dataObj = new JSONObject();
                            JSONArray transactionGroupList = new JSONArray();
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("time", System.currentTimeMillis());
                            jsonObj.put("transactionGroupId", transactionGroupId);
                            transactionGroupList.add(jsonObj);
                            dataObj.put("transactionGroupList", transactionGroupList);
                            oldProcessTaskStepData.setData(dataObj.toJSONString());
                        }
                        processTaskStepDataMapper.replaceProcessTaskStepData(oldProcessTaskStepData);
                    }).execute();
                    if (!s.isSucceed()) {
                        // 增加提醒
                        logger.error(s.getError(), s.getException());
                        JSONObject errorMessageObj = new JSONObject();
                        errorMessageObj.put("error", s.getError());
                        errorMessageList.add(errorMessageObj);
                        JSONObject dataObj = new JSONObject();
                        dataObj.put("time", System.currentTimeMillis());
                        dataObj.put("errorList", errorMessageList);
                        currentProcessTaskStepVo.getParamObj().put(CmdbAuditDetailType.CMDBSYNCMESSAGE.getParamName(), dataObj.toJSONString());
                        flag = true;
                    }
                    /* 处理历史记录 **/
                    IProcessStepHandlerUtil.audit(currentProcessTaskStepVo, ProcessTaskAuditType.ACTIVE);
                }
            } catch (Exception e) {
                // 增加提醒
                logger.error(e.getMessage(), e);
                JSONObject errorMessageObj = new JSONObject();
                errorMessageObj.put("error", e.getMessage());
                errorMessageList.add(errorMessageObj);
                JSONObject dataObj = new JSONObject();
                dataObj.put("time", System.currentTimeMillis());
                dataObj.put("errorList", errorMessageList);
                currentProcessTaskStepVo.getParamObj().put(CmdbAuditDetailType.CMDBSYNCMESSAGE.getParamName(), dataObj.toJSONString());
                flag = true;
            }

            // 如果有异常，则根据失败策略执行操作
            if (flag) {
                ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo();
                processTaskStepDataVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                processTaskStepDataVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                processTaskStepDataVo.setType("ciEntitySyncError");
                JSONObject dataObj = new JSONObject();
                dataObj.put("time", System.currentTimeMillis());
                dataObj.put("errorList", errorMessageList);
                processTaskStepDataVo.setData(dataObj.toJSONString());
                processTaskStepDataVo.setFcu(UserContext.get().getUserUuid());
                processTaskStepDataMapper.replaceProcessTaskStepData(processTaskStepDataVo);
                String failPolicy = ciEntityConfig.getString("failPolicy");
                if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                    autoComplete2(currentProcessTaskStepVo);
                }
            } else {
                autoComplete2(currentProcessTaskStepVo);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ProcessTaskException(e.getMessage());
        }
        return 1;
    }

    private void autoComplete2(ProcessTaskStepVo currentProcessTaskStepVo) {
        List<Long> toProcessTaskStepIdList = processTaskMapper.getToProcessTaskStepIdListByFromIdAndType(currentProcessTaskStepVo.getId(), ProcessFlowDirection.FORWARD.getValue());
        if (toProcessTaskStepIdList.size() == 1) {
            Long nextStepId = toProcessTaskStepIdList.get(0);
            IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(CmdbProcessStepHandlerType.CMDBSYNC.getHandler());
            try {
                ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
                JSONObject paramObj = processTaskStepVo.getParamObj();
                paramObj.put("nextStepId", nextStepId);
                paramObj.put("action", ProcessTaskOperationType.STEP_COMPLETE.getValue());
                /* 自动处理 **/
                doNext(ProcessTaskOperationType.STEP_COMPLETE, new ProcessStepThread(processTaskStepVo) {
                    @Override
                    public void myExecute() {
                        handler.autoComplete(processTaskStepVo);
                    }
                });
            } catch (ProcessTaskNoPermissionException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 遍历configList，将“批量操作”的配置信息根据表单数据转换成多条“单个操作”配置信息
     * @param configList 配置信息列表
     * @param processTaskFormAttributeDataMap 表单数据信息
     * @return
     */
    private List<CiEntitySyncConfigVo> handleBatchDataSource(
            List<CiEntitySyncConfigVo> configList,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap
    ) {
        Map<String, List<String>> oldUuid2NewUuidListMap = new HashMap<>();
        List<CiEntitySyncConfigVo> newConfigList = new ArrayList<>();
        for (CiEntitySyncConfigVo configObj : configList) {
//            JSONObject configObj = configList.getJSONObject(i);
            if (!Objects.equals(configObj.getCreatePolicy(), "batch")) {
                // 单个操作配置，不做修改
                newConfigList.add(configObj);
                continue;
            }
            String oldUuid = configObj.getUuid();
            // 批量操作配置信息
            CiEntitySyncBatchDataSourceVo batchDataSource = configObj.getBatchDataSource();
//            if (batchDataSource == null) {
//                // 单个操作配置，不做修改
//                newConfigList.add(configObj);
//                continue;
//            }
            String attributeUuid = batchDataSource.getAttributeUuid();
            ProcessTaskFormAttributeDataVo formAttributeDataVo = processTaskFormAttributeDataMap.get(attributeUuid);
            List<CiEntitySyncFilterVo> filterList = batchDataSource.getFilterList();
            JSONArray tbodyList = getTbodyList(formAttributeDataVo, filterList);
            if (CollectionUtils.isEmpty(tbodyList)) {
                continue;
            }
            List<CiEntitySyncMappingVo> mappingList = configObj.getMappingList();
            // 遍历批量操作表格数据
            for (int j = 0; j < tbodyList.size(); j++) {
                JSONObject tbodyObj = tbodyList.getJSONObject(j);
                if (MapUtils.isEmpty(tbodyObj)) {
                    logger.warn("批量操作数据源表格的第" + j + "行数据为空");
                    continue;
                }
                System.out.println("tbodyObj = " + tbodyObj);
                CiEntitySyncConfigVo newConfigObj = new CiEntitySyncConfigVo();
//                boolean flag = false;
                List<CiEntitySyncMappingVo> newMappingList = new ArrayList<>();
                for (CiEntitySyncMappingVo mappingObj : mappingList) {
                    CiEntitySyncMappingVo newMappingObj = new CiEntitySyncMappingVo(mappingObj);
//                    JSONObject mappingObj = mappingList.getJSONObject(k);
                    String key = mappingObj.getKey();
                    String mappingMode = mappingObj.getMappingMode();
                    if (Objects.equals(mappingMode, "formTableComponent")) {
                        // 映射模式是表单表格组件
                        JSONArray valueList = mappingObj.getValueList();
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            String value = valueList.getString(0);
                            if (Objects.equals(value, attributeUuid)) {
                                String column = mappingObj.getColumn();
//                                String columnValue = tbodyObj.getString(column);
                                JSONArray newValueList = new JSONArray();
                                Object valueObj = tbodyObj.get(column);
                                if (valueObj instanceof JSONObject) {

                                } else if (valueObj instanceof JSONArray) {
                                    JSONArray valueArray = (JSONArray) valueObj;
                                    for (int k = 0; k < valueArray.size(); k++) {
                                        String valueStr = valueArray.getString(k);
                                        if (valueStr.contains("&=&")) {
                                            String[] split = valueStr.split("&=&");
                                            newValueList.add(split[0]);
                                        } else {
                                            newValueList.add(valueStr);
                                        }
                                    }
                                } else if (valueObj instanceof String) {
                                    String valueStr = (String) valueObj;
                                    if (valueStr.contains("&=&")) {
                                        String[] split = valueStr.split("&=&");
                                        newValueList.add(split[0]);
                                    } else {
                                        newValueList.add(valueStr);
                                    }
                                } else {
                                    String valueStr = valueObj.toString();
                                    if (valueStr.contains("&=&")) {
                                        String[] split = valueStr.split("&=&");
                                        newValueList.add(split[0]);
                                    } else {
                                        newValueList.add(valueStr);
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(newValueList)) {
//                                    flag = true;
                                    newMappingObj.setMappingMode("constant");
                                    newMappingObj.setValueList(newValueList);
                                } else {
                                    continue;
                                }
                            } else {
//                                newMappingObj.putAll(mappingObj);
                            }
                        } else {
                            continue;
                        }
                    } else {
//                        newMappingObj.putAll(mappingObj);
                    }
                    newMappingList.add(newMappingObj);
                }
//                if (flag) {
                String newUuid = UuidUtil.randomUuid();
                oldUuid2NewUuidListMap.computeIfAbsent(oldUuid, key -> new ArrayList<>()).add(newUuid);
                newConfigObj.setUuid(newUuid);
                newConfigObj.setCiId(configObj.getCiId());
                newConfigObj.setCiName(configObj.getCiName());
//                    newConfigObj.put("createPolicy", "single");
                newConfigObj.setCiLabel(configObj.getCiLabel());
                newConfigObj.setIsStart(configObj.getIsStart());
                newConfigObj.setCiIcon(configObj.getCiIcon());
                newConfigObj.setMappingList(newMappingList);
                newConfigList.add(newConfigObj);
//                }
            }
        }
        System.out.println("oldUuid2NewUuidListMap = " + JSONObject.toJSONString(oldUuid2NewUuidListMap));
        // 遍历newConfigList，根据oldUuid2NewUuidListMap，将关系映射配置信息中的valueList数据重新构建
        for (CiEntitySyncConfigVo configObj : newConfigList) {
//            JSONObject configObj = newConfigList.getJSONObject(i);
            List<CiEntitySyncMappingVo> mappingList = configObj.getMappingList();
            for (CiEntitySyncMappingVo mappingObj : mappingList) {
//                JSONObject mappingObj = mappingList.getJSONObject(k);
                String key = mappingObj.getKey();
                if (key.startsWith("rel")) {
                    JSONArray valueList = mappingObj.getValueList();
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        JSONArray newValueList = new JSONArray();
                        for (int i = 0; i < valueList.size(); i++) {
                            JSONObject valueObj = valueList.getJSONObject(i);
//                            String type = valueObj.getString("type");
//                            if (Objects.equals(type, "new")) {
                                String ciEntityUuid = valueObj.getString("ciEntityUuid");
                                List<String> newUuidList = oldUuid2NewUuidListMap.get(ciEntityUuid);
                                if (CollectionUtils.isNotEmpty(newUuidList)) {
                                    for (String newUuid : newUuidList) {
                                        JSONObject newValueObj = new JSONObject();
                                        newValueObj.putAll(valueObj);
                                        newValueObj.put("ciEntityUuid", newUuid);
                                        newValueList.add(newValueObj);
                                    }
                                    mappingObj.setValueList(newValueList);
                                } else {
                                    JSONObject newValueObj = new JSONObject();
                                    newValueObj.putAll(valueObj);
                                    newValueList.add(newValueObj);
                                }
//                            }
                        }
                    }
                }
            }
        }
        return newConfigList;
    }

    /**
     * 遍历configList，将mappingList中映射模式为“表单普通组件”和“表单表格组件”的数据替换称表单组件对应的真实值
     * @param configList 配置信息列表
     * @param formAttributeMap 表单组件信息
     * @param processTaskFormAttributeDataMap 表单数据信息
     */
    private void handleMappingFormComponent(
            List<CiEntitySyncConfigVo> configList,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap
    ) {
        for (CiEntitySyncConfigVo configObj : configList) {
            List<CiEntitySyncMappingVo> mappingList = configObj.getMappingList();
            for (CiEntitySyncMappingVo mappingObj : mappingList) {
                JSONArray valueList = mappingObj.getValueList();
                if (CollectionUtils.isEmpty(valueList)) {
                    continue;
                }
                JSONArray newValueList = new JSONArray();
                String mappingMode = mappingObj.getMappingMode();
                mappingObj.setValueList(newValueList);
                if (Objects.equals(mappingMode, "formTableComponent")) {
                    mappingObj.setMappingMode("constant");
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(valueList.getString(0));
                    if (attributeDataVo == null) {
                        continue;
                    }
//                    JSONArray filterList = mappingObj.getJSONArray("filterList");
                    List<CiEntitySyncFilterVo> filterList = mappingObj.getFilterList();
                    JSONArray tbodyList = getTbodyList(attributeDataVo, filterList);
                    if (CollectionUtils.isEmpty(tbodyList)) {
                        continue;
                    }
                    String column = mappingObj.getColumn();
                    for (int m = 0; m < tbodyList.size(); m++) {
                        JSONObject tbodyObj = tbodyList.getJSONObject(m);
                        if (MapUtils.isEmpty(tbodyObj)) {
                            continue;
                        }
                        Object valueObj = tbodyObj.get(column);
                        if (valueObj instanceof JSONObject) {

                        } else if (valueObj instanceof JSONArray) {
                            JSONArray valueArray = (JSONArray) valueObj;
                            for (int j = 0; j < valueArray.size(); j++) {
                                String valueStr = valueArray.getString(j);
                                if (valueStr.contains("&=&")) {
                                    String[] split = valueStr.split("&=&");
                                    newValueList.add(split[0]);
                                } else {
                                    newValueList.add(valueStr);
                                }
                            }
                        } else if (valueObj instanceof String) {
                            String valueStr = (String) valueObj;
                            if (valueStr.contains("&=&")) {
                                String[] split = valueStr.split("&=&");
                                newValueList.add(split[0]);
                            } else {
                                newValueList.add(valueStr);
                            }
                        } else {
                            String valueStr = valueObj.toString();
                            if (valueStr.contains("&=&")) {
                                String[] split = valueStr.split("&=&");
                                newValueList.add(split[0]);
                            } else {
                                newValueList.add(valueStr);
                            }
                        }
                    }
                } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                    mappingObj.setMappingMode("constant");
                    ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(valueList.getString(0));
                    if (attributeDataVo == null) {
                        continue;
                    }
                    Object dataObj = attributeDataVo.getDataObj();
                    if (dataObj == null) {
                        continue;
                    }
                    FormAttributeVo formAttributeVo = formAttributeMap.get(valueList.getString(0));
                    if (Objects.equals(formAttributeVo.getHandler(), FormHandler.FORMUPLOAD.getHandler())) {
//                        List<Long> idList = new ArrayList<>();
                        if (dataObj instanceof JSONArray) {
                            JSONArray dataArray = (JSONArray) dataObj;
                            for (int m = 0; m < dataArray.size(); m++) {
                                JSONObject data = dataArray.getJSONObject(m);
                                Long id = data.getLong("id");
                                if (id != null) {
                                    newValueList.add(id);
                                }
                            }
                        }
                    } else {
                        if (dataObj instanceof JSONObject) {

                        } else if (dataObj instanceof JSONArray) {
                            newValueList.addAll((JSONArray) dataObj);
                        } else {
                            newValueList.add(dataObj);
                        }
                    }
                } else {
                    newValueList.addAll(valueList);
                }
            }
        }
    }

    /**
     * 遍历configList，根据唯一规则属性值删除重复配置信息
     * @param configList 配置信息列表
     * @return
     */
    private List<CiEntitySyncConfigVo> removeDuplicatesByUniqueAttrValue(List<CiEntitySyncConfigVo> configList) {
        List<String> uniqueAttrValueListJoinStrList = new ArrayList<>();
        List<CiEntitySyncConfigVo> newConfigList = new ArrayList<>();
        for (CiEntitySyncConfigVo configObj : configList) {
//            JSONObject configObj = configList.getJSONObject(i);
            Long ciId = configObj.getCiId();
            CiVo ciVo = ciMapper.getCiById(ciId);
            if (ciVo == null) {
                String ciName = configObj.getCiName();
                throw new CiNotFoundException(ciName);
            }
            List<Long> uniqueAttrIdList = ciVo.getUniqueAttrIdList();
            if (CollectionUtils.isEmpty(uniqueAttrIdList)) {
                newConfigList.add(configObj);
            }
            Map<String, JSONArray> key2ValueListMap = new HashMap<>();
            List<CiEntitySyncMappingVo> mappingList = configObj.getMappingList();
            for (CiEntitySyncMappingVo mappingObj : mappingList) {
//                JSONObject mappingObj = mappingList.getJSONObject(j);
                String key = mappingObj.getKey();
                JSONArray valueList = mappingObj.getValueList();
                key2ValueListMap.put(key, valueList);
            }
            List<String> list = new ArrayList<>();
            list.add(ciId.toString());
            for (Long attrId : uniqueAttrIdList) {
                JSONArray valueList = key2ValueListMap.get("attr_" + attrId);
                list.add(valueList.toJSONString());
            }
            String uniqueAttrValueListJoinStr = String.join(",", list);
            if (uniqueAttrValueListJoinStrList.contains(uniqueAttrValueListJoinStr)) {
                continue;
            }
            //校验唯一规则
            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            ciEntityConditionVo.setCiId(ciId);
            ciEntityConditionVo.setAttrIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
            ciEntityConditionVo.setRelIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
            for (Long attrId : ciVo.getUniqueAttrIdList()) {
                AttrFilterVo filterVo = new AttrFilterVo();
                filterVo.setAttrId(attrId);
                filterVo.setExpression(SearchExpression.EQ.getExpression());
                List<String> valueList = new ArrayList<>();
                JSONArray valueArray = key2ValueListMap.get("attr_" + attrId);
                if (CollectionUtils.isNotEmpty(valueArray)) {
                    valueList = valueArray.toJavaList(String.class);
                }
                filterVo.setValueList(valueList);
                ciEntityConditionVo.addAttrFilter(filterVo);
            }
            if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                List<CiEntityVo> checkList = ciEntityService.searchCiEntity(ciEntityConditionVo);
                if (CollectionUtils.isNotEmpty(checkList)) {
                    configObj.setId(checkList.get(0).getId());
                }
            }
            uniqueAttrValueListJoinStrList.add(uniqueAttrValueListJoinStr);
            newConfigList.add(configObj);
        }
        return newConfigList;
    }

    /**
     * 遍历configList，重新生成uuid的值
     * @param configList 配置信息列表
     * @return
     */
    private List<CiEntitySyncConfigVo> updateUuidValue(List<CiEntitySyncConfigVo> configList) {
        Map<String, String> oldUuid2NewUuidMap = new HashMap<>();
        for (CiEntitySyncConfigVo configObject : configList) {
            Integer isStart = configObject.getIsStart();
            if (Objects.equals(isStart, 1)) {
                // TODO
            }
            String uuid = configObject.getUuid();
            Long id = configObject.getId();
            if (id != null) {
                CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(id);
                if (ciEntityVo == null) {
                    configObject.setId(null);
                    oldUuid2NewUuidMap.put(uuid, UuidUtil.randomUuid());
                } else {
                    if (!Objects.equals(uuid, ciEntityVo.getUuid())) {
                        oldUuid2NewUuidMap.put(uuid, ciEntityVo.getUuid());
                    }
                }
            } else if (StringUtils.isNotBlank(uuid)) {
                CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(uuid);
                if (ciEntityVo != null) {
                    oldUuid2NewUuidMap.put(uuid, UuidUtil.randomUuid());
//                    if (Objects.equals(isStart, 1)) {
//                        oldUuid2NewUuidMap.put(uuid, UuidUtil.randomUuid());
//                    } else {
//                        configObject.setId(ciEntityVo.getId());
//                    }
                }
            }
        }
        if (MapUtils.isNotEmpty(oldUuid2NewUuidMap)) {
            System.out.println("oldUuid2NewUuidMap = " + JSONObject.toJSONString(oldUuid2NewUuidMap));
            String configListStr = JSONObject.toJSONString(configList);
            for (Map.Entry<String, String> entry : oldUuid2NewUuidMap.entrySet()) {
                configListStr = configListStr.replace(entry.getKey(), entry.getValue());
            }
            return JSONObject.parseArray(configListStr, CiEntitySyncConfigVo.class);
        }
        return configList;
    }
    /**
     * 重新构建configList配置信息
     * @param configList 配置信息列表
     * @param formAttributeMap 表单组件信息
     * @param processTaskFormAttributeDataMap 表单数据信息
     * @return
     */
    private List<CiEntitySyncConfigVo> rebuildConfigList(
            List<CiEntitySyncConfigVo> configList,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        // 遍历configList，将“批量操作”的配置信息根据表单数据转换成多条“单个操作”配置信息
        configList = handleBatchDataSource(configList, processTaskFormAttributeDataMap);
        // 遍历configList，将mappingList中映射模式为“表单普通组件”和“表单表格组件”的数据替换称表单组件对应的真实值
        handleMappingFormComponent(configList, formAttributeMap, processTaskFormAttributeDataMap);
        // 遍历configList，根据唯一规则属性值删除重复配置信息
        configList = removeDuplicatesByUniqueAttrValue(configList);
        // 遍历configList，重新生成uuid的值
        configList = updateUuidValue(configList);
        return configList;
    }

    private List<CiEntityTransactionVo> createCiEntityTransactionVo(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            CiEntitySyncConfigVo mainConfigObj,
            Map<String, CiEntitySyncConfigVo> dependencyConfigMap,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();

        Long ciId = mainConfigObj.getCiId();
        Map<String, CiEntitySyncMappingVo> mappingMap = new HashMap<>();
        List<CiEntitySyncMappingVo> mappingList = mainConfigObj.getMappingList();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
//            JSONObject mappingObj = mappingList.getJSONObject(i);
//            if (MapUtils.isEmpty(mappingObj)) {
//                continue;
//            }
            String key = mappingObj.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            mappingMap.put(key, mappingObj);
        }
//        Long id = mainConfigObj.getId();
        String uuid = mainConfigObj.getUuid();
        CiEntityTransactionVo ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
//        if (id != null) {
//            ciEntityTransactionVo.setCiEntityId(id);
//            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
//        } else {
//            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
//        }
//        ciEntityTransactionVo.setCiId(ciId);
        /** 变更说明 **/
        CiEntitySyncMappingVo descriptionMappingObj = mappingMap.get("description");
        if (descriptionMappingObj != null) {
            JSONArray valueList = descriptionMappingObj.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                System.out.println("description is null");
            }
            String description = valueList.getString(0);
//            String description = parseDescription(descriptionMappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
            if (StringUtils.isBlank(description)) {
                System.out.println("description is null");
            }
            ciEntityTransactionVo.setDescription(description);
        } else {
            System.out.println("description is null");
        }

        /** 属性 **/
        JSONObject attrEntityData = new JSONObject();
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        for (AttrVo attrVo : attrList) {
            if (Objects.equals(attrVo.getExpression(), "")) {
                continue;
            }
            String key = "attr_" + attrVo.getId();
            CiEntitySyncMappingVo mappingObj = mappingMap.get(key);
            if (mappingObj == null) {
//                System.out.println(key + " is null");
                continue;
            }
//            JSONObject attrEntity = parseAttr(ciEntityTransactionMap, attrVo, mappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
            // 映射模式为常量
            JSONArray valueList = mappingObj.getValueList();
            if (CollectionUtils.isNotEmpty(valueList)) {
//                valueList = valueArray;
                for (int i = valueList.size() - 1; i >= 0; i--) {
                    if (valueList.get(i) instanceof JSONObject) {
                        JSONObject valueObj = valueList.getJSONObject(i);
                        String attrCiEntityUuid = valueObj.getString("uuid");
                        Long attrCiEntityId = valueObj.getLong("id");
                        if (attrCiEntityId == null && StringUtils.isNotBlank(attrCiEntityUuid)) {
                            CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(attrCiEntityUuid);
                            if (tmpVo != null) {
                                //替换掉原来的ciEntityUuid为新的ciEntityId
                                valueList.set(i, tmpVo.getCiEntityId());
                            } else {
                                //使用uuid寻找配置项
                                CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(attrCiEntityUuid);
                                if (uuidCiEntityVo == null) {
                                    throw new NewCiEntityNotFoundException(attrCiEntityUuid);
                                } else {
                                    valueList.set(i, uuidCiEntityVo.getId());
                                }
                            }
                        } else if (attrCiEntityId != null) {
                            valueList.set(i, attrCiEntityId);
                        } else {
                            valueList.remove(i);
                        }
                    }
                }
            }
            JSONObject attrEntity = new JSONObject();
            attrEntity.put("type", attrVo.getType());
            attrEntity.put("config", attrVo.getConfig());
            attrEntity.put("valueList", valueList);
            attrEntityData.put(key, attrEntity);
        }
        ciEntityTransactionVo.setAttrEntityData(attrEntityData);
        /** 关系 **/
        JSONObject relEntityData = new JSONObject();
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        for (RelVo relVo : relList) {
            String key = "rel" + relVo.getDirection() + "_" + relVo.getId();
            CiEntitySyncMappingVo mappingObj = mappingMap.get(key);
            if (mappingObj == null) {
//                System.out.println(key + " is null");
                continue;
            }
            String mappingMode = mappingObj.getMappingMode();
            if (Objects.equals(mappingMode, "new")) {
                JSONArray valueList = mappingObj.getValueList();
                if (CollectionUtils.isNotEmpty(valueList)) {
                    List<Long> ciEntityIdList = new ArrayList<>();
                    List<JSONObject> alreadyExistRelList = new ArrayList<>();
                    for (int i = 0; i < valueList.size(); i++) {
                        JSONObject valueObj = valueList.getJSONObject(i);
                        if (MapUtils.isEmpty(valueObj)) {
                            continue;
                        }
                        String ciEntityUuid = valueObj.getString("ciEntityUuid");
                        if (StringUtils.isBlank(ciEntityUuid)) {
                            continue;
                        }
                        Long ciEntityId = valueObj.getLong("ciEntityId");
                        if (ciEntityId == null) {
                            CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(ciEntityUuid);
                            if (tmpVo != null) {
                                ciEntityId = tmpVo.getCiEntityId();
                                System.out.println("tmpVo.getCiEntityId() = " + tmpVo.getCiEntityId());
                                valueObj.put("ciEntityId", ciEntityId);
                            } else {
                                CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(ciEntityUuid);
                                if (uuidCiEntityVo == null) {
                                    throw new NewCiEntityNotFoundException(valueObj.getString("ciEntityUuid"));
                                } else {
                                    ciEntityId = uuidCiEntityVo.getId();
                                    valueObj.put("ciEntityId", ciEntityId);
                                }
                            }
                        }
                        ciEntityIdList.add(ciEntityId);
                        String type = valueObj.getString("type");
                        if (!Objects.equals(type, "new")) {
                            continue;
                        }
                        CiEntitySyncConfigVo dependencyConfig = dependencyConfigMap.get(ciEntityUuid);
                        if (dependencyConfig == null) {
                            continue;
                        }
                        // 关系选择追加模式时，需要将该配置项原来就关联的关系数据补充到valueList中
                        if (Objects.equals(dependencyConfig.getAction(), "append") && ciEntityTransactionVo.getCiEntityId() != null) {
//                            Long relCiEntityId = dependencyConfig.getId();
                            List<RelEntityVo> relEntityList;
                            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                relEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(ciEntityTransactionVo.getCiEntityId(), relVo.getId(), null);
                                for (RelEntityVo relEntityVo : relEntityList) {
//                                    if (relCiEntityId != null && Objects.equals(relCiEntityId, relEntityVo.getToCiEntityId())) {
//                                        continue;
//                                    }
                                    JSONObject jsonObj = new JSONObject();
                                    jsonObj.put("ciEntityId", relEntityVo.getToCiEntityId());
                                    jsonObj.put("ciEntityName", relEntityVo.getToCiEntityName());
                                    jsonObj.put("ciId", relEntityVo.getToCiId());
                                    alreadyExistRelList.add(jsonObj);
                                }
                            } else {
                                relEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(ciEntityTransactionVo.getCiEntityId(), relVo.getId(), null);
                                for (RelEntityVo relEntityVo : relEntityList) {
//                                    if (relCiEntityId != null && Objects.equals(relCiEntityId, relEntityVo.getFromCiEntityId())) {
//                                        continue;
//                                    }
                                    JSONObject jsonObj = new JSONObject();
                                    jsonObj.put("ciEntityId", relEntityVo.getFromCiEntityId());
                                    jsonObj.put("ciEntityName", relEntityVo.getFromCiEntityName());
                                    jsonObj.put("ciId", relEntityVo.getFromCiId());
                                    alreadyExistRelList.add(jsonObj);
                                }
                            }
                        }
                        List<CiEntityTransactionVo> list = createCiEntityTransactionVo(ciEntityTransactionMap, dependencyConfig, dependencyConfigMap, formAttributeMap, processTaskFormAttributeDataMap);
                        ciEntityTransactionList.addAll(list);
                    }
//                    JSONObject relEntity = parseRel(relVo, mappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
                    // 关系选择追加模式时，需要将该配置项原来就关联的关系数据补充到valueList中
                    for (JSONObject jsonObj : alreadyExistRelList) {
                        if (ciEntityIdList.contains(jsonObj.getLong("ciEntityId"))) {
                            continue;
                        }
                        valueList.add(jsonObj);
                    }
                    JSONObject relEntity = new JSONObject();
                    relEntity.put("valueList", valueList);
                    relEntityData.put(key, relEntity);
                }
            }
        }
        ciEntityTransactionVo.setRelEntityData(relEntityData);
        /** 全局属性 **/
        JSONObject globalAttrEntityData = new JSONObject();
        GlobalAttrVo searchVo = new GlobalAttrVo();
        searchVo.setIsActive(1);
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(searchVo);
        for (GlobalAttrVo globalAttrVo : globalAttrList) {
            String key = "global_" + globalAttrVo.getId();
            CiEntitySyncMappingVo mappingObj = mappingMap.get(key);
            if (mappingObj == null) {
//                System.out.println(key + " is null");
                continue;
            }
//            JSONObject globalAttrEntity = parseGlobalAttr(globalAttrVo, mappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
            JSONArray valueList = new JSONArray();
            String mappingMode = mappingObj.getMappingMode();
            if (Objects.equals(mappingMode, "constant")) {
                // 映射模式为常量
                JSONArray valueArray = mappingObj.getValueList();
                if (CollectionUtils.isNotEmpty(valueArray)) {
                valueList = valueArray;
                }
            }
            JSONObject globalAttrEntity = new JSONObject();
            globalAttrEntity.put("valueList", valueList);
            globalAttrEntityData.put(key, globalAttrEntity);
        }
        ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrEntityData);

        ciEntityTransactionList.add(ciEntityTransactionVo);
        return ciEntityTransactionList;
    }

    private JSONArray getTbodyList(ProcessTaskFormAttributeDataVo formAttributeDataVo, List<CiEntitySyncFilterVo> filterList) {
        JSONArray tbodyList = new JSONArray();
        if (formAttributeDataVo == null) {
            return tbodyList;
        }
        if (!Objects.equals(formAttributeDataVo.getType(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLEINPUTER.getHandler())
                && !Objects.equals(formAttributeDataVo.getType(), neatlogic.framework.form.constvalue.FormHandler.FORMTABLESELECTOR.getHandler())) {
            return tbodyList;
        }
        if (formAttributeDataVo.getDataObj() == null) {
            return tbodyList;
        }
        JSONArray dataList = (JSONArray) formAttributeDataVo.getDataObj();
        // 数据过滤
        if (CollectionUtils.isNotEmpty(filterList)) {
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject data = dataList.getJSONObject(i);
                if (MapUtils.isEmpty(data)) {
                    continue;
                }
                boolean flag = true;
                for (CiEntitySyncFilterVo filterObj : filterList) {
//                    JSONObject filterObj = filterList.getJSONObject(j);
//                    if (MapUtils.isEmpty(filterObj)) {
//                        continue;
//                    }
                    String column = filterObj.getColumn();
                    if (StringUtils.isBlank(column)) {
                        continue;
                    }
                    String expression = filterObj.getExpression();
                    if (StringUtils.isBlank(expression)) {
                        continue;
                    }
                    String value = filterObj.getValue();
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }
                    if (Objects.equals(expression, Expression.EQUAL.getExpression())) {
                        if (!Objects.equals(value, data.getString(column))) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.UNEQUAL.getExpression())) {
                        if (Objects.equals(value, data.getString(column))) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.LIKE.getExpression())) {
                        String columnValue = data.getString(column);
                        if (StringUtils.isBlank(columnValue)) {
                            flag = false;
                            break;
                        }
                        if (!columnValue.contains(value)) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.NOTLIKE.getExpression())) {
                        String columnValue = data.getString(column);
                        if (StringUtils.isBlank(columnValue)) {
                            continue;
                        }
                        if (columnValue.contains(value)) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    tbodyList.add(data);
                }
            }
        } else {
            tbodyList = dataList;
        }
        return tbodyList;
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
