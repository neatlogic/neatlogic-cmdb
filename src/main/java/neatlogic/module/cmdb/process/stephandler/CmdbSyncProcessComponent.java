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
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.NewCiEntityNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.process.constvalue.ProcessFlowDirection;
import neatlogic.framework.process.constvalue.ProcessStepMode;
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
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
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
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

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
            // rerunStepToCreateNewJob为1时表示重新激活自动化步骤时创建新作业，rerunStepToCreateNewJob为0时表示重新激活自动化步骤时不创建新作业，也不重跑旧作业，即什么都不做
            Integer rerunStepToSync = ciEntityConfig.getInteger("rerunStepToSync");
            if (!Objects.equals(rerunStepToSync, 1)) {
                ProcessTaskStepDataVo search = new ProcessTaskStepDataVo();
                search.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                search.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                search.setType("ciEntitySyncResult");
                ProcessTaskStepDataVo processTaskStepData = processTaskStepDataMapper.getProcessTaskStepData(search);
                if (processTaskStepData != null) {
                    return 1;
                }
            }
            ProcessTaskStepDataVo processTaskStepData = new ProcessTaskStepDataVo();
            processTaskStepData.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
            processTaskStepData.setProcessTaskStepId(currentProcessTaskStepVo.getId());
            processTaskStepData.setType("ciEntitySyncError");
            processTaskStepDataMapper.deleteProcessTaskStepData(processTaskStepData);

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
            System.out.println("ciEntityConfig1 = " + ciEntityConfig);
            ciEntityConfig = rebuildCiEntityConfig(ciEntityConfig, processTaskFormAttributeDataMap);
            System.out.println("ciEntityConfig2 = " + ciEntityConfig);
            JSONArray configList = ciEntityConfig.getJSONArray("configList");
            if (CollectionUtils.isEmpty(configList)) {
                return 1;
            }
            System.out.println("configList.size() = " + configList.size());

            List<JSONObject> startConfigList = new ArrayList<>();
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
            Map<String, JSONObject> dependencyConfigMap = new HashMap<>();
            for (int i = 0; i < configList.size(); i++) {
                JSONObject configObject = configList.getJSONObject(i);
                String uuid = configObject.getString("uuid");
                System.out.println("uuid = " + uuid);
                if (StringUtils.isBlank(uuid)) {
                    continue;
                }
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                Long id = configObject.getLong("id");
                if (id != null) {
                    System.out.println("id = " + id);
                    ciEntityTransactionVo.setCiEntityId(id);
                    ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                } else {
                    CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(uuid);
                    if (uuidCiEntityVo != null) {
                        ciEntityTransactionVo.setCiEntityId(uuidCiEntityVo.getId());
                        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                        configObject.put("id", uuidCiEntityVo.getId());
                    } else {
                        ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                    }
                }
                Integer isStart = configObject.getInteger("isStart");
                if (Objects.equals(isStart, 1)) {
                    startConfigList.add(configObject);
                } else {
                    dependencyConfigMap.put(uuid, configObject);
                }
                ciEntityTransactionVo.setCiEntityUuid(uuid);
                ciEntityTransactionMap.put(uuid, ciEntityTransactionVo);
            }
            if (CollectionUtils.isEmpty(startConfigList)) {
                return 0;
            }
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            for (JSONObject mainConfigObj : startConfigList) {
                Long ciId = mainConfigObj.getLong("ciId");
                if (ciId == null) {
                    return 0;
                }
                List<CiEntityTransactionVo> list = createSingleCiEntityVo(ciEntityTransactionMap, mainConfigObj, dependencyConfigMap, formAttributeMap, processTaskFormAttributeDataMap);
                for (CiEntityTransactionVo ciEntityTransactionVo : list) {
                    if (!ciEntityTransactionList.contains(ciEntityTransactionVo)) {
                        ciEntityTransactionList.add(ciEntityTransactionVo);
                    }
                }
            }
            boolean flag = false;
            JSONArray errorMessageList = new JSONArray();
            if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
                for (CiEntityTransactionVo t : ciEntityTransactionList) {
                    t.setAllowCommit(true);
                }
                System.out.println("ciEntityTransactionList = " + JSONObject.toJSONString(ciEntityTransactionList));
                System.out.println("ciEntityTransactionList.size() = " + ciEntityTransactionList.size());
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                    InputFromContext.init(InputFrom.ITSM);
                    Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
                    System.out.println("transactionGroupId = " + transactionGroupId);
                    ProcessTaskStepDataVo search = new ProcessTaskStepDataVo();
                    search.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                    search.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                    search.setType("ciEntitySyncResult");
                    ProcessTaskStepDataVo oldProcessTaskStepData = processTaskStepDataMapper.getProcessTaskStepData(search);
                    if (oldProcessTaskStepData != null) {
                        JSONObject dataObj = oldProcessTaskStepData.getData();
                        JSONArray transactionGroupIdList = dataObj.getJSONArray("transactionGroupIdList");
                        transactionGroupIdList.add(transactionGroupId);
                    } else {
                        oldProcessTaskStepData = search;
                        oldProcessTaskStepData.setFcu(UserContext.get().getUserUuid());
                        JSONObject dataObj = new JSONObject();
                        List<Long> transactionGroupIdList = new ArrayList<>();
                        transactionGroupIdList.add(transactionGroupId);
                        dataObj.put("transactionGroupIdList", transactionGroupIdList);
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
                    flag = true;
                }

            }
            boolean isAutoComplete = true;
            // 如果有异常，则根据失败策略执行操作
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
                if (!FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                    isAutoComplete = false;
                }
            }
            if (isAutoComplete) {
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
        } catch (Exception e) {
            System.out.println("异常2...");
            logger.error(e.getMessage(), e);
            throw new ProcessTaskException(e.getMessage());
        }
        return 1;
    }

    private JSONObject rebuildCiEntityConfig(JSONObject ciEntityConfig, Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        JSONArray configList = ciEntityConfig.getJSONArray("configList");
        if (CollectionUtils.isEmpty(configList)) {
            return ciEntityConfig;
        }
        Map<String, List<String>> oldUuid2NewUuidListMap = new HashMap<>();
        JSONArray configArray = new JSONArray();
        for (int i = 0; i < configList.size(); i++) {
            JSONObject configObject = configList.getJSONObject(i);
            String oldUuid = configObject.getString("uuid");
            // 批量遍历表格
            JSONObject batchDataSource = configObject.getJSONObject("batchDataSource");
            if (MapUtils.isEmpty(batchDataSource)) {
                configArray.add(configObject);
                continue;
            }
            String attributeUuid = batchDataSource.getString("attributeUuid");
            ProcessTaskFormAttributeDataVo formAttributeDataVo = processTaskFormAttributeDataMap.get(attributeUuid);
            JSONArray filterList = batchDataSource.getJSONArray("filterList");
            JSONArray tbodyList = getTbodyList(formAttributeDataVo, filterList);
            if (CollectionUtils.isEmpty(tbodyList)) {
                continue;
            }
            // 遍历表格数据
            for (int j = 0; j < tbodyList.size(); j++) {
                JSONObject newConfigObj = new JSONObject();
                JSONObject tbodyObj = tbodyList.getJSONObject(j);
                if (MapUtils.isEmpty(tbodyObj)) {
                    continue;
                }
                System.out.println("tbodyObj = " + tbodyObj);
                boolean flag = false;
                JSONArray mappingArray = new JSONArray();
                JSONArray mappingList = configObject.getJSONArray("mappingList");
                for (int k = 0; k < mappingList.size(); k++) {
                    JSONObject newMappingObj = new JSONObject();
                    JSONObject mappingObj = mappingList.getJSONObject(k);
                    String key = mappingObj.getString("key");
                    String mappingMode = mappingObj.getString("mappingMode");
                    if (Objects.equals(mappingMode, "formTableComponent")) {
                        JSONArray valueList = mappingObj.getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            String value = valueList.getString(0);
                            if (Objects.equals(value, attributeUuid)) {
                                String column = mappingObj.getString("column");
                                String columnValue = tbodyObj.getString(column);
                                if (StringUtils.isNotBlank(columnValue)) {
                                    flag = true;
                                    newMappingObj.put("key", key);
                                    newMappingObj.put("mappingMode", "constant");
                                    JSONArray newValueList = new JSONArray();
                                    newValueList.add(columnValue);
                                    newMappingObj.put("valueList", newValueList);
                                } else {
                                    continue;
                                }
                            } else {
                                newMappingObj.putAll(mappingObj);
                            }
                        } else {
                            continue;
                        }
                    } else {
                        newMappingObj.putAll(mappingObj);
                    }
                    mappingArray.add(newMappingObj);
                }
                if (flag) {
                    String newUuid = UuidUtil.randomUuid();
                    oldUuid2NewUuidListMap.computeIfAbsent(oldUuid, key -> new ArrayList<>()).add(newUuid);
                    newConfigObj.put("uuid", newUuid);
                    newConfigObj.put("ciId", configObject.get("ciId"));
                    newConfigObj.put("ciName", configObject.get("ciName"));
//                    newConfigObj.put("createPolicy", "single");
                    newConfigObj.put("ciLabel", configObject.get("ciLabel"));
                    newConfigObj.put("isStart", configObject.get("isStart"));
                    newConfigObj.put("ciIcon", configObject.get("ciIcon"));
                    newConfigObj.put("mappingList", mappingArray);
                    configArray.add(newConfigObj);
                }
            }
        }
        configList = configArray;
        for (int i = 0; i < configList.size(); i++) {
            JSONObject configObject = configList.getJSONObject(i);
            JSONArray mappingList = configObject.getJSONArray("mappingList");
            for (int k = 0; k < mappingList.size(); k++) {
                JSONObject mappingObj = mappingList.getJSONObject(k);
                String key = mappingObj.getString("key");
                if (key.startsWith("rel")) {
                    JSONArray valueList = mappingObj.getJSONArray("valueList");
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        JSONObject valueObj = valueList.getJSONObject(0);
                        String type = valueObj.getString("type");
                        if (Objects.equals(type, "new")) {
                            String ciEntityUuid = valueObj.getString("ciEntityUuid");
                            List<String> newUuidList = oldUuid2NewUuidListMap.get(ciEntityUuid);
                            if (CollectionUtils.isNotEmpty(newUuidList)) {
                                JSONArray newValueList = new JSONArray();
                                for (String newUuid : newUuidList) {
                                    JSONObject newValueObj = new JSONObject();
                                    newValueObj.putAll(valueObj);
                                    newValueObj.put("ciEntityUuid", newUuid);
                                    newValueList.add(newValueObj);
                                }
                                mappingObj.put("valueList", newValueList);
                            }
                        }
                    }
                }
            }
        }
        List<String> needReplaceUuidList = new ArrayList<>();
        for (int i = 0; i < configList.size(); i++) {
            JSONObject configObject = configList.getJSONObject(i);
            Integer isStart = configObject.getInteger("isStart");
            if (Objects.equals(isStart, 1)) {
            }
            String uuid = configObject.getString("uuid");
            Long id = configObject.getLong("id");
            if (id != null) {
                CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(id);
                if (ciEntityVo == null) {
                    configObject.remove("id");
                    needReplaceUuidList.add(uuid);
                }
            } else if (StringUtils.isNotBlank(uuid)) {
                CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(uuid);
                if (ciEntityVo != null) {
                    if (Objects.equals(isStart, 1)) {
                        needReplaceUuidList.add(uuid);
                    } else {
                        configObject.put("id", ciEntityVo.getId());
                    }
                }
            }
        }
        ciEntityConfig.put("configList", configList);
        if (CollectionUtils.isNotEmpty(needReplaceUuidList)) {
            System.out.println("needReplaceUuidList = " + JSONObject.toJSONString(needReplaceUuidList));
            String ciEntityConfigStr = ciEntityConfig.toJSONString();
            for (String oldUuid : needReplaceUuidList) {
                String newUuid = UuidUtil.randomUuid();
                ciEntityConfigStr = ciEntityConfigStr.replace(oldUuid, newUuid);
            }
            return JSONObject.parseObject(ciEntityConfigStr);
//            configList = ciEntityConfig.getJSONArray("configList");
        }
        return ciEntityConfig;
    }
    private List<CiEntityTransactionVo> createSingleCiEntityVo(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            JSONObject mainConfigObj,
            Map<String, JSONObject> dependencyConfigMap,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();

        Long ciId = mainConfigObj.getLong("ciId");
        Map<String, JSONObject> mappingMap = new HashMap<>();
        JSONArray mappingList = mainConfigObj.getJSONArray("mappingList");
        for (int i = 0; i < mappingList.size(); i++) {
            JSONObject mappingObj = mappingList.getJSONObject(i);
            if (MapUtils.isEmpty(mappingObj)) {
                continue;
            }
            String key = mappingObj.getString("key");
            if (StringUtils.isBlank(key)) {
                continue;
            }
            mappingMap.put(key, mappingObj);
        }
        Long id = mainConfigObj.getLong("id");
        String uuid = mainConfigObj.getString("uuid");
        CiEntityTransactionVo ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
        if (id != null) {
            ciEntityTransactionVo.setCiEntityId(id);
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        } else {
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        }
        ciEntityTransactionVo.setCiId(ciId);
        /** 变更说明 **/
        JSONObject descriptionMappingObj = mappingMap.get("description");
        if (MapUtils.isNotEmpty(descriptionMappingObj)) {
            String description = parseDescription(descriptionMappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
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
            String key = "attr_" + attrVo.getId();
            JSONObject mappingObj = mappingMap.get(key);
            if (MapUtils.isEmpty(mappingObj)) {
                System.out.println(key + " is null");
                continue;
            }
            JSONObject attrEntity = parseAttr(ciEntityTransactionMap, attrVo, mappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
            attrEntityData.put(key, attrEntity);
        }
        ciEntityTransactionVo.setAttrEntityData(attrEntityData);
        /** 关系 **/
        JSONObject relEntityData = new JSONObject();
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        for (RelVo relVo : relList) {
            String key = "rel" + relVo.getDirection() + "_" + relVo.getId();
            JSONObject mappingObj = mappingMap.get(key);
            if (MapUtils.isEmpty(mappingObj)) {
                System.out.println(key + " is null");
                continue;
            }
            String mappingMode = mappingObj.getString("mappingMode");
            if (Objects.equals(mappingMode, "new")) {
                JSONArray valueArray = mappingObj.getJSONArray("valueList");
                if (CollectionUtils.isNotEmpty(valueArray)) {
                    for (int i = 0; i < valueArray.size(); i++) {
                        JSONObject valueObj = valueArray.getJSONObject(i);
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
                                System.out.println("tmpVo.getCiEntityId() = " + tmpVo.getCiEntityId());
                                valueObj.put("ciEntityId", tmpVo.getCiEntityId());
                            } else {
                                CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(ciEntityUuid);
                                if (uuidCiEntityVo == null) {
                                    throw new NewCiEntityNotFoundException(valueObj.getString("ciEntityUuid"));
                                } else {
                                    valueObj.put("ciEntityId", uuidCiEntityVo.getId());
                                }
                            }
                        }
                        String type = valueObj.getString("type");
                        if (!Objects.equals(type, "new")) {
                            continue;
                        }
                        JSONObject dependencyConfig = dependencyConfigMap.get(ciEntityUuid);
                        if (MapUtils.isNotEmpty(dependencyConfig)) {
                            List<CiEntityTransactionVo> list = createSingleCiEntityVo(ciEntityTransactionMap, dependencyConfig, dependencyConfigMap, formAttributeMap, processTaskFormAttributeDataMap);
                            ciEntityTransactionList.addAll(list);
                        }
                    }
                    JSONObject relEntity = parseRel(relVo, mappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
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
            JSONObject mappingObj = mappingMap.get(key);
            if (MapUtils.isEmpty(mappingObj)) {
                System.out.println(key + " is null");
                continue;
            }
            JSONObject globalAttrEntity = parseGlobalAttr(globalAttrVo, mappingObj, null, formAttributeMap, processTaskFormAttributeDataMap);
            globalAttrEntityData.put(key, globalAttrEntity);
        }
        ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrEntityData);

        ciEntityTransactionList.add(ciEntityTransactionVo);
        return ciEntityTransactionList;
    }

    private JSONArray getTbodyList(ProcessTaskFormAttributeDataVo formAttributeDataVo, JSONArray filterList) {
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
                for (int j = 0; j < filterList.size(); j++) {
                    JSONObject filterObj = filterList.getJSONObject(j);
                    if (MapUtils.isEmpty(filterObj)) {
                        continue;
                    }
                    String column = filterObj.getString("column");
                    if (StringUtils.isBlank(column)) {
                        continue;
                    }
                    String expression = filterObj.getString("expression");
                    if (StringUtils.isBlank(expression)) {
                        continue;
                    }
                    String value = filterObj.getString("value");
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

    private List<String> parseFormTableComponentMappingValue(FormAttributeVo formAttributeVo, JSONArray tbodyList, String column) {
        List<String> resultList = new ArrayList<>();
        if (CollectionUtils.isEmpty(tbodyList)) {
            return resultList;
        }
        for (int i = 0; i < tbodyList.size(); i++) {
            JSONObject tbodyObj = tbodyList.getJSONObject(i);
            if (MapUtils.isEmpty(tbodyObj)) {
                continue;
            }
            String columnValue = tbodyObj.getString(column);
            if (StringUtils.isBlank(columnValue)) {
                continue;
            }
            resultList.add(columnValue);
        }
        return resultList;
    }

    private String mappingModeFormTableComponent(
            JSONObject mappingObj,
            JSONObject tbodyObj) {
        String column = mappingObj.getString("column");
        return tbodyObj.getString(column);
    }

    private List<String> mappingModeFormTableComponent(
            JSONObject mappingObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        JSONArray valueList = mappingObj.getJSONArray("valueList");
        if (CollectionUtils.isEmpty(valueList)) {
            return null;
        }
        String column = mappingObj.getString("column");
        FormAttributeVo formAttributeVo = formAttributeMap.get(valueList.getString(0));
        ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(valueList.getString(0));
        JSONArray filterList = mappingObj.getJSONArray("filterList");
        JSONArray tbodyList = getTbodyList(attributeDataVo, filterList);
        List<String> list = parseFormTableComponentMappingValue(formAttributeVo, tbodyList, column);
        return list;
    }

    private Object mappingModeFormCommonComponent(
            JSONObject mappingObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        JSONArray valueList = mappingObj.getJSONArray("valueList");
        if (CollectionUtils.isEmpty(valueList)) {
            return null;
        }
        FormAttributeVo formAttributeVo = formAttributeMap.get(valueList.getString(0));
        if (formAttributeVo == null) {
            return null;
        }
        ProcessTaskFormAttributeDataVo attributeDataVo = processTaskFormAttributeDataMap.get(valueList.getString(0));
        if (attributeDataVo == null) {
            return null;
        }
        Object dataObj = attributeDataVo.getDataObj();
        if (dataObj == null) {
            return null;
        }
        if (Objects.equals(formAttributeVo.getHandler(), FormHandler.FORMUPLOAD.getHandler())) {
            List<Long> idList = new ArrayList<>();
            if (dataObj instanceof JSONArray) {
                JSONArray dataArray = (JSONArray) dataObj;
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);
                    Long id = data.getLong("id");
                    if (id != null) {
                        idList.add(id);
                    }
                }
            }
            return idList;
        }
        return dataObj;
    }

    private String parseDescription(
            JSONObject mappingObj,
            JSONObject tbodyObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        String mappingMode = mappingObj.getString("mappingMode");
        if (Objects.equals(mappingMode, "formTableComponent")) {
            // 映射模式为表单表格组件
            List<String> list = mappingModeFormTableComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
            if (CollectionUtils.isNotEmpty(list)) {
                return String.join(",", list);
            }
        } else if (Objects.equals(mappingMode, "formCommonComponent")) {
            // 映射模式为表单普通组件
            Object value = mappingModeFormCommonComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
            if (value != null) {
                return value.toString();
            }
        } else if (Objects.equals(mappingMode, "constant")) {
            // 映射模式为常量
            JSONArray valueList = mappingObj.getJSONArray("valueList");
            if (CollectionUtils.isNotEmpty(valueList)) {
                return valueList.getString(0);
            }
        }
        return null;
    }

    private JSONObject parseGlobalAttr(
            GlobalAttrVo globalAttrVo,
            JSONObject mappingObj,
            JSONObject tbodyObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        List<GlobalAttrItemVo> itemList = globalAttrVo.getItemList();
        Map<Long, GlobalAttrItemVo> id2ItemMap = itemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        Map<String, GlobalAttrItemVo> name2ItemMap = itemList.stream().collect(Collectors.toMap(e -> e.getValue(), e -> e));
        JSONArray valueList = new JSONArray();
        String mappingMode = mappingObj.getString("mappingMode");
        if (Objects.equals(mappingMode, "formTableComponent")) {
            // 映射模式为表单表格组件
            if (tbodyObj != null) {
                String column = mappingObj.getString("column");
                String value = tbodyObj.getString(column);
                GlobalAttrItemVo globalAttrItemVo = name2ItemMap.get(value);
                if (globalAttrItemVo != null) {
                    JSONObject valueObj = new JSONObject();
                    valueObj.put("attrId", globalAttrVo.getId());
                    valueObj.put("id", globalAttrItemVo.getId());
                    valueObj.put("sort", globalAttrItemVo.getSort());
                    valueObj.put("value", globalAttrItemVo.getValue());
                    valueList.add(valueObj);
                }
            } else {
                List<String> list = mappingModeFormTableComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
                for (String value : list) {
                    GlobalAttrItemVo globalAttrItemVo = name2ItemMap.get(value);
                    if (globalAttrItemVo != null) {
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("attrId", globalAttrVo.getId());
                        valueObj.put("id", globalAttrItemVo.getId());
                        valueObj.put("sort", globalAttrItemVo.getSort());
                        valueObj.put("value", globalAttrItemVo.getValue());
                        valueList.add(valueObj);
                        if (!Objects.equals(globalAttrVo.getIsMultiple(), 1)) {
                            break;
                        }
                    }
                }
            }
        } else if (Objects.equals(mappingMode, "formCommonComponent")) {
            // 映射模式为表单普通组件
            Object value = mappingModeFormCommonComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
            if (value != null) {
                if (value instanceof JSONObject) {

                } else if (value instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) value;
                    for (int i = 0; i < valueArray.size(); i++) {
                        String valueStr = valueArray.getString(0);
                        GlobalAttrItemVo globalAttrItemVo = name2ItemMap.get(valueStr);
                        if (globalAttrItemVo != null) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("attrId", globalAttrVo.getId());
                            valueObj.put("id", globalAttrItemVo.getId());
                            valueObj.put("sort", globalAttrItemVo.getSort());
                            valueObj.put("value", globalAttrItemVo.getValue());
                            valueList.add(valueObj);
                            if (!Objects.equals(globalAttrVo.getIsMultiple(), 1)) {
                                break;
                            }
                        }
                    }
                } else if (value instanceof Long) {
                    GlobalAttrItemVo globalAttrItemVo = id2ItemMap.get((Long) value);
                    if (globalAttrItemVo != null) {
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("attrId", globalAttrVo.getId());
                        valueObj.put("id", globalAttrItemVo.getId());
                        valueObj.put("sort", globalAttrItemVo.getSort());
                        valueObj.put("value", globalAttrItemVo.getValue());
                        valueList.add(valueObj);
                    }
                } else if (value instanceof String) {
                    GlobalAttrItemVo globalAttrItemVo = name2ItemMap.get((String) value);
                    if (globalAttrItemVo != null) {
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("attrId", globalAttrVo.getId());
                        valueObj.put("id", globalAttrItemVo.getId());
                        valueObj.put("sort", globalAttrItemVo.getSort());
                        valueObj.put("value", globalAttrItemVo.getValue());
                        valueList.add(valueObj);
                    }
                } else {
                    GlobalAttrItemVo globalAttrItemVo = name2ItemMap.get(value.toString());
                    if (globalAttrItemVo != null) {
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("attrId", globalAttrVo.getId());
                        valueObj.put("id", globalAttrItemVo.getId());
                        valueObj.put("sort", globalAttrItemVo.getSort());
                        valueObj.put("value", globalAttrItemVo.getValue());
                        valueList.add(valueObj);
                    }
                }
            }
        } else if (Objects.equals(mappingMode, "constant")) {
            // 映射模式为常量
            JSONArray valueArray = mappingObj.getJSONArray("valueList");
            if (CollectionUtils.isNotEmpty(valueArray)) {
//                valueList = valueArray;
                for (int i = 0; i < valueArray.size(); i++) {
                    Object value = valueArray.get(i);
                    if (value instanceof JSONObject) {
                        valueList.add(value);
                    } else if (value instanceof Long) {
                        GlobalAttrItemVo globalAttrItemVo = id2ItemMap.get((Long) value);
                        if (globalAttrItemVo != null) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("attrId", globalAttrVo.getId());
                            valueObj.put("id", globalAttrItemVo.getId());
                            valueObj.put("sort", globalAttrItemVo.getSort());
                            valueObj.put("value", globalAttrItemVo.getValue());
                            valueList.add(valueObj);
                            if (!Objects.equals(globalAttrVo.getIsMultiple(), 1)) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("valueList", valueList);
        return resultObj;
    }

    private JSONObject parseAttr(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            AttrVo attrVo,
            JSONObject mappingObj,
            JSONObject tbodyObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        JSONArray valueList = new JSONArray();
        String mappingMode = mappingObj.getString("mappingMode");
        if (Objects.equals(mappingMode, "formTableComponent")) {
            // 映射模式为表单表格组件
            if (tbodyObj != null) {
                String column = mappingObj.getString("column");
                String value = tbodyObj.getString(column);
                if (StringUtils.isNotBlank(value)) {
                    valueList.add(value);
                }
            } else {
                List<String> list = mappingModeFormTableComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
                if (CollectionUtils.isNotEmpty(list)) {
                    valueList.addAll(list);
                }
            }
        } else if (Objects.equals(mappingMode, "formCommonComponent")) {
            // 映射模式为表单普通组件
            Object value = mappingModeFormCommonComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
            if (value != null) {
                if (value instanceof JSONObject) {

                } else if (value instanceof JSONArray) {
                    valueList = (JSONArray) value;
                } else {
                    valueList.add(value);
                }
            }
        } else if (Objects.equals(mappingMode, "constant")) {
            // 映射模式为常量
            JSONArray valueArray = mappingObj.getJSONArray("valueList");
            if (CollectionUtils.isNotEmpty(valueArray)) {
                valueList = valueArray;
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
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("type", attrVo.getType());
        resultObj.put("config", attrVo.getConfig());
        resultObj.put("valueList", valueList);
        return resultObj;
    }

    private JSONObject parseRel(
            RelVo relVo,
            JSONObject mappingObj,
            JSONObject tbodyObj,
            Map<String, FormAttributeVo> formAttributeMap,
            Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap) {
        Long ciId = null;
        if (Objects.equals(relVo.getDirection(), "from")) {
            ciId = relVo.getToCiId();
        } else if (Objects.equals(relVo.getDirection(), "to")) {
            ciId = relVo.getFromCiId();
        }
        JSONArray valueList = new JSONArray();
        String mappingMode = mappingObj.getString("mappingMode");
        if (Objects.equals(mappingMode, "formTableComponent")) {
            // 映射模式为表单表格组件
            if (tbodyObj != null) {
                String column = mappingObj.getString("column");
                String value = tbodyObj.getString(column);
                Long ciEntityId = ciEntityMapper.getIdByCiIdAndName(ciId, value);
                if (ciEntityId != null) {
                    CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
                    if (ciEntityVo != null) {
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("ciId", ciEntityVo.getTypeId());
                        valueObj.put("ciEntityId", ciEntityVo.getId());
                        valueObj.put("ciEntityName", ciEntityVo.getName());
                        valueList.add(valueObj);
                    }
                }
            } else {
                List<String> list = mappingModeFormTableComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
                for (String value : list) {
                    Long ciEntityId = ciEntityMapper.getIdByCiIdAndName(ciId, value);
                    if (ciEntityId != null) {
                        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
                        if (ciEntityVo != null) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("ciId", ciEntityVo.getTypeId());
                            valueObj.put("ciEntityId", ciEntityVo.getId());
                            valueObj.put("ciEntityName", ciEntityVo.getName());
                            valueList.add(valueObj);
                        }
                    }
                }
            }
        } else if (Objects.equals(mappingMode, "formCommonComponent")) {
            // 映射模式为表单普通组件
            Object value = mappingModeFormCommonComponent(mappingObj, formAttributeMap, processTaskFormAttributeDataMap);
            if (value != null) {
                if (value instanceof JSONObject) {

                } else if (value instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) value;
                    for (int i = 0; i < valueArray.size(); i++) {
                        Long ciEntityId = valueArray.getLong(i);
                        if (ciEntityId == null) {
                            continue;
                        }
                        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
                        if (ciEntityVo != null) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("ciId", ciEntityVo.getTypeId());
                            valueObj.put("ciEntityId", ciEntityVo.getId());
                            valueObj.put("ciEntityName", ciEntityVo.getName());
                            valueList.add(valueObj);
                        }
                    }
                } else if (value instanceof Long) {
                    Long ciEntityId = (Long) value;
                    CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
                    if (ciEntityVo != null) {
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("ciId", ciEntityVo.getTypeId());
                        valueObj.put("ciEntityId", ciEntityVo.getId());
                        valueObj.put("ciEntityName", ciEntityVo.getName());
                        valueList.add(valueObj);
                    }
                } else {
                    Long ciEntityId = ciEntityMapper.getIdByCiIdAndName(ciId, value.toString());
                    if (ciEntityId != null) {
                        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
                        if (ciEntityVo != null) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("ciId", ciEntityVo.getTypeId());
                            valueObj.put("ciEntityId", ciEntityVo.getId());
                            valueObj.put("ciEntityName", ciEntityVo.getName());
                            valueList.add(valueObj);
                        }
                    }
                }
            }
        } else if (Objects.equals(mappingMode, "constant")) {
            // 映射模式为常量
            JSONArray valueArray = mappingObj.getJSONArray("valueList");
            if (CollectionUtils.isNotEmpty(valueArray)) {
                valueList = valueArray;
            }
        } else if (Objects.equals(mappingMode, "new")) {
            JSONArray valueArray = mappingObj.getJSONArray("valueList");
            if (CollectionUtils.isNotEmpty(valueArray)) {
                valueList = valueArray;
//                for (int i = 0; i < valueArray.size(); i++) {
//                    JSONObject valueObj = valueArray.getJSONObject(i);
//                    if (MapUtils.isEmpty(valueObj)) {
//                        continue;
//                    }
//                    String ciEntityUuid = valueObj.getString("ciEntityUuid");
//                    if (StringUtils.isBlank(ciEntityUuid)) {
//                        continue;
//                    }
//
//                }
            }
        }
        JSONObject resultObj = new JSONObject();
//        resultObj.put("type", attrVo.getType());
//        resultObj.put("config", attrVo.getConfig());
        resultObj.put("valueList", valueList);
        return resultObj;
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
