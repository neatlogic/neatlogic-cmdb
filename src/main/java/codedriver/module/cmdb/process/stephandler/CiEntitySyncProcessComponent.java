/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.process.stephandler;

import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.common.constvalue.InputFrom;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.process.constvalue.ProcessStepMode;
import codedriver.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskStepDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.exception.core.ProcessTaskException;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerBase;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CiEntitySyncProcessComponent extends ProcessStepHandlerBase {
    static Logger logger = LoggerFactory.getLogger(CiEntitySyncProcessComponent.class);

    @Autowired
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public String getHandler() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getHandler();
    }

    @Override
    public String getType() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getType();
    }

    @Override
    public ProcessStepMode getMode() {
        // FIXME 记得改回MT
        return ProcessStepMode.AT;
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
    public String getName() {
        return CmdbProcessStepHandlerType.CIENTITYSYNC.getName();
    }

    @Override
    public int getSort() {
        return 9;
    }

    @Override
    protected int myActive(ProcessTaskStepVo currentProcessTaskStepVo) {
        ProcessTaskStepVo processTaskStepVo =
                processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
        String stepConfig = selectContentByHashMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
        // 获取参数
        Map<Long, JSONArray> syncCiEntityMap = new HashMap<>();
        Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
        Map<Long, CiVo> ciMap = new HashMap<>();
        try {
            JSONObject stepConfigObj = JSONObject.parseObject(stepConfig);
            currentProcessTaskStepVo.setParamObj(stepConfigObj);
            if (MapUtils.isNotEmpty(stepConfigObj)) {
                JSONArray handlerList = stepConfigObj.getJSONArray("handlerList");
                if (CollectionUtils.isNotEmpty(handlerList)) {
                    for (int hIndex = 0; hIndex < handlerList.size(); hIndex++) {
                        ProcessTaskFormAttributeDataVo p = new ProcessTaskFormAttributeDataVo();
                        p.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                        p.setAttributeUuid(handlerList.getString(hIndex));
                        ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo =
                                processTaskMapper.getProcessTaskFormAttributeDataByProcessTaskIdAndAttributeUuid(p);
                        if (processTaskFormAttributeDataVo != null) {
                            JSONArray dataList = JSONArray.parseArray(processTaskFormAttributeDataVo.getData());
                            for (int dIndex = 0; dIndex < dataList.size(); dIndex++) {
                                JSONObject dataObj = dataList.getJSONObject(dIndex);
                                Long ciId = dataObj.getLong("ciId");
                                String uuid = dataObj.getString("uuid");
                                Long id = dataObj.getLong("id");
                                if (ciId != null) {
                                    if (!syncCiEntityMap.containsKey(ciId)) {
                                        syncCiEntityMap.put(ciId, new JSONArray());
                                        ciMap.put(ciId, ciMapper.getCiById(ciId));
                                    }
                                    syncCiEntityMap.get(ciId).add(dataObj);
                                }
                                if (StringUtils.isNotBlank(uuid)) {
                                    CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                                    if (id != null) {
                                        ciEntityTransactionVo.setCiEntityId(id);
                                    }
                                    ciEntityTransactionMap.put(uuid, ciEntityTransactionVo);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("hash为" + processTaskStepVo.getConfigHash() + "的processtask_step_config内容不是合法的JSON格式", ex);
        }

        // 审计详情
        JSONArray auditList = new JSONArray();
        if (MapUtils.isNotEmpty(syncCiEntityMap)) {
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                // 获取表单数据，写入CMDB
                InputFromContext.init(InputFrom.ITSM);
                TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                for (Long ciId : syncCiEntityMap.keySet()) {
                    JSONArray ciEntitySyncList = syncCiEntityMap.get(ciId);
                    for (int entityIndex = 0; entityIndex < ciEntitySyncList.size(); entityIndex++) {
                        JSONObject auditObj = new JSONObject();
                        JSONObject ciEntityObj = ciEntitySyncList.getJSONObject(entityIndex);
                        String uuid = ciEntityObj.getString("uuid");
                        CiEntityTransactionVo ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
                        try {
                            if ("delete".equalsIgnoreCase(ciEntityObj.getString("actionType"))
                                    && ciEntityObj.getLong("id") != null) {
                                // 删除操作
                                auditObj.put("ciEntityName", ciEntityService.getCiEntityBaseInfoById(ciEntityObj.getLong("id")).getName());
                                auditObj.put("ciEntityId", ciEntityObj.getLong("id"));
                                auditObj.put("ciId", ciId);
                                auditObj.put("ciName", ciMap.get(ciId).getName());
                                auditObj.put("ciLabel", ciMap.get(ciId).getLabel());
                                auditObj.put("action", TransactionActionType.DELETE.getValue());
                                CiEntityVo ciEntityVo = new CiEntityVo();
                                ciEntityVo.setId(ciEntityObj.getLong("id"));
                                Long transactionId = ciEntityService.deleteCiEntity(ciEntityVo, true, transactionGroupVo);
                                auditObj.put("status", "success");
                                auditObj.put("transactionId", transactionId);
                            } else {
                                Long id = ciEntityObj.getLong("id");
                                ciEntityTransactionVo.setCiId(ciId);
                                ciEntityTransactionVo.setCiEntityId(id);
                                ciEntityTransactionVo.setCiEntityUuid(uuid);

                                if ("insert".equalsIgnoreCase(ciEntityObj.getString("actionType")) || id == null) {
                                    ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                                } else {
                                    ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                                }
                                // 解析属性数据
                                JSONObject attrObj = ciEntityObj.getJSONObject("attrEntityData");
                                //修正新配置项的uuid为id
                                if (MapUtils.isNotEmpty(attrObj)) {
                                    for (String key : attrObj.keySet()) {
                                        JSONObject obj = attrObj.getJSONObject(key);
                                        JSONArray valueList = obj.getJSONArray("valueList");
                                        //删除没用的属性
                                        obj.remove("actualValueList");
                                        if (CollectionUtils.isNotEmpty(valueList)) {
                                            //因为可能需要删除某些成员，所以需要倒着循环
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
                                                            throw new ApiRuntimeException("找不到" + attrCiEntityUuid + "的新配置项");
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
                                }
                                ciEntityTransactionVo.setAttrEntityData(attrObj);
                                // 解析关系数据
                                JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
                                //修正新配置项的uuid为id
                                if (MapUtils.isNotEmpty(relObj)) {
                                    for (String key : relObj.keySet()) {
                                        JSONObject obj = relObj.getJSONObject(key);
                                        JSONArray relDataList = obj.getJSONArray("valueList");
                                        if (CollectionUtils.isNotEmpty(relDataList)) {
                                            for (int i = 0; i < relDataList.size(); i++) {
                                                JSONObject relEntityObj = relDataList.getJSONObject(i);
                                                if (relEntityObj.getLong("ciEntityId") == null && StringUtils.isNotBlank(relEntityObj.getString("ciEntityUuid"))) {
                                                    CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(relEntityObj.getString("ciEntityUuid"));
                                                    if (tmpVo != null) {
                                                        relEntityObj.put("ciEntityId", tmpVo.getCiEntityId());
                                                    } else {
                                                        throw new ApiRuntimeException("找不到" + relEntityObj.getString("ciEntityUuid") + "的新配置项");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                ciEntityTransactionVo.setRelEntityData(relObj);
                                // 因为不一定编辑所有属性，所以需要切换成局部更新模式
                                ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
                                ciEntityTransactionVo.setAllowCommit(true);
                                auditObj.put("action", ciEntityTransactionVo.getAction());
                                auditObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
                                auditObj.put("ciId", ciId);
                                auditObj.put("ciName", ciMap.get(ciId).getName());
                                auditObj.put("ciLabel", ciMap.get(ciId).getLabel());
                                Long transactionId = ciEntityService.saveCiEntity(ciEntityTransactionVo, transactionGroupVo);
                                transactionGroupVo.addTransactionId(transactionId);
                                auditObj.put("status", "success");
                                auditObj.put("transactionId", transactionId);
                            }
                        } catch (Exception ex) {
                            auditObj.put("error", ex.getMessage());
                            auditObj.put("status", "failed");
                        } finally {
                            CiEntityVo oldCiEntityVo = ciEntityService.getCiEntityBaseInfoById(ciEntityTransactionVo.getCiEntityId());
                            if (oldCiEntityVo != null) {
                                auditObj.put("ciEntityName", oldCiEntityVo.getName());
                            } else {
                                auditObj.put("ciEntityName", "新配置项");
                            }
                            auditObj.put("originalData", ciEntityObj);
                            auditList.add(auditObj);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(transactionGroupVo.getTransactionIdList())) {
                    for (Long transactionId : transactionGroupVo.getTransactionIdList()) {
                        transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionId);
                    }
                }
            }).execute();
            if (CollectionUtils.isNotEmpty(auditList)) {
                JSONObject auditData = new JSONObject();
                auditData.put("ciEntityList", auditList);
                ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo();
                processTaskStepDataVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                processTaskStepDataVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                processTaskStepDataVo.setType("cientitysync");
                processTaskStepDataVo.setData(auditData.toJSONString());
                processTaskStepDataMapper.replaceProcessTaskStepData(processTaskStepDataVo);
            }
        }

        return 0;
    }


    @Override
    protected int myTransfer(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList) {
        return 1;
    }

    @Override
    protected int myAssign(ProcessTaskStepVo currentProcessTaskStepVo, Set<ProcessTaskStepWorkerVo> workerSet)
            throws ProcessTaskException {
        return defaultAssign(currentProcessTaskStepVo, workerSet);
    }

    @Override
    protected Set<ProcessTaskStepVo> myGetNext(ProcessTaskStepVo currentProcessTaskStepVo,
                                               List<ProcessTaskStepVo> nextStepList, Long nextStepId) throws ProcessTaskException {
        Set<ProcessTaskStepVo> nextStepSet = new HashSet<>();
        if (nextStepList.size() == 1) {
            nextStepSet.add(nextStepList.get(0));
        } else if (nextStepList.size() > 1) {
            if (nextStepId == null) {
                throw new ProcessTaskException("找到多个后续节点");
            }
            for (ProcessTaskStepVo processTaskStepVo : nextStepList) {
                if (processTaskStepVo.getId().equals(nextStepId)) {
                    nextStepSet.add(processTaskStepVo);
                    break;
                }
            }
        }
        return nextStepSet;
    }

    @Override
    protected int myRedo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myStart(ProcessTaskStepVo processTaskStepVo) {
        return 0;
    }

    @Override
    public Boolean isAllowStart() {
        return null;
    }

    @Override
    protected int myStartProcess(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    protected int myHandle(ProcessTaskStepVo currentProcessTaskStepVo) {
        currentProcessTaskStepVo.setIsAllDone(true);
        return 0;
    }

    @Override
    protected int myComplete(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myCompleteAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRetreat(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 1;
    }

    @Override
    protected int myAbort(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myBack(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myHang(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRecover(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int mySaveDraft(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 1;
    }

    @Override
    protected int myPause(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

}