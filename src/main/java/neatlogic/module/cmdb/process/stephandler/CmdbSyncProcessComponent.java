/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.process.stephandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.AttrEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.*;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import neatlogic.framework.cmdb.exception.attrtype.AttrTypeNotFoundException;
import neatlogic.framework.cmdb.exception.ci.*;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.Expression;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.form.constvalue.FormHandler;
import neatlogic.framework.process.constvalue.ProcessFlowDirection;
import neatlogic.framework.process.constvalue.ProcessStepMode;
import neatlogic.framework.process.constvalue.ProcessTaskAuditType;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.automatic.FailPolicy;
import neatlogic.framework.process.crossover.*;
import neatlogic.framework.process.dto.*;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import neatlogic.framework.process.stephandler.core.IProcessStepHandler;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.process.stephandler.core.ProcessStepThread;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.framework.util.FormUtil;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.process.constvalue.CmdbAuditDetailType;
import neatlogic.module.cmdb.process.dto.*;
import neatlogic.module.cmdb.process.exception.AbstractCiTargetCiIdAttrNotFoundException;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
@Deprecated
//@Service
public class CmdbSyncProcessComponent extends ProcessStepHandlerBase {

    private final Logger logger = LoggerFactory.getLogger(CmdbSyncProcessComponent.class);

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
    private CiEntityService ciEntityService;

