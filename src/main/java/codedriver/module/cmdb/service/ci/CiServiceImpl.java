package codedriver.module.cmdb.service.ci;

import codedriver.framework.batch.BatchJob;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.cmdb.constvalue.TransactionStatus;
import codedriver.module.cmdb.cischema.CiSchemaHandler;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.module.cmdb.dto.transaction.TransactionVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.service.rel.RelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CiServiceImpl implements CiService {
    private final static Logger logger = LoggerFactory.getLogger(CiServiceImpl.class);

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

    @Autowired
    private RelService relService;

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public int saveCi(CiVo ciVo) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int deleteCi(Long ciId) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        //补充受影响配置项的事务信息
        List<RelVo> relList = relMapper.getRelByCiId(ciId);
        for (RelVo relVo : relList) {
            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByFromCiIdAndRelId(relVo.getId(), ciId);
                BatchRunner<RelEntityVo> runner = new BatchRunner<>();
                TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                //并发清理配置项数据，最高并发10个线程
                int parallel = 10;
                runner.execute(relEntityList, parallel, new BatchJob<RelEntityVo>() {
                    @Override
                    public void execute(RelEntityVo item) {
                        if (item != null) {
                            //写入事务
                            TransactionVo transactionVo = new TransactionVo();
                            transactionMapper.insertTransaction(transactionVo);
                            //写入事务分组
                            transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionVo.getId());

                            //写入目标端配置项事务
                            CiEntityTransactionVo toCiEntityTransactionVo = new CiEntityTransactionVo();
                            toCiEntityTransactionVo.setCiEntityId(item.getToCiEntityId());
                            toCiEntityTransactionVo.setCiId(item.getToCiId());
                            toCiEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
                            toCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            toCiEntityTransactionVo.setTransactionId(transactionVo.getId());

                            // 保存快照
                            ciEntityService.createSnapshot(toCiEntityTransactionVo);

                            RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo(item, RelActionType.DELETE);
                            relEntityVo.setTransactionId(transactionVo.getId());

                            //写入配置项事务
                            transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
                            // 写入属性事务
                            transactionMapper.insertRelEntityTransaction(relEntityVo);

                            //真正修改数据
                            relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                                    item.getFromCiEntityId(), item.getToCiEntityId());
                            transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                            transactionMapper.updateTransactionStatus(transactionVo);
                        }
                    }
                });
            } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByToCiIdAndRelId(relVo.getId(), ciId);
                BatchRunner<RelEntityVo> runner = new BatchRunner<>();
                TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                //并发清理配置项数据，最高并发10个线程
                int parallel = 10;
                runner.execute(relEntityList, parallel, new BatchJob<RelEntityVo>() {
                    @Override
                    public void execute(RelEntityVo item) {
                        if (item != null) {
                            //写入事务
                            TransactionVo transactionVo = new TransactionVo();
                            transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                            transactionMapper.insertTransaction(transactionVo);
                            //写入事务分组
                            transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionVo.getId());

                            //写入来源端配置项事务
                            CiEntityTransactionVo toCiEntityTransactionVo = new CiEntityTransactionVo();
                            toCiEntityTransactionVo.setCiEntityId(item.getFromCiEntityId());
                            toCiEntityTransactionVo.setCiId(item.getFromCiId());
                            toCiEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
                            toCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            toCiEntityTransactionVo.setTransactionId(transactionVo.getId());

                            // 保存快照
                            ciEntityService.createSnapshot(toCiEntityTransactionVo);

                            RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo(item, RelActionType.DELETE);
                            relEntityVo.setTransactionId(transactionVo.getId());

                            //写入配置项事务
                            transactionMapper.insertCiEntityTransaction(toCiEntityTransactionVo);
                            // 写入属性事务
                            transactionMapper.insertRelEntityTransaction(relEntityVo);

                            //真正修改数据
                            relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                                    item.getFromCiEntityId(), item.getToCiEntityId());
                        }
                    }
                });
            }
        }
        // 删除配置项相关信息
        ciEntityMapper.deleteCiEntityByCiId(ciId);
        // 删除事务相关信息
        transactionMapper.deleteTransactionByCiId(ciId);
        // 删除模型相关信息
        ciMapper.deleteCiById(ciId);
        //删除视图
        CiSchemaHandler.deleteCi(ciVo);

        return 1;
    }

    @Override
    public CiVo getCiById(Long id) {
        CiVo ciVo = ciMapper.getCiById(id);
        List<AttrVo> attrList = attrMapper.getAttrByCiId(id);
        List<RelVo> relList = relMapper.getRelByCiId(id);
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        return ciVo;
    }
}
