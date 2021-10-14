/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.rel;

import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.TransactionStatus;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RelServiceImpl implements RelService {
    //private final static Logger logger = LoggerFactory.getLogger(RelServiceImpl.class);

    @Autowired
    private RelMapper relMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private RelEntityMapper relEntityMapper;


    @Override
    @Transactional
    public void deleteRel(RelVo relVo) {
        //删除所有relEntity属性值，需要生成事务
        RelEntityVo relEntityVo = new RelEntityVo();
        relEntityVo.setRelId(relVo.getId());
        relEntityVo.setPageSize(100);
        List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
        BatchRunner<RelEntityVo> runner = new BatchRunner<>();
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
                    fromCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.FROM.getValue(), item.getToCiId(), item.getToCiEntityId(), item.getToCiEntityName(), TransactionActionType.DELETE.getValue());

                    //写入配置项事务
                    transactionMapper.insertCiEntityTransaction(fromCiEntityTransactionVo);
                } else {
                    //补充关系删除事务数据到同一个配置项的事务数据中
                    for (CiEntityTransactionVo fromCiEntityTransactionVo : fromCiEntityTransactionList) {
                        fromCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.FROM.getValue(), item.getToCiId(), item.getToCiEntityId(), item.getToCiEntityName(), TransactionActionType.DELETE.getValue());
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
                    toCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.TO.getValue(), item.getFromCiId(), item.getFromCiEntityId(), item.getFromCiEntityName(), TransactionActionType.DELETE.getValue());

                    transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
                } else {
                    for (CiEntityTransactionVo toCiEntityTransactionVo : toCiEntityTransactionList) {
                        toCiEntityTransactionVo.addRelEntityData(relVo, RelDirectionType.TO.getValue(), item.getFromCiId(), item.getFromCiEntityId(), item.getFromCiEntityName(), TransactionActionType.DELETE.getValue());
                        transactionMapper.updateCiEntityTransactionContent(toCiEntityTransactionVo);
                    }
                }
                //正式删除关系数据
                relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                        item.getFromCiEntityId(), item.getToCiEntityId());
            }


            relEntityVo.setCacheFlushKey(SnowflakeUtil.uniqueLong());//由于在同一个事务里，所以需要增加一个新参数扰乱mybatis的一级缓存
            relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
        }
        //删除模型关系
        relMapper.deleteRelById(relVo.getId());
    }
}
