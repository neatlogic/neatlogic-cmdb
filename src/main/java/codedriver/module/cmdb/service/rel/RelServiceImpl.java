/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.rel;

import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.TransactionStatus;
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
        //并发清理配置项数据，最高并发5个线程
        while (CollectionUtils.isNotEmpty(relEntityList)) {
            int parallel = 5;
            BatchRunner.State state = runner.execute(relEntityList, parallel, item -> {
                if (item != null) {
                    //检查当前配置项在当前事务组下是否已经存在事务，如果已经存在则无需创建新的事务
                    CiEntityTransactionVo fromCiEntityTransactionVo = transactionMapper.getCiEntityTransactionByTransactionGroupIdAndCiEntityId(transactionGroupVo.getId(), item.getFromCiEntityId());
                    if (fromCiEntityTransactionVo == null) {
                        //写入事务
                        TransactionVo fromTransactionVo = new TransactionVo();
                        fromTransactionVo.setCiId(item.getFromCiId());
                        fromTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                        transactionMapper.insertTransaction(fromTransactionVo);
                        //写入事务分组
                        transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), fromTransactionVo.getId());
                        //写入来源端配置项事务
                        fromCiEntityTransactionVo = new CiEntityTransactionVo();
                        fromCiEntityTransactionVo.setCiEntityId(item.getFromCiEntityId());
                        fromCiEntityTransactionVo.setCiId(item.getFromCiId());
                        fromCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                        fromCiEntityTransactionVo.setTransactionId(fromTransactionVo.getId());
                        fromCiEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getFromCiEntityId()));
                        // 创建快照快照
                        ciEntityService.createSnapshot(fromCiEntityTransactionVo);
                        //补充关系删除事务数据
                        fromCiEntityTransactionVo.addRelEntityData(item.getRelId(), RelDirectionType.TO.getValue(), item.getToCiId(), item.getToCiEntityId(), TransactionActionType.DELETE.getValue());

                        //写入配置项事务
                        transactionMapper.insertCiEntityTransaction(fromCiEntityTransactionVo);
                    } else {
                        //补充关系删除事务数据到同一个配置项的事务数据中
                        fromCiEntityTransactionVo.addRelEntityData(item.getRelId(), RelDirectionType.TO.getValue(), item.getToCiId(), item.getToCiEntityId(), TransactionActionType.DELETE.getValue());
                        transactionMapper.updateCiEntityTransactionContent(fromCiEntityTransactionVo);
                    }

                    //针对目标配置项重新做一遍以上逻辑
                    CiEntityTransactionVo toCiEntityTransactionVo = transactionMapper.getCiEntityTransactionByTransactionGroupIdAndCiEntityId(transactionGroupVo.getId(), item.getToCiEntityId());
                    if (toCiEntityTransactionVo == null) {
                        //写入目标端配置项事务
                        //写入事务
                        TransactionVo toTransactionVo = new TransactionVo();
                        toTransactionVo.setCiId(item.getToCiId());
                        toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                        transactionMapper.insertTransaction(toTransactionVo);
                        //写入事务分组
                        transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());

                        toCiEntityTransactionVo = new CiEntityTransactionVo();
                        toCiEntityTransactionVo.setCiEntityId(item.getToCiEntityId());
                        toCiEntityTransactionVo.setCiId(item.getToCiId());
                        toCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                        toCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                        toCiEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getToCiEntityId()));

                        // 创建快照
                        ciEntityService.createSnapshot(toCiEntityTransactionVo);
                        //补充事务删除数据
                        toCiEntityTransactionVo.addRelEntityData(item.getRelId(), RelDirectionType.FROM.getValue(), item.getFromCiId(), item.getFromCiEntityId(), TransactionActionType.DELETE.getValue());

                        transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
                    } else {
                        toCiEntityTransactionVo.addRelEntityData(item.getRelId(), RelDirectionType.FROM.getValue(), item.getFromCiId(), item.getFromCiEntityId(), TransactionActionType.DELETE.getValue());
                        transactionMapper.updateCiEntityTransactionContent(toCiEntityTransactionVo);
                    }
                    //正式删除关系数据
                    relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                            item.getFromCiEntityId(), item.getToCiEntityId());

                }
            });
            if (state.isSucceed()) {
                //由于是边删除边查询，所以不需要换页直接查询，直到所有数据删除完毕为止，必须判断是否所有操作都能正常完成，否则可能导致死循环
                relEntityList = relEntityMapper.getRelEntityByRelId(relEntityVo);
            } else {
                throw new RuntimeException(state.getError());
            }
        }

        //删除模型关系
        relMapper.deleteRelById(relVo.getId());
    }


}
