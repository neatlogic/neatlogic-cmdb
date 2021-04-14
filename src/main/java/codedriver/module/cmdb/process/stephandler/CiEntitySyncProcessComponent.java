/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.process.stephandler;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.process.constvalue.ProcessStepMode;
import codedriver.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskStepDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.exception.core.ProcessTaskException;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerBase;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class CiEntitySyncProcessComponent extends ProcessStepHandlerBase {
    static Logger logger = LoggerFactory.getLogger(CiEntitySyncProcessComponent.class);

    @Autowired
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Autowired
    private CiEntityService ciEntityService;

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

    @SuppressWarnings("serial")
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
    protected int myActive(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        ProcessTaskStepVo processTaskStepVo =
                processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
        String stepConfig = selectContentByHashMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
        // 获取参数
        Map<Long, JSONArray> syncCiEntityMap = new HashMap<>();
        try {
            JSONObject stepConfigObj = JSONObject.parseObject(stepConfig);
            currentProcessTaskStepVo.setParamObj(stepConfigObj);
            if (MapUtils.isNotEmpty(stepConfigObj)) {
                JSONArray handlerList = stepConfigObj.getJSONArray("handlerList");
                if (CollectionUtils.isNotEmpty(handlerList)) {
                    for (int hindex = 0; hindex < handlerList.size(); hindex++) {
                        ProcessTaskFormAttributeDataVo p = new ProcessTaskFormAttributeDataVo();
                        p.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                        p.setAttributeUuid(handlerList.getString(hindex));
                        ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo =
                                processTaskMapper.getProcessTaskFormAttributeDataByProcessTaskIdAndAttributeUuid(p);
                        if (processTaskFormAttributeDataVo != null) {
                            JSONArray dataList = JSONArray.parseArray(processTaskFormAttributeDataVo.getData());
                            for (int dindex = 0; dindex < dataList.size(); dindex++) {
                                JSONObject dataObj = dataList.getJSONObject(dindex);
                                JSONArray entityObjList = dataObj.getJSONArray("entityList");
                                Long ciId = dataObj.getLong("ciId");
                                if (ciId != null && CollectionUtils.isNotEmpty(entityObjList)) {
                                    syncCiEntityMap.put(ciId, entityObjList);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("hash为" + processTaskStepVo.getConfigHash() + "的processtask_step_config内容不是合法的JSON格式", ex);
        }

        // 获取表单数据，写入CMDB
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        JSONArray transactionList = new JSONArray();
        // 审计详情
        JSONArray auditList = new JSONArray();
        if (MapUtils.isNotEmpty(syncCiEntityMap)) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            // 为了隔离配置项保存和流程的事务，所以需要另起线程执行
            CachedThreadPool
                    .execute(new CiEntitySyncThread(syncCiEntityMap, auditList, transactionGroupVo, countDownLatch));
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
            }
            if (CollectionUtils.isNotEmpty(transactionGroupVo.getTransactionIdList())) {
                for (Long transactionId : transactionGroupVo.getTransactionIdList()) {
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionId);
                }
            }
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

    private class CiEntitySyncThread extends CodeDriverThread {
        private final Map<Long, JSONArray> ciEntitySyncMap;
        private final JSONArray auditList;
        private final TransactionGroupVo transactionGroupVo;
        private final CountDownLatch countDownLatch;

        public CiEntitySyncThread(Map<Long, JSONArray> _ciEntitySyncMap, JSONArray _auditList,
                                  TransactionGroupVo _transactionGroupVo, CountDownLatch _countDownLatch) {
            ciEntitySyncMap = _ciEntitySyncMap;
            auditList = _auditList;
            transactionGroupVo = _transactionGroupVo;
            countDownLatch = _countDownLatch;
        }

        @Override
        protected void execute() {
            try {
                List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                Iterator<Long> cikeys = ciEntitySyncMap.keySet().iterator();
                while (cikeys.hasNext()) {
                    Long ciId = cikeys.next();
                    JSONArray ciEntitySyncList = ciEntitySyncMap.get(ciId);
                    for (int entityIndex = 0; entityIndex < ciEntitySyncList.size(); entityIndex++) {
                        JSONObject auditObj = new JSONObject();
                        JSONObject ciEntityObj = ciEntitySyncList.getJSONObject(entityIndex);
                        try {
                            if ("delete".equalsIgnoreCase(ciEntityObj.getString("form_actiontype"))
                                    && ciEntityObj.getLong("id") != null) {
                                // 删除操作
                                ciEntityService.deleteCiEntity(ciEntityObj.getLong("id"));
                                continue;
                            }

                            Long id = ciEntityObj.getLong("id");
                            String uuid = ciEntityObj.getString("uuid");
                            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                            ciEntityTransactionVo.setCiId(ciId);
                            ciEntityTransactionVo.setCiEntityId(id);
                            ciEntityTransactionVo.setCiEntityUuid(uuid);

                            if (id == null) {
                                ciEntityTransactionVo.setTransactionMode(TransactionActionType.INSERT);
                            } else {
                                ciEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
                            }
                            // 解析属性数据
                            JSONObject attrObj = ciEntityObj.getJSONObject("attrEntityData");
                            ciEntityTransactionVo.setAttrEntityData(attrObj);
                            // 解析关系数据
                            JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
                            ciEntityTransactionVo.setRelEntityData(relObj);
                            // 因为不一定编辑所有属性，所以需要切换成局部更新模式
                            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
                            Long transactionId = ciEntityService.saveCiEntity(ciEntityTransactionVo);
                            transactionGroupVo.addTransactionId(transactionId);
                            auditObj.put("status", "success");
                            auditObj.put("transactionId", transactionId);
                            auditObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
                        } catch (Exception ex) {
                            auditObj.put("error", ex.getMessage());
                            auditObj.put("status", "failed");
                        } finally {
                            auditObj.put("originalData", ciEntityObj);
                            auditList.add(auditObj);
                        }
                    }
                }
            } finally {
                countDownLatch.countDown();
            }
        }

    }

    @Override
    protected int myTransfer(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList)
            throws ProcessTaskException {
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
    protected int myRedo(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
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
    protected int myStartProcess(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    protected int myHandle(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
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
    protected int myRetreat(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myAbort(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myBack(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
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
    protected int mySaveDraft(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myPause(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

}