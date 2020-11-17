package codedriver.module.cmdb.process.stephandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.process.constvalue.ProcessStepMode;
import codedriver.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import codedriver.framework.process.dto.ProcessTaskStepDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerPolicyVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.exception.core.ProcessTaskException;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerBase;
import codedriver.framework.process.workerpolicy.core.IWorkerPolicyHandler;
import codedriver.framework.process.workerpolicy.core.WorkerPolicyHandlerFactory;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;

/**
 * @Author:chenqiwei
 * @Time:2020年11月10日
 * @ClassName: CiEntitySyncProcessComponent
 * @Description: 同步配置项数据流程组件
 */
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
        JSONArray ciEntitySyncList = null;
        try {
            JSONObject stepConfigObj = JSONObject.parseObject(stepConfig);
            currentProcessTaskStepVo.setParamObj(stepConfigObj);
            if (MapUtils.isNotEmpty(stepConfigObj)) {
                ciEntitySyncList = stepConfigObj.getJSONArray("ciEntitySyncList");
            }
        } catch (Exception ex) {
            logger.error("hash为" + processTaskStepVo.getConfigHash() + "的processtask_step_config内容不是合法的JSON格式", ex);
        }

        /** 测试代码 **/
        ciEntitySyncList = new JSONArray();
        ciEntitySyncList.add(JSONObject.parse(
            "{\"attrEntityData\":{\"attr_193105512898561\":{\"valueList\":[\"test\"]},\"attr_193105756168192\":{\"valueList\":[\"2.2.2.2\"]},\"attr_193105890385920\":{\"valueList\":[\"30\"]},\"attr_193106418868224\":{\"valueList\":[\"8.X\"]}},\"relEntityData\":{\"relfrom_193107098345472\":[{\"ciEntityId\":199658357923840,\"ciEntityName\":\"CNSZ87344\"}],\"relto_193112576106496\":[{\"ciEntityId\":228892598083584}]},\"ciId\":\"193105512898560\"}"));
        // 测试代码结束

        // 获取表单数据，写入CMDB
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        JSONArray transactionList = new JSONArray();
        // 审计详情
        JSONArray auditList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciEntitySyncList)) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            // 为了隔离配置项保存和流程的事务，所以需要另起线程执行
            CachedThreadPool
                .execute(new CiEntitySyncThread(ciEntitySyncList, auditList, transactionGroupVo, countDownLatch));
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
                ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo(true);
                processTaskStepDataVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                processTaskStepDataVo.setProcessTaskStepId(currentProcessTaskStepVo.getFromProcessTaskStepId());
                processTaskStepDataVo.setType("cientitysync");
                processTaskStepDataVo.setData(auditData.toJSONString());
                processTaskStepDataMapper.replaceProcessTaskStepData(processTaskStepDataVo);
            }
        }

        return 0;
    }

    private class CiEntitySyncThread extends CodeDriverThread {
        private JSONArray ciEntitySyncList;
        private JSONArray auditList;
        private TransactionGroupVo transactionGroupVo;
        private CountDownLatch countDownLatch;

        public CiEntitySyncThread(JSONArray _ciEntitySyncList, JSONArray _auditList,
            TransactionGroupVo _transactionGroupVo, CountDownLatch _countDownLatch) {
            ciEntitySyncList = _ciEntitySyncList;
            auditList = _auditList;
            transactionGroupVo = _transactionGroupVo;
            countDownLatch = _countDownLatch;
        }

        @Override
        protected void execute() {
            try {
                for (int entityIndex = 0; entityIndex < ciEntitySyncList.size(); entityIndex++) {
                    JSONObject auditObj = new JSONObject();
                    JSONObject jsonObj = ciEntitySyncList.getJSONObject(entityIndex);
                    try {
                        Long ciId = jsonObj.getLong("ciId");
                        Long id = jsonObj.getLong("id");
                        TransactionActionType mode = TransactionActionType.INSERT;
                        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                        ciEntityTransactionVo.setCiId(ciId);
                        // 解析属性数据
                        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
                        if (MapUtils.isNotEmpty(attrObj)) {
                            List<AttrEntityTransactionVo> attrEntityList = new ArrayList<>();
                            Iterator<String> keys = attrObj.keySet().iterator();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                Long attrId = null;
                                try {
                                    attrId = Long.parseLong(key.replace("attr_", ""));
                                } catch (Exception ex) {
                                    logger.error(ex.getMessage(), ex);
                                }
                                if (attrId != null) {
                                    AttrEntityTransactionVo attrEntityVo = new AttrEntityTransactionVo();
                                    attrEntityVo.setAttrId(attrId);
                                    JSONObject attrDataObj = attrObj.getJSONObject(key);
                                    JSONArray valueObjList = attrDataObj.getJSONArray("valueList");
                                    attrEntityVo.setActualValueList(
                                        valueObjList.stream().map(v -> v.toString()).collect(Collectors.toList()));
                                    attrEntityList.add(attrEntityVo);
                                }
                            }
                            ciEntityTransactionVo.setAttrEntityTransactionList(attrEntityList);
                        }
                        // 解析关系数据
                        JSONObject relObj = jsonObj.getJSONObject("relEntityData");
                        if (MapUtils.isNotEmpty(relObj)) {
                            List<RelEntityTransactionVo> relEntityList = new ArrayList<>();
                            Iterator<String> keys = relObj.keySet().iterator();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                JSONArray relDataList = relObj.getJSONArray(key);

                                if (key.startsWith("relfrom_")) {// 当前配置项处于from位置
                                    if (CollectionUtils.isNotEmpty(relDataList)) {
                                        for (int i = 0; i < relDataList.size(); i++) {
                                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                                            RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo();
                                            relEntityVo.setRelId(Long.parseLong(key.replace("relfrom_", "")));
                                            relEntityVo.setToCiEntityId(relEntityObj.getLong("ciEntityId"));
                                            relEntityVo.setDirection(RelDirectionType.FROM.getValue());
                                            relEntityVo.setFromCiEntityId(ciEntityTransactionVo.getCiEntityId());
                                            relEntityVo.setAction(RelActionType.INSERT.getValue());// 默认是添加关系
                                            relEntityList.add(relEntityVo);
                                        }
                                    }
                                } else if (key.startsWith("relto_")) {// 当前配置项处于to位置
                                    if (CollectionUtils.isNotEmpty(relDataList)) {
                                        for (int i = 0; i < relDataList.size(); i++) {
                                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                                            RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo();
                                            relEntityVo.setRelId(Long.parseLong(key.replace("relto_", "")));
                                            relEntityVo.setFromCiEntityId(relEntityObj.getLong("ciEntityId"));
                                            relEntityVo.setDirection(RelDirectionType.TO.getValue());
                                            relEntityVo.setToCiEntityId(ciEntityTransactionVo.getCiEntityId());
                                            relEntityVo.setAction(RelActionType.INSERT.getValue());// 默认是添加关系
                                            relEntityList.add(relEntityVo);
                                        }
                                    }
                                }
                            }
                            ciEntityTransactionVo.setRelEntityTransactionList(relEntityList);
                        }
                        Long transactionId = ciEntityService.saveCiEntity(ciEntityTransactionVo, mode);
                        if (transactionId > 0) {
                            // 保存事务组，将来可能需要同时提交
                            transactionGroupVo.addTransactionId(transactionId);
                            auditObj.put("status", "success");
                        } else {
                            auditObj.put("status", "same");
                        }
                    } catch (Exception ex) {
                        auditObj.put("error", ex.getMessage());
                        auditObj.put("status", "failed");
                    } finally {
                        auditObj.put("originalData", jsonObj);
                        auditList.add(auditObj);
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
    protected int myAssign(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList)
        throws ProcessTaskException {
        /** 获取步骤配置信息 **/
        ProcessTaskStepVo processTaskStepVo =
            processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
        String stepConfig = selectContentByHashMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());

        String executeMode = "";
        int autoStart = 0;
        try {
            JSONObject stepConfigObj = JSONObject.parseObject(stepConfig);
            currentProcessTaskStepVo.getParamObj().putAll(stepConfigObj);
            if (MapUtils.isNotEmpty(stepConfigObj)) {
                JSONObject workerPolicyConfig = stepConfigObj.getJSONObject("workerPolicyConfig");
                if (MapUtils.isNotEmpty(stepConfigObj)) {
                    executeMode = workerPolicyConfig.getString("executeMode");
                    autoStart = workerPolicyConfig.getIntValue("autoStart");
                }
            }
        } catch (Exception ex) {
            logger.error("hash为" + processTaskStepVo.getConfigHash() + "的processtask_step_config内容不是合法的JSON格式", ex);
        }

        /** 如果workerList.size()>0，说明已经存在过处理人，则继续使用旧处理人，否则启用分派 **/
        if (CollectionUtils.isEmpty(workerList)) {
            /** 分配处理人 **/
            ProcessTaskStepWorkerPolicyVo processTaskStepWorkerPolicyVo = new ProcessTaskStepWorkerPolicyVo();
            processTaskStepWorkerPolicyVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
            List<ProcessTaskStepWorkerPolicyVo> workerPolicyList =
                processTaskMapper.getProcessTaskStepWorkerPolicy(processTaskStepWorkerPolicyVo);
            if (CollectionUtils.isNotEmpty(workerPolicyList)) {
                for (ProcessTaskStepWorkerPolicyVo workerPolicyVo : workerPolicyList) {
                    IWorkerPolicyHandler workerPolicyHandler =
                        WorkerPolicyHandlerFactory.getHandler(workerPolicyVo.getPolicy());
                    if (workerPolicyHandler != null) {
                        List<ProcessTaskStepWorkerVo> tmpWorkerList =
                            workerPolicyHandler.execute(workerPolicyVo, currentProcessTaskStepVo);
                        /** 顺序分配处理人 **/
                        if ("sort".equals(executeMode) && CollectionUtils.isEmpty(tmpWorkerList)) {
                            // 找到处理人，则退出
                            workerList.addAll(tmpWorkerList);
                            break;
                        } else if ("batch".equals(executeMode)) {
                            // 去重取并集
                            tmpWorkerList.removeAll(workerList);
                            workerList.addAll(tmpWorkerList);
                        }
                    }
                }
            }
        }

        return autoStart;
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