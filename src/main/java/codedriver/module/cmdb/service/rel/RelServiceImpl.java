/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.rel;

import codedriver.framework.batch.BatchJob;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.cmdb.constvalue.TransactionStatus;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RelServiceImpl implements RelService {
    private final static Logger logger = LoggerFactory.getLogger(RelServiceImpl.class);

    @Autowired
    private RelMapper relMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private RelEntityMapper relEntityMapper;


    @Override
    @Transactional
    public void deleteRelById(RelVo relVo) {
        //删除所有attrEntity属性值，需要生成事务
        List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByRelId(relVo.getId());
        BatchRunner<RelEntityVo> runner = new BatchRunner<>();
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        //并发清理配置项数据，最高并发10个线程
        int parallel = 10;
        runner.execute(relEntityList, parallel, new BatchJob<RelEntityVo>() {
            @Override
            public void execute(RelEntityVo item) {
                if (item != null) {
                    //写入事务
                    TransactionVo fromTransactionVo = new TransactionVo();
                    fromTransactionVo.setCiId(item.getFromCiId());
                    fromTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                    transactionMapper.insertTransaction(fromTransactionVo);
                    //写入事务分组
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), fromTransactionVo.getId());
                    //写入来源端配置项事务
                    CiEntityTransactionVo fromCiEntityTransactionVo = new CiEntityTransactionVo();
                    fromCiEntityTransactionVo.setCiEntityId(item.getFromCiEntityId());
                    fromCiEntityTransactionVo.setCiId(item.getFromCiId());
                    fromCiEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
                    fromCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                    fromCiEntityTransactionVo.setTransactionId(fromTransactionVo.getId());

                    // 保存快照
                    ciEntityService.createSnapshot(fromCiEntityTransactionVo);

                    RelEntityTransactionVo fromRelEntityVo = new RelEntityTransactionVo(item, RelActionType.DELETE);
                    fromRelEntityVo.setTransactionId(fromTransactionVo.getId());

                    //写入配置项事务
                    transactionMapper.insertCiEntityTransaction(fromCiEntityTransactionVo);
                    // 写入关系事务
                    transactionMapper.insertRelEntityTransaction(fromRelEntityVo);


                    //写入目标端配置项事务
                    //写入事务
                    TransactionVo toTransactionVo = new TransactionVo();
                    toTransactionVo.setCiId(item.getToCiId());
                    toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                    transactionMapper.insertTransaction(toTransactionVo);
                    //写入事务分组
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());

                    CiEntityTransactionVo toCiEntityTransactionVo = new CiEntityTransactionVo();
                    toCiEntityTransactionVo.setCiEntityId(item.getToCiEntityId());
                    toCiEntityTransactionVo.setCiId(item.getToCiId());
                    toCiEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
                    toCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                    toCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());

                    // 保存快照
                    ciEntityService.createSnapshot(toCiEntityTransactionVo);

                    RelEntityTransactionVo toRelEntityVo = new RelEntityTransactionVo(item, RelActionType.DELETE);
                    toRelEntityVo.setTransactionId(toTransactionVo.getId());


                    transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
                    // 写入关系事务
                    transactionMapper.insertRelEntityTransaction(toRelEntityVo);

                    //真正修改数据
                    relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                            item.getFromCiEntityId(), item.getToCiEntityId());

                }
            }
        });
        //删除模型关系
        relMapper.deleteRelById(relVo.getId());
        //删除视图数据
        //CiSchemaHandler.deleteRel(relVo);
    }


}
