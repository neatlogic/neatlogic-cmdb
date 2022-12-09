/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.schedule.handler;

import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.TransactionStatus;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 清除过期关系
 */
@Component
@DisallowConcurrentExecution
public class ClearExpiredRelEntityScheduleJob extends JobBase {
    private static final String CRON_EXPRESSION = "0 0 0 * * ?";
    private final Map<Long, RelVo> relMap = new HashMap<>();
    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private RelMapper relMapper;


    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        String tenantUuid = jobObject.getTenantUuid();
        JobObject newJobObject = new JobObject.Builder("1", this.getGroupName(), this.getClassName(), tenantUuid).withCron(CRON_EXPRESSION).build();
        schedulerManager.loadJob(newJobObject);
    }

    @Override
    public void initJob(String tenantUuid) {
        JobObject newJobObject = new JobObject.Builder("1", this.getGroupName(), this.getClassName(), tenantUuid).withCron(CRON_EXPRESSION).build();
        schedulerManager.loadJob(newJobObject);
    }

    private RelVo getRelById(Long relId) {
        if (!relMap.containsKey(relId)) {
            relMap.put(relId, relMapper.getRelById(relId));
        }
        return relMap.get(relId);
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        RelEntityVo pRelEntityVo = new RelEntityVo();
        pRelEntityVo.setPageSize(100);
        pRelEntityVo.setCurrentPage(1);
        List<RelEntityVo> relEntityList = relEntityMapper.getExpiredRelEntity(pRelEntityVo);
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();

        while (CollectionUtils.isNotEmpty(relEntityList)) {
            for (RelEntityVo item : relEntityList) {
                //检查当前配置项在当前事务组下是否已经存在事务，如果已经存在则无需创建新的事务
                List<CiEntityTransactionVo> fromCiEntityTransactionList = transactionMapper.getCiEntityTransactionByTransactionGroupIdAndCiEntityId(transactionGroupVo.getId(), item.getFromCiEntityId());
                if (CollectionUtils.isEmpty(fromCiEntityTransactionList)) {
                    //写入事务
                    TransactionVo fromTransactionVo = new TransactionVo();
                    fromTransactionVo.setCiId(item.getFromCiId());
                    fromTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                    fromTransactionVo.setInputFrom(InputFromContext.get().getInputFrom());
                    fromTransactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                    fromTransactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                    transactionMapper.insertTransaction(fromTransactionVo);
                    //写入事务分组
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), fromTransactionVo.getId());
                    //写入来源端配置项事务
                    CiEntityTransactionVo fromCiEntityTransactionVo = new CiEntityTransactionVo();
                    fromCiEntityTransactionVo.setCiEntityId(item.getFromCiEntityId());
                    fromCiEntityTransactionVo.setCiId(item.getFromCiId());
                    fromCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                    fromCiEntityTransactionVo.setTransactionId(fromTransactionVo.getId());
                    fromCiEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getFromCiId(), item.getFromCiEntityId()));
                    // 创建快照快照
                    ciEntityService.createSnapshot(fromCiEntityTransactionVo);
                    //补充关系删除事务数据
                    fromCiEntityTransactionVo.addRelEntityData(getRelById(item.getRelId()), RelDirectionType.FROM.getValue(), item.getToCiId(), item.getToCiEntityId(), item.getToCiEntityName(), TransactionActionType.DELETE.getValue());

                    //写入配置项事务
                    transactionMapper.insertCiEntityTransaction(fromCiEntityTransactionVo);
                } else {
                    //补充关系删除事务数据到同一个配置项的事务数据中
                    for (CiEntityTransactionVo fromCiEntityTransactionVo : fromCiEntityTransactionList) {
                        fromCiEntityTransactionVo.addRelEntityData(getRelById(item.getRelId()), RelDirectionType.FROM.getValue(), item.getToCiId(), item.getToCiEntityId(), item.getToCiEntityName(), TransactionActionType.DELETE.getValue());
                        transactionMapper.updateCiEntityTransactionContent(fromCiEntityTransactionVo);
                    }
                }

                //针对目标配置项重新做一遍以上逻辑
                List<CiEntityTransactionVo> toCiEntityTransactionList = transactionMapper.getCiEntityTransactionByTransactionGroupIdAndCiEntityId(transactionGroupVo.getId(), item.getToCiEntityId());
                if (CollectionUtils.isEmpty(toCiEntityTransactionList)) {
                    //写入目标端配置项事务
                    //写入事务
                    TransactionVo toTransactionVo = new TransactionVo();
                    toTransactionVo.setCiId(item.getToCiId());
                    toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                    toTransactionVo.setInputFrom(InputFromContext.get().getInputFrom());
                    toTransactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                    toTransactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                    transactionMapper.insertTransaction(toTransactionVo);
                    //写入事务分组
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());
                    CiEntityTransactionVo toCiEntityTransactionVo = new CiEntityTransactionVo();
                    toCiEntityTransactionVo.setCiEntityId(item.getToCiEntityId());
                    toCiEntityTransactionVo.setCiId(item.getToCiId());
                    toCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                    toCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                    toCiEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getToCiId(), item.getToCiEntityId()));

                    // 创建快照
                    ciEntityService.createSnapshot(toCiEntityTransactionVo);
                    //补充事务删除数据
                    toCiEntityTransactionVo.addRelEntityData(getRelById(item.getRelId()), RelDirectionType.TO.getValue(), item.getFromCiId(), item.getFromCiEntityId(), item.getFromCiEntityName(), TransactionActionType.DELETE.getValue());

                    transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
                } else {
                    for (CiEntityTransactionVo toCiEntityTransactionVo : toCiEntityTransactionList) {
                        toCiEntityTransactionVo.addRelEntityData(getRelById(item.getRelId()), RelDirectionType.TO.getValue(), item.getFromCiId(), item.getFromCiEntityId(), item.getFromCiEntityName(), TransactionActionType.DELETE.getValue());
                        transactionMapper.updateCiEntityTransactionContent(toCiEntityTransactionVo);
                    }
                }
                //正式删除关系数据
                relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                        item.getFromCiEntityId(), item.getToCiEntityId());
            }

            pRelEntityVo.setCacheFlushKey(SnowflakeUtil.uniqueLong());//由于在同一个事务里，所以需要增加一个新参数扰乱mybatis的一级缓存
            relEntityList = relEntityMapper.getExpiredRelEntity(pRelEntityVo);
        }
    }


    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-EXPIRED-RELENTITY-CLEARER";
    }

}
