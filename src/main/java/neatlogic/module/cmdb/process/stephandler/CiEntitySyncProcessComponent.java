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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.NewCiEntityNotFoundException;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.process.constvalue.ProcessStepMode;
import neatlogic.framework.process.crossover.IProcessTaskCrossoverMapper;
import neatlogic.framework.process.crossover.IProcessTaskCrossoverService;
import neatlogic.framework.process.crossover.IProcessTaskStepDataCrossoverMapper;
import neatlogic.framework.process.crossover.ISelectContentByHashCrossoverMapper;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerBase;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
//@Deprecated
public class CiEntitySyncProcessComponent extends ProcessStepHandlerBase {
    static Logger logger = LoggerFactory.getLogger(CiEntitySyncProcessComponent.class);

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiMapper ciMapper;

    @Resource
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
        IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
        ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
        ProcessTaskStepVo processTaskStepVo =
                processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
        String stepConfig = selectContentByHashCrossoverMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
        // 获取参数
        Map<Long, JSONArray> syncCiEntityMap = new HashMap<>();
        Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
        Map<Long, CiVo> ciMap = new HashMap<>();
        try {
            JSONObject stepConfigObj = JSON.parseObject(stepConfig);
            currentProcessTaskStepVo.getParamObj().putAll(stepConfigObj);
            if (MapUtils.isNotEmpty(stepConfigObj)) {
                JSONArray handlerList = stepConfigObj.getJSONArray("handlerList");
                if (CollectionUtils.isNotEmpty(handlerList)) {
                    IProcessTaskCrossoverService processTaskCrossoverService = CrossoverServiceFactory.getApi(IProcessTaskCrossoverService.class);
                    List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskCrossoverService.getProcessTaskFormAttributeDataListByProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                    Map<String, ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataMap = processTaskFormAttributeDataList.stream().collect(Collectors.toMap(e -> e.getAttributeUuid(), e -> e));
                    for (int hIndex = 0; hIndex < handlerList.size(); hIndex++) {
                        ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo = processTaskFormAttributeDataMap.get(handlerList.getString(hIndex));
                        if (processTaskFormAttributeDataVo != null) {
                            JSONArray dataList = JSON.parseArray(processTaskFormAttributeDataVo.getData());
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
            new EscapeTransactionJob(() -> {
                // 获取表单数据，写入CMDB
                InputFromContext.init(InputFrom.ITSM);
                TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                for (Map.Entry<Long, JSONArray> entry : syncCiEntityMap.entrySet()) {
                    Long ciId = entry.getKey();
                    JSONArray ciEntitySyncList = entry.getValue();
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
                                ciEntityVo.setDescription(ciEntityObj.getString("description"));
                                Long transactionId = ciEntityService.deleteCiEntity(ciEntityVo, true, transactionGroupVo);
                                auditObj.put("status", "success");
                                auditObj.put("transactionId", transactionId);
                            } else {
                                Long id = ciEntityObj.getLong("id");
                                ciEntityTransactionVo.setCiId(ciId);
                                ciEntityTransactionVo.setCiEntityId(id);
                                ciEntityTransactionVo.setCiEntityUuid(uuid);
                                ciEntityTransactionVo.setDescription(ciEntityObj.getString("description"));
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
                                                            throw new NewCiEntityNotFoundException(attrCiEntityUuid);
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
                                                        throw new NewCiEntityNotFoundException(relEntityObj.getString("ciEntityUuid"));
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
                            if (ex instanceof ApiRuntimeException) {
                                auditObj.put("error", ((ApiRuntimeException) ex).getMessage());
                            } else {
                                auditObj.put("error", ex.getMessage());
                            }
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
                processTaskStepDataVo.setFcu(UserContext.get().getUserUuid());
                IProcessTaskStepDataCrossoverMapper processTaskStepDataCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskStepDataCrossoverMapper.class);
                processTaskStepDataCrossoverMapper.replaceProcessTaskStepData(processTaskStepDataVo);
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
    protected Set<Long> myGetNext(ProcessTaskStepVo currentProcessTaskStepVo, List<Long> nextStepIdList, Long nextStepId) throws ProcessTaskException {
        return defaultGetNext(nextStepIdList, nextStepId);
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
    protected int myReapproval(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myReapprovalAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
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