    @Resource
    private RelEntityMapper relEntityMapper;

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
                IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
                ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
                configHash = processTaskStepVo.getConfigHash();
                currentProcessTaskStepVo.setProcessStepUuid(processTaskStepVo.getProcessStepUuid());
            }
            // 获取工单当前步骤配置信息
            ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
            String config = selectContentByHashCrossoverMapper.getProcessTaskStepConfigByHash(configHash);
            if (StringUtils.isBlank(config)) {
                myAutoComplete(currentProcessTaskStepVo);
                return 0;
            }
            JSONObject ciEntityConfig = (JSONObject) JSONPath.read(config, "ciEntityConfig");
            if (MapUtils.isEmpty(ciEntityConfig)) {
                myAutoComplete(currentProcessTaskStepVo);
                return 0;
            }

            IProcessTaskStepDataCrossoverMapper processTaskStepDataCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskStepDataCrossoverMapper.class);
            boolean flag = false;
            JSONArray errorMessageList = new JSONArray();
            try {
                CiEntitySyncVo ciEntitySyncVo = ciEntityConfig.toJavaObject(CiEntitySyncVo.class);
                // rerunStepToSync为1时表示重新激活CMDB步骤时同步配置项实例，rerunStepToSync为0时表示重新激活CMDB步骤时不同步配置项实例，即什么都不做，直接自动流转到下一阶段
                Integer rerunStepToSync = ciEntitySyncVo.getRerunStepToSync();
                if (!Objects.equals(rerunStepToSync, 1)) {
                    ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
                    searchVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                    searchVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                    searchVo.setType("ciEntitySyncResult");
                    ProcessTaskStepDataVo processTaskStepData = processTaskStepDataCrossoverMapper.getProcessTaskStepData(searchVo);
                    if (processTaskStepData != null) {
                        myAutoComplete(currentProcessTaskStepVo);
                        return 1;
                    }
                }
                ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
                searchVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                searchVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                searchVo.setType("ciEntitySyncError");
                ProcessTaskStepDataVo processTaskStepData = processTaskStepDataCrossoverMapper.getProcessTaskStepData(searchVo);
                if (processTaskStepData != null) {
                    processTaskStepDataCrossoverMapper.deleteProcessTaskStepDataById(processTaskStepData.getId());
                }
                List<CiEntitySyncConfigVo> configList = ciEntitySyncVo.getConfigList();
                /* 重新构建configList配置信息 */
                configList = rebuildConfigList(configList, currentProcessTaskStepVo.getProcessTaskId());
                if (CollectionUtils.isEmpty(configList)) {
                    myAutoComplete(currentProcessTaskStepVo);
                    return 1;
                }
                /* 遍历configList， 找出起始模型配置列表，根据id和uuid的值判断是新增还是更新 */
                List<CiEntitySyncConfigVo> startConfigList = new ArrayList<>();
                Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
                Map<String, CiEntitySyncConfigVo> dependencyConfigMap = new HashMap<>();
                for (CiEntitySyncConfigVo configObject : configList) {
                    List<Long> uniqueAttrIdList = ciMapper.getCiUniqueByCiId(configObject.getCiId());
                    List<AttrVo> attrList = attrMapper.getAttrByCiId(configObject.getCiId());
                    for (AttrVo attrVo : attrList) {
                        if (uniqueAttrIdList.contains(attrVo.getId()) && Objects.equals(attrVo.getType(), "expression")) {
                            attrVo.setCiLabel(configObject.getCiLabel());
                            attrVo.setCiName(configObject.getCiName());
                            throw new CiUniqueRuleAttrTypeIrregularException(attrVo);
                        }
                    }
                    String uuid = configObject.getUuid();
                    CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                    ciEntityTransactionVo.setCiEntityUuid(uuid);
                    ciEntityTransactionVo.setCiId(configObject.getCiId());
//                    ciEntityTransactionVo.setCiName(configObject.getCiName());
                    ciEntityTransactionVo.setAllowCommit(true);
                    if (StringUtils.isNotBlank(configObject.getEditMode())) {
                        ciEntityTransactionVo.setEditMode(configObject.getEditMode());
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
                    myAutoComplete(currentProcessTaskStepVo);
                    return 0;
                }
                /* 遍历起始模型配置信息列表，生成CiEntityTransactionVo列表 */
                List<CiEntityTransactionVo> allCiEntityTransactionList = new ArrayList<>();
                for (CiEntitySyncConfigVo mainConfigObj : startConfigList) {
                    List<CiEntityTransactionVo> list = createCiEntityTransactionVo(ciEntityTransactionMap, mainConfigObj, dependencyConfigMap);
                    for (CiEntityTransactionVo ciEntityTransactionVo : list) {
                        if (!allCiEntityTransactionList.contains(ciEntityTransactionVo)) {
                            allCiEntityTransactionList.add(ciEntityTransactionVo);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(allCiEntityTransactionList)) {
                    // 遍历ciEntityTransactionList，根据唯一规则属性值删除重复配置信息
                    List<CiEntityTransactionVo> ciEntityTransactionList = removeDuplicatesByUniqueAttrValue(allCiEntityTransactionList);
                    EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                        InputFromContext.init(InputFrom.ITSM);
                        Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
                        currentProcessTaskStepVo.getParamObj().put(CmdbAuditDetailType.CMDBSYNCMESSAGE.getParamName(), transactionGroupId);
                        ProcessTaskStepDataVo search = new ProcessTaskStepDataVo();
                        search.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                        search.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                        search.setType("ciEntitySyncResult");
                        ProcessTaskStepDataVo oldProcessTaskStepData = processTaskStepDataCrossoverMapper.getProcessTaskStepData(search);
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
                        processTaskStepDataCrossoverMapper.replaceProcessTaskStepData(oldProcessTaskStepData);
                    }).execute();
                    if (!s.isSucceed()) {
                        // 增加提醒
                        logger.error(s.getError(), s.getException());
                        logger.error("导致异常的数据：" + JSONObject.toJSONString(ciEntityTransactionList));
                        JSONObject errorMessageObj = new JSONObject();
                        String error = s.getError();
                        if (error == null) {
                            error = "null";
                        }
                        errorMessageObj.put("error", error);
                        errorMessageList.add(errorMessageObj);
                        JSONObject dataObj = new JSONObject();
                        dataObj.put("time", System.currentTimeMillis());
                        dataObj.put("errorList", errorMessageList);
                        currentProcessTaskStepVo.getParamObj().put(CmdbAuditDetailType.CMDBSYNCMESSAGE.getParamName(), dataObj.toJSONString());
                        flag = true;
                    }
                    /* 处理历史记录 **/
                    IProcessStepHandlerCrossoverUtil processStepHandlerCrossoverUtil = CrossoverServiceFactory.getApi(IProcessStepHandlerCrossoverUtil.class);
                    processStepHandlerCrossoverUtil.audit(currentProcessTaskStepVo, ProcessTaskAuditType.ACTIVE);
                }
            } catch (Exception e) {
                // 增加提醒
                logger.error(e.getMessage(), e);
                JSONObject errorMessageObj = new JSONObject();
                String error = e.getMessage();
                if (error == null) {
                    error = "null";
                }
                errorMessageObj.put("error", error);
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
                processTaskStepDataCrossoverMapper.replaceProcessTaskStepData(processTaskStepDataVo);
                String failPolicy = ciEntityConfig.getString("failPolicy");
                if (FailPolicy.KEEP_ON.getValue().equals(failPolicy)) {
                    myAutoComplete(currentProcessTaskStepVo);
                }
            } else {
                myAutoComplete(currentProcessTaskStepVo);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ProcessTaskException(e);
        }
        return 1;
    }

    private void myAutoComplete(ProcessTaskStepVo currentProcessTaskStepVo) {
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        List<Long> toProcessTaskStepIdList = processTaskCrossoverMapper.getToProcessTaskStepIdListByFromIdAndType(currentProcessTaskStepVo.getId(), ProcessFlowDirection.FORWARD.getValue());
        if (toProcessTaskStepIdList.size() == 1) {
            Long nextStepId = toProcessTaskStepIdList.get(0);
            IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(CmdbProcessStepHandlerType.CMDBSYNC.getHandler());
            try {
                ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
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
     * 遍历ciEntityTransactionList，根据唯一规则属性值删除重复配置信息
     * @param ciEntityTransactionList 配置信息列表
     * @return
     */
    private List<CiEntityTransactionVo> removeDuplicatesByUniqueAttrValue(List<CiEntityTransactionVo> ciEntityTransactionList) {
        // 保存唯一规则属性值组成的字符串与uuid的映射关系
        Map<String, String> uniqueAttrValueListJoinStr2UuidMap = new HashMap<>();
        // 保存将要删除uuid与等价替换uuid的映射关系
        Map<String, String> toDeleteUuid2EquivalentUuidMap = new HashMap<>();
        Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
        // 反向遍历，如果唯一规则属性值相同，后面数据优先级高
        for (int i = ciEntityTransactionList.size() - 1; i >= 0; i--) {
            CiEntityTransactionVo configObj = ciEntityTransactionList.get(i);
            ciEntityTransactionMap.put(configObj.getCiEntityUuid(), configObj);
            String uniqueAttrValueListJoinStr = "";
            if (Objects.equals(configObj.getAction(), TransactionActionType.UPDATE.getValue())) {
                uniqueAttrValueListJoinStr = configObj.getCiEntityId().toString();
            } else {
                Long ciId = configObj.getCiId();
                CiVo ciVo = ciMapper.getCiById(ciId);
                List<Long> uniqueAttrIdList = ciVo.getUniqueAttrIdList();
                if (CollectionUtils.isEmpty(uniqueAttrIdList)) {
                    continue;
                }
                List<String> list = new ArrayList<>();
                list.add(ciId.toString());
                for (Long attrId : uniqueAttrIdList) {
                    AttrEntityTransactionVo attrEntityTransaction = configObj.getAttrEntityTransactionByAttrId(attrId);
                    if (attrEntityTransaction != null) {
                        JSONArray valueList = attrEntityTransaction.getValueList();
                        if (valueList != null) {
                            list.add(valueList.toJSONString());
                        }
                    }
                }
                uniqueAttrValueListJoinStr = String.join(",", list);
            }

            String equivalentUuid = uniqueAttrValueListJoinStr2UuidMap.get(uniqueAttrValueListJoinStr);
            if (StringUtils.isNotBlank(equivalentUuid)) {
                toDeleteUuid2EquivalentUuidMap.put(configObj.getCiEntityUuid(), equivalentUuid);
            } else {
                uniqueAttrValueListJoinStr2UuidMap.put(uniqueAttrValueListJoinStr, configObj.getCiEntityUuid());
            }
        }

        List<CiEntityTransactionVo> newConfigList = new ArrayList<>();
        for (CiEntityTransactionVo configObj : ciEntityTransactionList) {
            if (toDeleteUuid2EquivalentUuidMap.containsKey(configObj.getCiEntityUuid())) {
                continue;
            }
            JSONObject relEntityData = configObj.getRelEntityData();
            for (Map.Entry<String, Object> entry : relEntityData.entrySet()) {
                JSONObject valueObject = (JSONObject) entry.getValue();
                JSONArray valueList = valueObject.getJSONArray("valueList");
                if (CollectionUtils.isEmpty(valueList)) {
                    continue;
                }
                for (int i = 0; i < valueList.size(); i++) {
                    JSONObject valueObj = valueList.getJSONObject(i);
                    if (MapUtils.isEmpty(valueObj)) {
                        continue;
                    }
                    String ciEntityUuid = valueObj.getString("ciEntityUuid");
                    String equivalentUuid = toDeleteUuid2EquivalentUuidMap.get(ciEntityUuid);
                    if (StringUtils.isNotBlank(equivalentUuid)) {
                        valueObj.put("ciEntityUuid", equivalentUuid);
                        merge(ciEntityTransactionMap.get(equivalentUuid), ciEntityTransactionMap.get(ciEntityUuid));
                    }
                }
            }
            newConfigList.add(configObj);
        }
        return newConfigList;
    }

    private void merge(CiEntityTransactionVo ciEntityTransactionA, CiEntityTransactionVo ciEntityTransactionB) {
        {
            JSONObject attrEntityDataA = ciEntityTransactionA.getAttrEntityData();
            JSONObject attrEntityDataB = ciEntityTransactionB.getAttrEntityData();
            if (MapUtils.isNotEmpty(attrEntityDataB)) {
                if (MapUtils.isNotEmpty(attrEntityDataA)) {
                    for (Map.Entry<String, Object> entryB : attrEntityDataB.entrySet()) {
                        JSONObject attrEntityB = (JSONObject) entryB.getValue();
                        JSONArray valueListB = attrEntityB.getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueListB)) {
                            boolean keyExists = false;
                            for (Map.Entry<String, Object> entryA : attrEntityDataA.entrySet()) {
                                if (Objects.equals(entryA.getKey(), entryB.getKey())) {
                                    keyExists = true;
                                    JSONObject attrEntityA = (JSONObject) entryA.getValue();
                                    JSONArray valueListA = attrEntityA.getJSONArray("valueList");
                                    if (CollectionUtils.isEmpty(valueListA)) {
                                        attrEntityDataA.put(entryB.getKey(), attrEntityB);
                                    }
                                }
                                if (keyExists) {
                                    break;
                                }
                            }
                            if (!keyExists) {
                                attrEntityDataA.put(entryB.getKey(), attrEntityB);
                            }
                        }
                    }
                } else {
                    ciEntityTransactionA.setAttrEntityData(attrEntityDataB);
                }
            }
        }
        {
            JSONObject globalAttrEntityDataA = ciEntityTransactionA.getGlobalAttrEntityData();
            JSONObject globalAttrEntityDataB = ciEntityTransactionB.getGlobalAttrEntityData();
            if (MapUtils.isNotEmpty(globalAttrEntityDataB)) {
                if (MapUtils.isNotEmpty(globalAttrEntityDataA)) {
                    for (Map.Entry<String, Object> entryB : globalAttrEntityDataB.entrySet()) {
                        JSONObject globalAttrEntityB = (JSONObject) entryB.getValue();
                        JSONArray valueListB = globalAttrEntityB.getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueListB)) {
                            boolean keyExists = false;
                            for (Map.Entry<String, Object> entryA : globalAttrEntityDataA.entrySet()) {
                                if (Objects.equals(entryA.getKey(), entryB.getKey())) {
                                    keyExists = true;
                                    JSONObject globalAttrEntityA = (JSONObject) entryA.getValue();
                                    JSONArray valueListA = globalAttrEntityA.getJSONArray("valueList");
                                    if (CollectionUtils.isEmpty(valueListA)) {
                                        globalAttrEntityDataA.put(entryB.getKey(), globalAttrEntityB);
                                    }
                                }
                                if (keyExists) {
                                    break;
                                }
                            }
                            if (!keyExists) {
                                globalAttrEntityDataA.put(entryB.getKey(), globalAttrEntityB);
                            }
                        }
                    }
                }  else {
                    ciEntityTransactionA.setGlobalAttrEntityData(globalAttrEntityDataB);
                }
            }
        }
        {
            JSONObject relEntityDataA = ciEntityTransactionA.getRelEntityData();
            JSONObject relEntityDataB = ciEntityTransactionB.getRelEntityData();
            if (MapUtils.isNotEmpty(relEntityDataB)) {
                if (MapUtils.isNotEmpty(relEntityDataA)) {
                    for (Map.Entry<String, Object> entryB : relEntityDataB.entrySet()) {
                        JSONObject attrEntityB = (JSONObject) entryB.getValue();
                        JSONArray valueListB = attrEntityB.getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueListB)) {
                            boolean keyExists = false;
                            for (Map.Entry<String, Object> entryA : relEntityDataA.entrySet()) {
                                if (Objects.equals(entryA.getKey(), entryB.getKey())) {
                                    keyExists = true;
                                    JSONObject attrEntityA = (JSONObject) entryA.getValue();
                                    JSONArray valueListA = attrEntityA.getJSONArray("valueList");
                                    valueListA.addAll(valueListB);
                                }
                                if (keyExists) {
                                    break;
                                }
                            }
                            if (!keyExists) {
                                relEntityDataA.put(entryB.getKey(), attrEntityB);
                            }
                        }
                    }
                } else {
                    ciEntityTransactionA.setAttrEntityData(relEntityDataB);
                }
            }
        }
    }

    /**
     * 重新构建configList配置信息
     * @param originalConfigList 原始配置信息列表
     * @param processTaskId 工单ID
     * @return
     */
    private List<CiEntitySyncConfigVo> rebuildConfigList(
            List<CiEntitySyncConfigVo> originalConfigList,
            Long processTaskId
    ) {
        List<CiEntitySyncConfigVo> newConfigList = new ArrayList<>();
        // 找出起始模型配置信息
        CiEntitySyncConfigVo startConfigObj = originalConfigList.stream().filter(e -> Objects.equals(e.getIsStart(), 1)).findFirst().get();
        if (startConfigObj != null) {
            Map<String, Object> formAttributeDataMap = new HashMap<>();
            String formConfig = null;
            // 如果工单有表单信息，则查询出表单配置及数据
            IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
            ProcessTaskFormVo processTaskFormVo = processTaskCrossoverMapper.getProcessTaskFormByProcessTaskId(processTaskId);
            if (processTaskFormVo != null) {
                ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
                formConfig = selectContentByHashCrossoverMapper.getProcessTaskFromContentByHash(processTaskFormVo.getFormContentHash());
                IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
                List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(processTaskId);
                if (CollectionUtils.isNotEmpty(processTaskFormAttributeDataList)) {
                    for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                        Object dataObj = formAttributeDataAdaptsToCmdb(attributeDataVo.getAttributeUuid(), attributeDataVo.getDataObj(), formConfig);
                        if (dataObj == null) {
                            continue;
                        }
                        formAttributeDataMap.put(attributeDataVo.getAttributeUuid(), dataObj);
                    }
                }
            }
            // 遍历configList，将“批量操作”的配置信息根据表单数据转换成多条“单个操作”配置信息
//            List<CiEntitySyncConfigVo> ciEntitySyncConfigList = new ArrayList<>();
//            ciEntitySyncConfigList.add(startConfigObj);
//            List<String> ciEntityUuidList = new ArrayList<>();
//            JSONArray children = startConfigObj.getChildren();
//            if (CollectionUtils.isNotEmpty(children)) {
//                for (int i = 0; i < children.size(); i++) {
//                    JSONObject child = children.getJSONObject(i);
//                    String ciEntityUuid = child.getString("ciEntityUuid");
//                    if (StringUtils.isNotBlank(ciEntityUuid)) {
//                        ciEntityUuidList.add(ciEntityUuid);
//                    }
//                }
//            }
//            for (CiEntitySyncConfigVo ciEntitySyncConfig : originalConfigList) {
//                if (ciEntityUuidList.contains(ciEntitySyncConfig.getUuid())) {
//                    ciEntitySyncConfigList.add(ciEntitySyncConfig);
//                }
//            }
            handleBatchDataSource(originalConfigList, startConfigObj, formAttributeDataMap, newConfigList, formConfig);
        }
        return newConfigList;
    }

    private List<CiEntityTransactionVo> createCiEntityTransactionVo(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            CiEntitySyncConfigVo mainConfigObj,
            Map<String, CiEntitySyncConfigVo> dependencyConfigMap) {
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        Map<String, CiEntitySyncMappingVo> mappingMap = new HashMap<>();
        List<CiEntitySyncMappingVo> mappingList = mainConfigObj.getMappingList();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
            String key = mappingObj.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            mappingMap.put(key, mappingObj);
        }
        String uuid = mainConfigObj.getUuid();
        CiEntityTransactionVo ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
        /** 变更说明 **/
        CiEntitySyncMappingVo descriptionMappingObj = mappingMap.get("description");
        if (descriptionMappingObj != null) {
            JSONArray valueList = descriptionMappingObj.getValueList();
            if (CollectionUtils.isNotEmpty(valueList)) {
                String description = valueList.getString(0);
                if (StringUtils.isNotBlank(description)) {
                    ciEntityTransactionVo.setDescription(description);
                }
            }
        }

        /** 属性 **/
        JSONObject attrEntityData = buildAttrEntityData(ciEntityTransactionMap, mainConfigObj, mappingMap);
        ciEntityTransactionVo.setAttrEntityData(attrEntityData);
        /** 根据唯一属性列表查询配置项id **/
        Long ciEntityId = getCiEntityIdByUniqueAttrIdList(ciEntityTransactionMap, mainConfigObj);
        if (ciEntityId != null) {
            ciEntityTransactionVo.setCiEntityId(ciEntityId);
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        } else {
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        }
        /** 关系 **/
        JSONObject relEntityData = buildRelEntityData(ciEntityTransactionMap, mainConfigObj, dependencyConfigMap, mappingMap, ciEntityTransactionList);
        ciEntityTransactionVo.setRelEntityData(relEntityData);
        /** 全局属性 **/
        JSONObject globalAttrEntityData = buildGlobalAttrEntityData(mainConfigObj, mappingMap);
        ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrEntityData);

        ciEntityTransactionList.add(ciEntityTransactionVo);
        return ciEntityTransactionList;
    }

    /**
     * 组装属性
     *
     * @param mainConfigObj
     * @param mappingMap
     * @return
     */
    private JSONObject buildAttrEntityData(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            CiEntitySyncConfigVo mainConfigObj,
            Map<String, CiEntitySyncMappingVo> mappingMap
    ) {
        JSONObject attrEntityData = new JSONObject();
        Long ciId = mainConfigObj.getCiId();
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        for (AttrVo attrVo : attrList) {
            if (Objects.equals(attrVo.getType(), "expression")) {
                continue;
            }
            String key = "attr_" + attrVo.getId();
            CiEntitySyncMappingVo mappingObj = mappingMap.get(key);
            if (Objects.equals(mainConfigObj.getEditMode(), EditModeType.PARTIAL.getValue())) {
                if (mappingObj == null || CollectionUtils.isEmpty(mappingObj.getValueList())) {
                    continue;
                }
            }
            JSONObject attrEntity = new JSONObject();
            attrEntity.put("type", attrVo.getType());
            attrEntity.put("config", attrVo.getConfig());
            // 对于设置必填的自动采集字段，saveMode设置为允许为空，不校验必填
            if (!Objects.equals(mainConfigObj.getEditMode(), EditModeType.PARTIAL.getValue())
                    && attrVo.getIsRequired().equals(1)
                    && Objects.equals(attrVo.getInputType(), InputType.AT.getValue())) {
                attrEntity.put("saveMode", SaveModeType.ALLOW_EMPTY.getValue());
            }
            if (mappingObj == null || CollectionUtils.isEmpty(mappingObj.getValueList())) {
                attrEntity.put("valueList", new JSONArray());
            } else {
                if (Objects.equals(attrVo.getType(), PropHandlerType.SELECT.getValue()) || Objects.equals(attrVo.getType(), PropHandlerType.TABLE.getValue())) {
                    JSONArray valueList = validSelectAndTableAttrValueList(ciEntityTransactionMap, attrVo, mappingObj.getValueList());
                    attrEntity.put("valueList", valueList);
                } else {
                    attrEntity.put("valueList", mappingObj.getValueList());
                }
            }
            attrEntityData.put(key, attrEntity);
        }
        return attrEntityData;
    }

    /**
     * 根据唯一属性列表查询配置项id
     * @param ciEntityTransactionMap
     * @param mainConfigObj
     * @return
     */
    private Long getCiEntityIdByUniqueAttrIdList(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            CiEntitySyncConfigVo mainConfigObj) {
        Long ciId = mainConfigObj.getCiId();
        CiVo ciVo = ciMapper.getCiById(ciId);
        List<Long> uniqueAttrIdList = ciVo.getUniqueAttrIdList();
        if (CollectionUtils.isEmpty(uniqueAttrIdList)) {
            return null;
        }
        CiEntityTransactionVo ciEntityTransactionVo = ciEntityTransactionMap.get(mainConfigObj.getUuid());
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
            List<String> valueList = new ArrayList<>();
            AttrEntityTransactionVo attrEntityTransaction = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrId);
            if (attrEntityTransaction != null) {
                JSONArray valueArray = attrEntityTransaction.getValueList();
                if (CollectionUtils.isNotEmpty(valueArray)) {
                    valueList = valueArray.toJavaList(String.class);
                }
            }
            if (CollectionUtils.isEmpty(valueList)) {
                AttrVo attr = attrMapper.getAttrById(attrId);
                if (attr == null) {
                    throw new AttrNotFoundException(ciVo.getName(), attrId.toString());
                }
                attr.setCiName(ciVo.getName());
                attr.setCiLabel(ciVo.getLabel());
                throw new CiUniqueAttrNotFoundException(attr);
            }
            AttrFilterVo filterVo = new AttrFilterVo();
            filterVo.setAttrId(attrId);
            filterVo.setExpression(SearchExpression.EQ.getExpression());
            filterVo.setValueList(valueList);
            ciEntityConditionVo.addAttrFilter(filterVo);
        }
        if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
            List<CiEntityVo> checkList = ciEntityService.searchCiEntity(ciEntityConditionVo);
            if (CollectionUtils.isNotEmpty(checkList)) {
                return checkList.get(0).getId();
            }
        }
        return null;
    }
    /**
     * 组装关系
     *
     * @param ciEntityTransactionMap
     * @param mainConfigObj
     * @param dependencyConfigMap
     * @param mappingMap
     * @param ciEntityTransactionList
     * @return
     */
    private JSONObject buildRelEntityData(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            CiEntitySyncConfigVo mainConfigObj,
            Map<String, CiEntitySyncConfigVo> dependencyConfigMap,
            Map<String, CiEntitySyncMappingVo> mappingMap,
            List<CiEntityTransactionVo> ciEntityTransactionList
    ) {
        JSONObject relEntityData = new JSONObject();
        Long ciId = mainConfigObj.getCiId();
        String uuid = mainConfigObj.getUuid();
        CiEntityTransactionVo ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        for (RelVo relVo : relList) {
            String key = "rel" + relVo.getDirection() + "_" + relVo.getId();
            CiEntitySyncMappingVo mappingObj = mappingMap.get(key);
            if (mappingObj == null) {
                continue;
            }
            String mappingMode = mappingObj.getMappingMode();
            if (Objects.equals(mappingMode, "new")) {// 表示关系
                List<JSONObject> alreadyExistRelList = new ArrayList<>();
                List<Long> ciEntityIdList = new ArrayList<>();
                if (Objects.equals(mainConfigObj.getEditMode(), EditModeType.PARTIAL.getValue())
                        && !Objects.equals(mainConfigObj.getAction(), "replace")) {
                    List<RelEntityVo> relEntityList;
                    if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        relEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(ciEntityTransactionVo.getCiEntityId(), relVo.getId(), null);
                        for (RelEntityVo relEntityVo : relEntityList) {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("ciEntityId", relEntityVo.getToCiEntityId());
                            jsonObj.put("ciEntityName", relEntityVo.getToCiEntityName());
                            jsonObj.put("ciId", relEntityVo.getToCiId());
                            alreadyExistRelList.add(jsonObj);
                        }
                    } else {
                        relEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(ciEntityTransactionVo.getCiEntityId(), relVo.getId(), null);
                        for (RelEntityVo relEntityVo : relEntityList) {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("ciEntityId", relEntityVo.getFromCiEntityId());
                            jsonObj.put("ciEntityName", relEntityVo.getFromCiEntityName());
                            jsonObj.put("ciId", relEntityVo.getFromCiId());
                            alreadyExistRelList.add(jsonObj);
                        }
                    }
                }
                JSONArray valueList = mappingObj.getValueList();
                if (CollectionUtils.isNotEmpty(valueList)) {
                    for (int i = 0; i < valueList.size(); i++) {
                        JSONObject valueObj = valueList.getJSONObject(i);
                        if (MapUtils.isEmpty(valueObj)) {
                            continue;
                        }
                        String ciEntityUuid = valueObj.getString("ciEntityUuid");
                        if (StringUtils.isBlank(ciEntityUuid)) {
                            continue;
                        }
                        String type = valueObj.getString("type");
                        if (Objects.equals(type, "new")) {// 新配置信息
                            CiEntitySyncConfigVo dependencyConfig = dependencyConfigMap.get(ciEntityUuid);
                            if (dependencyConfig == null) {
                                continue;
                            }
                            List<CiEntityTransactionVo> list = createCiEntityTransactionVo(ciEntityTransactionMap, dependencyConfig, dependencyConfigMap);
                            ciEntityTransactionList.addAll(list);
                            // 关系选择追加模式时，需要将该配置项原来就关联的关系数据补充到valueList中
                            if (Objects.equals(dependencyConfig.getAction(), "append") && ciEntityTransactionVo.getCiEntityId() != null) {
                                valueObj.put("action", RelActionType.INSERT.getValue());
                            } else {
                                valueObj.put("action", RelActionType.REPLACE.getValue());
                            }
                        }

                        Long ciEntityId = valueObj.getLong("ciEntityId");
                        if (ciEntityId == null) {
                            CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(ciEntityUuid);
                            if (tmpVo != null) {
                                ciEntityId = tmpVo.getCiEntityId();
                                valueObj.put("ciEntityId", ciEntityId);
                            }
                        }
                        ciEntityIdList.add(ciEntityId);
                    }
                } else {
                    valueList = new JSONArray();
                }

                // 关系选择追加模式时，需要将该配置项原来就关联的关系数据补充到valueList中
                for (JSONObject jsonObj : alreadyExistRelList) {
                    if (ciEntityIdList.contains(jsonObj.getLong("ciEntityId"))) {
                        continue;
                    }
                    valueList.add(jsonObj);
                }
                if (CollectionUtils.isNotEmpty(valueList)) {
                    JSONObject relEntity = new JSONObject();
                    relEntity.put("valueList", valueList);
                    relEntityData.put(key, relEntity);
                }
            }
        }
        return relEntityData;
    }

    /**
     * 组装全局属性
     *
     * @param mappingMap
     * @return
     */
    private JSONObject buildGlobalAttrEntityData(CiEntitySyncConfigVo mainConfigObj, Map<String, CiEntitySyncMappingVo> mappingMap) {
        JSONObject globalAttrEntityData = new JSONObject();
        GlobalAttrVo searchVo = new GlobalAttrVo();
        searchVo.setIsActive(1);
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(searchVo);
        for (GlobalAttrVo globalAttrVo : globalAttrList) {
            String key = "global_" + globalAttrVo.getId();
            CiEntitySyncMappingVo mappingObj = mappingMap.get(key);
            if (mappingObj == null) {
                if (Objects.equals(mainConfigObj.getEditMode(), EditModeType.GLOBAL.getValue())) {
                    JSONObject globalAttrEntity = new JSONObject();
                    globalAttrEntity.put("valueList", new JSONArray());
                    globalAttrEntityData.put(key, globalAttrEntity);
                }
                continue;
            }
            JSONArray valueList = new JSONArray();
            // 映射模式为常量
            JSONArray valueArray = mappingObj.getValueList();
            if (CollectionUtils.isNotEmpty(valueArray)) {
                for (int i = 0; i < valueArray.size(); i++) {
                    Object valueObj = valueArray.get(i);
                    if (valueObj instanceof JSONObject) {
                        valueList.add(valueObj);
                    } else {
                        GlobalAttrItemVo globalAttrItemVo = new GlobalAttrItemVo();
                        globalAttrItemVo.setAttrId(globalAttrVo.getId());
                        List<GlobalAttrItemVo> itemList = globalAttrMapper.searchGlobalAttrItem(globalAttrItemVo);
                        for (GlobalAttrItemVo item : itemList) {
                            if (Objects.equals(item.getValue(), valueObj)
                                    || Objects.equals(item.getId().toString(), valueObj.toString())) {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("id", item.getId());
                                jsonObj.put("value", item.getValue());
                                jsonObj.put("sort", item.getSort());
                                jsonObj.put("attrId", globalAttrVo.getId());
                                valueList.add(jsonObj);
                            }
                        }
                    }
                }
            }
            if (CollectionUtils.isEmpty(valueList)) {
                if (Objects.equals(mainConfigObj.getEditMode(), EditModeType.PARTIAL.getValue())) {
                    continue;
                }
            }
            JSONObject globalAttrEntity = new JSONObject();
            globalAttrEntity.put("valueList", valueList);
            globalAttrEntityData.put(key, globalAttrEntity);
        }
        return globalAttrEntityData;
    }

    /**
     * 处理和校验下拉框类型和表格类型属性的值
     *
     * @param attrVo
     * @param valueList
     */
    private JSONArray validSelectAndTableAttrValueList(
            Map<String, CiEntityTransactionVo> ciEntityTransactionMap,
            AttrVo attrVo,
            JSONArray valueList
    ) {
        IAttrValueHandler attrHandler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        if (attrHandler == null) {
            throw new AttrTypeNotFoundException(attrVo.getType());
        }
        if (CollectionUtils.isEmpty(valueList)) {
            return valueList;
        }
        List<Long> longValueList = new ArrayList<>();
        List<String> stringValueList = new ArrayList<>();
        JSONArray tempList = new JSONArray();
        tempList.addAll(valueList);
        for (int i = 0; i < tempList.size(); i++) {
            Object valueObj = tempList.get(i);
            if (valueObj instanceof Long) {
                longValueList.add((Long) valueObj);
            } else if (valueObj instanceof String) {
                String valueStr = valueObj.toString();
                try {
                    Integer.valueOf(valueStr);
                    stringValueList.add(valueStr);
                } catch (NumberFormatException e1) {
                    try {
                        longValueList.add(Long.valueOf(valueStr));
                    } catch (NumberFormatException e2) {
                        stringValueList.add(valueStr);
                    }
                }
            }
        }
        JSONArray newValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(longValueList)) {
            JSONArray array = new JSONArray();
            array.addAll(longValueList);
            attrHandler.valid(attrVo, array);
            newValueList.addAll(longValueList);
        }
        if (CollectionUtils.isNotEmpty(stringValueList)) {
            CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
            if (ciVo == null) {
                throw new CiNotFoundException(attrVo.getTargetCiId());
            }
            for (String valueStr : stringValueList) {
                CiEntityVo search = new CiEntityVo();
                search.setName(valueStr);
                if (ciVo.getIsVirtual().equals(0)) {
                    // 非虚拟模型
                    List<CiVo> downwardCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    Map<Long, CiVo> downwardCiMap = downwardCiList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                    boolean isFind = false;
                    for (Map.Entry<String, CiEntityTransactionVo> entry : ciEntityTransactionMap.entrySet()) {
                        CiEntityTransactionVo ciEntityTransactionVo = entry.getValue();
                        CiVo downwardCi = downwardCiMap.get(ciEntityTransactionVo.getCiId());
                        if (downwardCi == null) {
                            continue;
                        }
                        if (downwardCi.getNameAttrId() == null) {
                            continue;
                        }
                        AttrEntityTransactionVo attrEntityTransaction = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(downwardCi.getNameAttrId());
                        if (attrEntityTransaction == null) {
                            continue;
                        }
                        if (CollectionUtils.isEmpty(attrEntityTransaction.getValueList())) {
                            continue;
                        }
                        for (Object value : attrEntityTransaction.getValueList()) {
                            if (Objects.equals(value, valueStr)) {
                                newValueList.add(ciEntityTransactionVo.getCiEntityId());//
                                isFind = true;
                                break;
                            }
                        }
                    }
                    if (!isFind) {
                        search.setIdList(new ArrayList<>(downwardCiMap.keySet()));
                        List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityListByCiIdListAndName(search);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            throw new AttrValueIrregularException(attrVo, valueStr);
                        }
                        for (CiEntityVo ciEntity : ciEntityList) {
                            newValueList.add(ciEntity.getId());
                        }
                    }
                } else {
                    // 虚拟模型
                    search.setCiId(ciVo.getId());
                    List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(search);
                    if (CollectionUtils.isEmpty(ciEntityList)) {
                        throw new AttrValueIrregularException(attrVo, valueStr);
                    }
                    for (CiEntityVo ciEntity : ciEntityList) {
                        newValueList.add(ciEntity.getId());
                    }
                }
            }
        }
        return newValueList;
    }

    /**
     * 遍历configList，将“批量操作”的配置信息根据表单数据转换成多条“单个操作”配置信息
     * @param originalConfigList  原始配置信息列表
     * @param currentConfigList 当前要处理的配置信息列表
     * @param formAttributeDataMap 表单数据信息
     * @param newConfigList 收集新产生的配置信息列表
     * @param formConfig 表单配置信息
     * @return
     */
    private JSONArray handleBatchDataSource(
            List<CiEntitySyncConfigVo> originalConfigList,
            List<CiEntitySyncConfigVo> currentConfigList,
            Map<String, Object> formAttributeDataMap,
            List<CiEntitySyncConfigVo> newConfigList,
            String formConfig
    ) {
        CiEntitySyncConfigVo currentConfig = currentConfigList.get(0);
        Long ciId = currentConfig.getCiId();
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            String ciName = currentConfig.getCiName();
            throw new CiNotFoundException(ciName);
        }
        JSONArray resultList = new JSONArray();
        List<String> batchDataColumnList = new ArrayList<>();
        JSONArray batchDataList = new JSONArray();
        if (Objects.equals(currentConfig.getCreatePolicy(), "batch")) {
            // 批量操作配置信息
            CiEntitySyncBatchDataSourceVo batchDataSource = currentConfig.getBatchDataSource();
            String attributeUuid = batchDataSource.getAttributeUuid();
            Object dataObj = formAttributeDataMap.get(attributeUuid);
            if (dataObj == null) {
                return resultList;
            }
            if (dataObj instanceof JSONArray) {
                JSONArray dataList = (JSONArray) dataObj;
                List<CiEntitySyncFilterVo> filterList = batchDataSource.getFilterList();
                batchDataList = filterData(dataList, filterList, formConfig);
                if (CollectionUtils.isEmpty(batchDataList)) {
                    return resultList;
                }
                JSONObject rowDataObj = batchDataList.getJSONObject(0);
                batchDataColumnList.addAll(rowDataObj.keySet());
            } else {
                return resultList;
            }
        }
        Map<String, CiEntitySyncConfigVo> targetCiConfigMap = new HashMap<>();
        for (int i = 1; i < currentConfigList.size(); i++) {
            CiEntitySyncConfigVo ciEntitySyncConfig = currentConfigList.get(i);
            targetCiConfigMap.put(ciEntitySyncConfig.getCiId().toString(), ciEntitySyncConfig);
            targetCiConfigMap.put(ciEntitySyncConfig.getCiName(), ciEntitySyncConfig);
            targetCiConfigMap.put(ciEntitySyncConfig.getCiLabel(), ciEntitySyncConfig);
        }
        List<CiEntitySyncMappingVo> mappingList = handleMappingFormComponent(currentConfig.getMappingList(), batchDataColumnList, formAttributeDataMap);
        if (Objects.equals(currentConfig.getCreatePolicy(), "batch")) {
            if (CollectionUtils.isNotEmpty(batchDataList)) {
                // 遍历批量操作表格数据
                for (int j = 0; j < batchDataList.size(); j++) {
                    JSONObject rowDataObj = batchDataList.getJSONObject(j);
                    if (MapUtils.isEmpty(rowDataObj)) {
                        continue;
                    }
                    List<CiEntitySyncMappingVo> newMappingList = new ArrayList<>();
                    newMappingList.addAll(mappingList);
                    List<CiEntitySyncMappingVo> list = handleMappingFormTableComponent(rowDataObj, batchDataColumnList, currentConfig.getMappingList());
                    newMappingList.addAll(list);
                    CiEntitySyncConfigVo newConfigObj = createCiEntitySyncConfigVo(ciVo, currentConfig, newMappingList, batchDataColumnList, formAttributeDataMap, targetCiConfigMap, rowDataObj);
                    JSONObject resultObj = new JSONObject();
                    resultObj.put("ciEntityUuid", newConfigObj.getUuid());
                    resultObj.put("ciEntityName", newConfigObj.getCiName());
                    resultObj.put("ciId", newConfigObj.getCiId());
                    resultObj.put("type", "new");
                    resultList.add(resultObj);
                    newConfigList.add(newConfigObj);
                    formAttributeDataMap.putAll(rowDataObj);
                    Map<String, Object> newFormAttributeDataMap = new HashMap<>();
                    newFormAttributeDataMap.putAll(formAttributeDataMap);
                    newFormAttributeDataMap.putAll(rowDataObj);
                    handleMappingRelConfig(originalConfigList, newConfigObj, newFormAttributeDataMap, newConfigList, formConfig);
                }
            }
        } else {
            CiEntitySyncConfigVo newConfigObj = createCiEntitySyncConfigVo(ciVo, currentConfig, mappingList,  batchDataColumnList, formAttributeDataMap, targetCiConfigMap, null);
            JSONObject resultObj = new JSONObject();
            resultObj.put("ciEntityUuid", newConfigObj.getUuid());
            resultObj.put("ciEntityName", newConfigObj.getCiName());
            resultObj.put("ciId", newConfigObj.getCiId());
            resultObj.put("type", "new");
            resultList.add(resultObj);
            newConfigList.add(newConfigObj);
            handleMappingRelConfig(originalConfigList, newConfigObj, formAttributeDataMap, newConfigList, formConfig);
        }
        return resultList;
    }

    /**
     * 遍历configList，将“批量操作”的配置信息根据表单数据转换成多条“单个操作”配置信息
     * @param originalConfigList  原始配置信息列表
     * @param currentConfig 当前要处理的配置信息
     * @param formAttributeDataMap 表单数据信息
     * @param newConfigList 收集新产生的配置信息列表
     * @param formConfig 表单配置信息
     * @return
     */
    private JSONArray handleBatchDataSource(
            List<CiEntitySyncConfigVo> originalConfigList,
            CiEntitySyncConfigVo currentConfig,
            Map<String, Object> formAttributeDataMap,
            List<CiEntitySyncConfigVo> newConfigList,
            String formConfig
    ) {
        Long ciId = currentConfig.getCiId();
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            String ciName = currentConfig.getCiName();
            throw new CiNotFoundException(ciName);
        }
        JSONArray resultList = new JSONArray();
        List<String> batchDataColumnList = new ArrayList<>();
        JSONArray batchDataList = new JSONArray();
        if (Objects.equals(currentConfig.getCreatePolicy(), "batch")) {
            // 批量操作配置信息
            CiEntitySyncBatchDataSourceVo batchDataSource = currentConfig.getBatchDataSource();
            String attributeUuid = batchDataSource.getAttributeUuid();
            Object dataObj = formAttributeDataMap.get(attributeUuid);
            if (dataObj == null) {
                return resultList;
            }
            if (dataObj instanceof JSONArray) {
                JSONArray dataList = (JSONArray) dataObj;
                List<CiEntitySyncFilterVo> filterList = batchDataSource.getFilterList();
                batchDataList = filterData(dataList, filterList, formConfig);
                if (CollectionUtils.isEmpty(batchDataList)) {
                    return resultList;
                }
                JSONObject rowDataObj = batchDataList.getJSONObject(0);
                batchDataColumnList.addAll(rowDataObj.keySet());
            } else {
                return resultList;
            }
        }
        List<CiEntitySyncMappingVo> mappingList = handleMappingFormComponent(currentConfig.getMappingList(), batchDataColumnList, formAttributeDataMap);
        if (Objects.equals(currentConfig.getCreatePolicy(), "batch")) {
            if (CollectionUtils.isNotEmpty(batchDataList)) {
                // 遍历批量操作表格数据
                for (int j = 0; j < batchDataList.size(); j++) {
                    JSONObject rowDataObj = batchDataList.getJSONObject(j);
                    if (MapUtils.isEmpty(rowDataObj)) {
                        continue;
                    }
                    List<CiEntitySyncMappingVo> newMappingList = new ArrayList<>();
                    newMappingList.addAll(mappingList);
                    List<CiEntitySyncMappingVo> list = handleMappingFormTableComponent(rowDataObj, batchDataColumnList, currentConfig.getMappingList());
                    newMappingList.addAll(list);
                    CiEntitySyncConfigVo newConfigObj = createCiEntitySyncConfigVo(ciVo, currentConfig, newMappingList);
                    JSONObject resultObj = new JSONObject();
                    resultObj.put("ciEntityUuid", newConfigObj.getUuid());
                    resultObj.put("ciEntityName", newConfigObj.getCiName());
                    resultObj.put("ciId", newConfigObj.getCiId());
                    resultObj.put("type", "new");
                    resultList.add(resultObj);
                    newConfigList.add(newConfigObj);
                    formAttributeDataMap.putAll(rowDataObj);
                    Map<String, Object> newFormAttributeDataMap = new HashMap<>();
                    newFormAttributeDataMap.putAll(formAttributeDataMap);
                    newFormAttributeDataMap.putAll(rowDataObj);
                    handleMappingRelConfig(originalConfigList, newConfigObj, newFormAttributeDataMap, newConfigList, formConfig);
                }
            }
        } else {
            CiEntitySyncConfigVo newConfigObj = createCiEntitySyncConfigVo(ciVo, currentConfig, mappingList);
            JSONObject resultObj = new JSONObject();
            resultObj.put("ciEntityUuid", newConfigObj.getUuid());
            resultObj.put("ciEntityName", newConfigObj.getCiName());
            resultObj.put("ciId", newConfigObj.getCiId());
            resultObj.put("type", "new");
            resultList.add(resultObj);
            newConfigList.add(newConfigObj);
            handleMappingRelConfig(originalConfigList, newConfigObj, formAttributeDataMap, newConfigList, formConfig);
        }
        return resultList;
    }

    /**
     * 处理关系的新的配置项数据
     * @param originalConfigList 原始配置信息列表
     * @param currentConfig 当前要处理的配置信息
     * @param formAttributeDataMap 表单数据信息
     * @param newConfigList 收集新产生的配置信息列表
     * @param formConfig 表单配置信息
     */
    private void oldHandleMappingRelConfig(
            List<CiEntitySyncConfigVo> originalConfigList,
            CiEntitySyncConfigVo currentConfig,
            Map<String, Object> formAttributeDataMap,
            List<CiEntitySyncConfigVo> newConfigList,
            String formConfig
    ) {
        List<CiEntitySyncMappingVo> mappingList = currentConfig.getMappingList();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
            String mappingMode = mappingObj.getMappingMode();
            if (!Objects.equals(mappingMode, "new")) {
                continue;
            }
            JSONArray valueList = mappingObj.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                continue;
            }
            JSONArray newValueList = new JSONArray();
            boolean isFrom = false;
            List<String> ciEntityUuidList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                JSONObject valueObj = valueList.getJSONObject(i);
                if (MapUtils.isEmpty(valueObj)) {
                    continue;
                }
                String ciEntityUuid = valueObj.getString("ciEntityUuid");
                if (StringUtils.isBlank(ciEntityUuid)) {
                    continue;
                }
                ciEntityUuidList.add(ciEntityUuid);
                String type = valueObj.getString("type");
                if (Objects.equals(type, "from")) {
                    JSONObject newValueObj = new JSONObject();
                    newValueObj.putAll(valueObj);
                    newValueList.add(newValueObj);
                    isFrom = true;
                    break;
                }
            }
            if (!isFrom) {
                List<CiEntitySyncConfigVo> relConfigObjList = new ArrayList<>();
                for (CiEntitySyncConfigVo originalConfig : originalConfigList) {
                    if (ciEntityUuidList.contains(originalConfig.getUuid())) {
                        replaceMappingRelFromCiEntityUuid(originalConfig, currentConfig.getUuid());
                        relConfigObjList.add(originalConfig);
                    }
                }
                if (CollectionUtils.isNotEmpty(relConfigObjList)) {
                    newValueList = handleBatchDataSource(originalConfigList, relConfigObjList, formAttributeDataMap, newConfigList, formConfig);
                } else {
                    logger.warn("mappingObj = " + JSONObject.toJSONString(mappingObj));
                }
            }
            mappingObj.setValueList(newValueList);
        }
    }

    /**
     * 处理关系的新的配置项数据
     * @param originalConfigList 原始配置信息列表
     * @param currentConfig 当前要处理的配置信息
     * @param formAttributeDataMap 表单数据信息
     * @param newConfigList 收集新产生的配置信息列表
     * @param formConfig 表单配置信息
     */
    private void handleMappingRelConfig(
            List<CiEntitySyncConfigVo> originalConfigList,
            CiEntitySyncConfigVo currentConfig,
            Map<String, Object> formAttributeDataMap,
            List<CiEntitySyncConfigVo> newConfigList,
            String formConfig
    ) {
        List<CiEntitySyncMappingVo> mappingList = currentConfig.getMappingList();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
            String mappingMode = mappingObj.getMappingMode();
            if (!Objects.equals(mappingMode, "new")) {
                continue;
            }
            JSONArray valueList = mappingObj.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                continue;
            }
            JSONArray newValueList = new JSONArray();
            boolean isFrom = false;
            List<String> ciEntityUuidList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                JSONObject valueObj = valueList.getJSONObject(i);
                if (MapUtils.isEmpty(valueObj)) {
                    continue;
                }
                String ciEntityUuid = valueObj.getString("ciEntityUuid");
                if (StringUtils.isBlank(ciEntityUuid)) {
                    continue;
                }
                ciEntityUuidList.add(ciEntityUuid);
                String type = valueObj.getString("type");
                if (Objects.equals(type, "from")) {
                    JSONObject newValueObj = new JSONObject();
                    newValueObj.putAll(valueObj);
                    newValueList.add(newValueObj);
                    isFrom = true;
                    break;
                }
            }
            if (!isFrom) {
                for (CiEntitySyncConfigVo originalConfig : originalConfigList) {
                    if (ciEntityUuidList.contains(originalConfig.getUuid())) {
                        replaceMappingRelFromCiEntityUuid(originalConfig, currentConfig.getUuid());
                        JSONArray list = handleBatchDataSource(originalConfigList, originalConfig, formAttributeDataMap, newConfigList, formConfig);
                        newValueList.addAll(list);
                    }
                }
            }
            mappingObj.setValueList(newValueList);
        }
    }

    /**
     * 替换关系的配置信息中的来源配置项的ciEntityUuid
     * @param relConfigObj
     * @param fromCiEntityUuid
     */
    private void replaceMappingRelFromCiEntityUuid(CiEntitySyncConfigVo relConfigObj, String fromCiEntityUuid) {
        List<CiEntitySyncMappingVo> mappingList = relConfigObj.getMappingList();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
            String mappingMode = mappingObj.getMappingMode();
            if (!Objects.equals(mappingMode, "new")) {
                continue;
            }
            JSONArray valueList = mappingObj.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                continue;
            }
            for (int i = 0; i < valueList.size(); i++) {
                JSONObject valueObj = valueList.getJSONObject(i);
                if (MapUtils.isEmpty(valueObj)) {
                    continue;
                }
                String ciEntityUuid = valueObj.getString("ciEntityUuid");
                if (StringUtils.isBlank(ciEntityUuid)) {
                    continue;
                }
                String type = valueObj.getString("type");
                if (Objects.equals(type, "from")) {
                    valueObj.put("ciEntityUuid", fromCiEntityUuid);
                }
            }
        }
    }

    /**
     * 遍历configList，将mappingList中映射模式为“表单普通组件”和“表单表格组件”的数据替换称表单组件对应的真实值
     * @param mappingList 配置信息
     * @param batchDataColumnList 遍历对象属性列表
     * @param formAttributeDataMap 表单数据信息
     */
    private List<CiEntitySyncMappingVo> handleMappingFormComponent(
            List<CiEntitySyncMappingVo> mappingList,
            List<String> batchDataColumnList,
            Map<String, Object> formAttributeDataMap) {

        List<CiEntitySyncMappingVo> newMappingList = new ArrayList<>();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
            JSONArray valueList = mappingObj.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                continue;
            }
            JSONArray newValueList = new JSONArray();
            String mappingMode = mappingObj.getMappingMode();
            if (Objects.equals(mappingMode, "new")) {
                continue;
            }
            if (Objects.equals(mappingMode, "formTableComponent")
                    || Objects.equals(mappingMode, "formSubassemblyComponent")) {
                String attributeUuid = valueList.getString(valueList.size() -1);
                if (batchDataColumnList.contains(attributeUuid)) {
//                    newValueList.addAll(valueList);
                    continue;
                }
                Object dataObj = formAttributeDataMap.get(attributeUuid);
                if (dataObj == null) {
                    continue;
                }
                if (dataObj instanceof JSONArray) {
                    newValueList.addAll((JSONArray) dataObj);
                } else {
                    newValueList.add(dataObj);
                }
            } else if (Objects.equals(mappingMode, "formCommonComponent")) {
                Object dataObj = formAttributeDataMap.get(valueList.getString(0));
                if (dataObj == null) {
                    continue;
                }
                if (dataObj instanceof JSONArray) {
                    newValueList.addAll((JSONArray) dataObj);
                } else {
                    newValueList.add(dataObj);
                }
            }
            for (int i = newValueList.size() - 1; i >= 0; i--) {
                if (StringUtils.isBlank(newValueList.getString(i))){
                    newValueList.remove(i);
                }
            }
            if (CollectionUtils.isNotEmpty(newValueList)) {
                CiEntitySyncMappingVo newMappingObj = new CiEntitySyncMappingVo(mappingObj);
                newMappingObj.setMappingMode("constant");
                newMappingObj.setValueList(newValueList);
                newMappingList.add(newMappingObj);
            }
        }
        return newMappingList;
    }

    private List<CiEntitySyncMappingVo> handleMappingFormTableComponent(
            JSONObject rowDataObj,
            List<String> batchDataColumnList,
            List<CiEntitySyncMappingVo> mappingList
    ) {
        List<CiEntitySyncMappingVo> newMappingList = new ArrayList<>();
        for (CiEntitySyncMappingVo mappingObj : mappingList) {
            JSONArray valueList = mappingObj.getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                continue;
            }
            String mappingMode = mappingObj.getMappingMode();
            if (Objects.equals(mappingMode, "formTableComponent") || Objects.equals(mappingMode, "formSubassemblyComponent")) {
                // 映射模式是表单表格组件
                String attributeUuid = valueList.getString(valueList.size() -1);
                if (!batchDataColumnList.contains(attributeUuid)) {
                    continue;
                }
                JSONArray newValueList = new JSONArray();
                Object valueObj = rowDataObj.get(attributeUuid);
                if (valueObj instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) valueObj;
                    newValueList.addAll(valueArray);
                } else {
                    newValueList.add(valueObj);
                }
                for (int i = newValueList.size() - 1; i >= 0; i--) {
                    if (StringUtils.isBlank(newValueList.getString(i))){
                        newValueList.remove(i);
                    }
                }
                if (CollectionUtils.isNotEmpty(newValueList)) {
                    CiEntitySyncMappingVo newMappingObj = new CiEntitySyncMappingVo(mappingObj);
                    newMappingObj.setMappingMode("constant");
                    newMappingObj.setValueList(newValueList);
                    newMappingList.add(newMappingObj);
                }
            }
        }
        return newMappingList;
    }

    private CiEntitySyncConfigVo createCiEntitySyncConfigVo(
            CiVo ciVo,
            CiEntitySyncConfigVo currentConfig,
            List<CiEntitySyncMappingVo> newMappingList,
            List<String> batchDataColumnList,
            Map<String, Object> formAttributeDataMap,
            Map<String, CiEntitySyncConfigVo> targetCiConfigMap,
            JSONObject rowDataObj) {
        CiEntitySyncConfigVo newConfigObj = new CiEntitySyncConfigVo();
        newConfigObj.setUuid(UuidUtil.randomUuid());
        if (Objects.equals(ciVo.getIsAbstract(), 1)) {
            Optional<CiEntitySyncMappingVo> targetCiIdMappingObj = newMappingList.stream().filter(e -> Objects.equals(e.getKey(), "targetCiId")).findFirst();
            if (!targetCiIdMappingObj.isPresent()) {
                throw new AbstractCiTargetCiIdAttrNotFoundException(ciVo);
            }
            JSONArray valueList = targetCiIdMappingObj.get().getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                throw new AbstractCiTargetCiIdAttrNotFoundException(ciVo);
            }
            String valueStr = valueList.getString(0);
            CiEntitySyncConfigVo targetCiConfig = targetCiConfigMap.get(valueStr);
            if (targetCiConfig == null) {
                CiVo targetCi = null;
                List<CiVo> downwardCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                for (CiVo downwardCi : downwardCiList) {
                    if (Objects.equals(downwardCi.getId().toString(), valueStr)) {
                        targetCi = downwardCi;
                    } else if (Objects.equals(downwardCi.getName(), valueStr)) {
                        targetCi = downwardCi;
                    } else if (Objects.equals(downwardCi.getLabel(), valueStr)) {
                        targetCi = downwardCi;
                    }
                }
                if (targetCi == null) {
                    throw new DownwardCiNotFoundException(ciVo, valueStr);
                } else if (Objects.equals(targetCi.getIsAbstract(), 1)) {
                    throw new CiIsAbstractedException(CiIsAbstractedException.Type.DATA, targetCi.getLabel() + "(" + targetCi.getName() + ")");
                }
                newConfigObj.setCiId(targetCi.getId());
                newConfigObj.setCiName(targetCi.getName());
                newConfigObj.setCiLabel(targetCi.getLabel());
                newConfigObj.setIsStart(currentConfig.getIsStart());
                newConfigObj.setCiIcon(targetCi.getIcon());
                newConfigObj.setEditMode(currentConfig.getEditMode());
            } else {
                List<CiEntitySyncMappingVo> mappingList = targetCiConfig.getMappingList();
                List<CiEntitySyncMappingVo> list = handleMappingFormComponent(mappingList, batchDataColumnList, formAttributeDataMap);
                newMappingList.addAll(list);
                if (rowDataObj != null) {
                    list = handleMappingFormTableComponent(rowDataObj, batchDataColumnList, targetCiConfig.getMappingList());
                    newMappingList.addAll(list);
                }
                for (CiEntitySyncMappingVo mapping : mappingList) {
                    if (Objects.equals(mapping.getMappingMode(), "new")) {
                        newMappingList.add(new CiEntitySyncMappingVo(mapping));
                    }
                }
                newConfigObj.setCiId(targetCiConfig.getCiId());
                newConfigObj.setCiName(targetCiConfig.getCiName());
                newConfigObj.setCiLabel(targetCiConfig.getCiLabel());
                newConfigObj.setIsStart(currentConfig.getIsStart());
                newConfigObj.setCiIcon(targetCiConfig.getCiIcon());
                newConfigObj.setEditMode(targetCiConfig.getEditMode());
            }
        } else {
            newConfigObj.setCiId(currentConfig.getCiId());
            newConfigObj.setCiName(currentConfig.getCiName());
            newConfigObj.setCiLabel(currentConfig.getCiLabel());
            newConfigObj.setIsStart(currentConfig.getIsStart());
            newConfigObj.setCiIcon(currentConfig.getCiIcon());
            newConfigObj.setEditMode(currentConfig.getEditMode());
        }
        List<CiEntitySyncMappingVo> mappingList = currentConfig.getMappingList();
        for (CiEntitySyncMappingVo mapping : mappingList) {
            if (Objects.equals(mapping.getMappingMode(), "new")) {
                newMappingList.add(new CiEntitySyncMappingVo(mapping));
            }
        }
        newConfigObj.setMappingList(newMappingList);
        return newConfigObj;
    }

    private CiEntitySyncConfigVo createCiEntitySyncConfigVo(
            CiVo ciVo,
            CiEntitySyncConfigVo currentConfig,
            List<CiEntitySyncMappingVo> newMappingList) {
        CiEntitySyncConfigVo newConfigObj = new CiEntitySyncConfigVo();
        newConfigObj.setUuid(UuidUtil.randomUuid());
        if (Objects.equals(ciVo.getIsAbstract(), 1)) {
            Optional<CiEntitySyncMappingVo> targetCiIdMappingObj = newMappingList.stream().filter(e -> Objects.equals(e.getKey(), "targetCiId")).findFirst();
            if (!targetCiIdMappingObj.isPresent()) {
                throw new AbstractCiTargetCiIdAttrNotFoundException(ciVo);
            }
            JSONArray valueList = targetCiIdMappingObj.get().getValueList();
            if (CollectionUtils.isEmpty(valueList)) {
                throw new AbstractCiTargetCiIdAttrNotFoundException(ciVo);
            }
            String valueStr = valueList.getString(0);
            CiVo targetCi = null;
            List<CiVo> downwardCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            for (CiVo downwardCi : downwardCiList) {
                if (Objects.equals(downwardCi.getId().toString(), valueStr)) {
                    targetCi = downwardCi;
                } else if (Objects.equals(downwardCi.getName(), valueStr)) {
                    targetCi = downwardCi;
                } else if (Objects.equals(downwardCi.getLabel(), valueStr)) {
                    targetCi = downwardCi;
                }
            }
            if (targetCi == null) {
                throw new DownwardCiNotFoundException(ciVo, valueStr);
            } else if (Objects.equals(targetCi.getIsAbstract(), 1)) {
                throw new CiIsAbstractedException(CiIsAbstractedException.Type.DATA, targetCi.getLabel() + "(" + targetCi.getName() + ")");
            }
            newConfigObj.setCiId(targetCi.getId());
            newConfigObj.setCiName(targetCi.getName());
            newConfigObj.setCiLabel(targetCi.getLabel());
            newConfigObj.setIsStart(currentConfig.getIsStart());
            newConfigObj.setCiIcon(targetCi.getIcon());
            newConfigObj.setEditMode(currentConfig.getEditMode());
            newConfigObj.setAction(currentConfig.getAction());
        } else {
            newConfigObj.setCiId(currentConfig.getCiId());
            newConfigObj.setCiName(currentConfig.getCiName());
            newConfigObj.setCiLabel(currentConfig.getCiLabel());
            newConfigObj.setIsStart(currentConfig.getIsStart());
            newConfigObj.setCiIcon(currentConfig.getCiIcon());
            newConfigObj.setEditMode(currentConfig.getEditMode());
            newConfigObj.setAction(currentConfig.getAction());
        }
        List<CiEntitySyncMappingVo> mappingList = currentConfig.getMappingList();
        for (CiEntitySyncMappingVo mapping : mappingList) {
            if (Objects.equals(mapping.getMappingMode(), "new")) {
                newMappingList.add(new CiEntitySyncMappingVo(mapping));
            }
        }
        newConfigObj.setMappingList(newMappingList);
        return newConfigObj;
    }

    /**
     * 表单数据适配CMDB数据
     * @param attributeUuid 表单属性uuid
     * @param originalValue 表单属性值
     * @param formConfig 表单配置信息
     * @return
     */
    private Object formAttributeDataAdaptsToCmdb(String attributeUuid, Object originalValue, String formConfig) {
        if (originalValue == null) {
            return null;
        }
        String handler = FormUtil.getFormAttributeHandler(attributeUuid, formConfig);
        if (handler == null) {
            return null;
        }
        if (Objects.equals(handler, FormHandler.FORMUPLOAD.getHandler())) {
            JSONArray resultList = new JSONArray();
            if (originalValue instanceof JSONArray) {
                JSONArray dataArray = (JSONArray) originalValue;
                for (int m = 0; m < dataArray.size(); m++) {
                    JSONObject data = dataArray.getJSONObject(m);
                    Long id = data.getLong("id");
                    if (id != null) {
                        resultList.add(id);
                    }
                }
            }
            return resultList;
        } else if (Objects.equals(handler, FormHandler.FORMRADIO.getHandler())
                || Objects.equals(handler, FormHandler.FORMCHECKBOX.getHandler())
                || Objects.equals(handler, FormHandler.FORMSELECT.getHandler())) {
            return FormUtil.getFormSelectAttributeValueByOriginalValue(originalValue);
        }
        return originalValue;
    }

    /**
     * 过滤数据
     * @param dataList 输入数据
     * @param filterList 过滤设置列表
     * @param formConfig 表单配置信息
     * @return
     */
    private JSONArray filterData(JSONArray dataList, List<CiEntitySyncFilterVo> filterList, String formConfig) {
        JSONArray resultList = new JSONArray();
        JSONArray tempList = new JSONArray();
        // 数据过滤
        if (CollectionUtils.isNotEmpty(filterList)) {
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject rowData = dataList.getJSONObject(i);
                if (MapUtils.isEmpty(rowData)) {
                    continue;
                }
                boolean flag = true;
                for (CiEntitySyncFilterVo filterObj : filterList) {
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
                    Object data = formAttributeDataAdaptsToCmdb(column, rowData.get(column), formConfig);
                    if (Objects.equals(expression, Expression.EQUAL.getExpression())) {
                        if (!Objects.equals(value, data)) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.UNEQUAL.getExpression())) {
                        if (Objects.equals(value, data)) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.LIKE.getExpression())) {
                        String columnValue = (String) data;
                        if (StringUtils.isBlank(columnValue)) {
                            flag = false;
                            break;
                        }
                        if (!columnValue.contains(value)) {
                            flag = false;
                            break;
                        }
                    } else if (Objects.equals(expression, Expression.NOTLIKE.getExpression())) {
                        String columnValue = (String) data;
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
                    tempList.add(rowData);
                }
            }
        } else {
            tempList = dataList;
        }

        for (int i = 0; i < tempList.size(); i++) {
            JSONObject newRowDataObj = new JSONObject();
            JSONObject rowDataObj = tempList.getJSONObject(i);
            for (Map.Entry<String, Object> entry : rowDataObj.entrySet()) {
                Object valueObj = formAttributeDataAdaptsToCmdb(entry.getKey(), entry.getValue(), formConfig);
                if (valueObj == null) {
                    continue;
                }
                newRowDataObj.put(entry.getKey(), valueObj);
            }
            if (MapUtils.isEmpty(newRowDataObj)) {
                continue;
            }
            resultList.add(newRowDataObj);
        }
        return resultList;
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
    @Override
    public boolean isHidden() {
        return true;
    }

}
