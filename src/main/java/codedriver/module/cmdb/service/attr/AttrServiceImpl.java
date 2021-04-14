/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.attr;

import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.cmdb.constvalue.TransactionStatus;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.AttrType;
import codedriver.module.cmdb.exception.attr.AttrDeleteDeniedException;
import codedriver.module.cmdb.exception.attr.AttrNotFoundException;
import codedriver.module.cmdb.exception.attr.InsertAttrToSchemaException;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttrServiceImpl implements AttrService {
    private final static Logger logger = LoggerFactory.getLogger(AttrServiceImpl.class);

    @Autowired
    private CiMapper ciMapper;
    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiSchemaMapper ciSchemaMapper;


    @Override
    @Transactional
    public void insertAttr(AttrVo attrVo) {
        attrMapper.insertAttr(attrVo);
        CiVo ciVo = ciMapper.getCiById(attrVo.getCiId());
        //把模型信息设进去为了可以生成属性表名
        attrVo.setCiVo(ciVo);
        AttrType attrType = AttrType.get(attrVo.getType());
        if (attrType != null) {
            if (!attrType.isNeedTargetCi()) {
                //由于以下操作是DDL操作，所以需要使用EscapeTransactionJob避开当前事务
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> ciSchemaMapper.insertAttrToCiSchema(ciVo.getCiTableName(), attrVo)).execute();
                if (!s.isSucceed()) {
                    throw new InsertAttrToSchemaException(attrVo.getName());
                }
            }
        }

    }

    @Override
    @Transactional
    public void updateAttr(AttrVo attrVo) {
        if (attrMapper.getAttrById(attrVo.getId()) == null) {
            throw new AttrNotFoundException(attrVo.getId());
        }
        attrMapper.updateAttr(attrVo);
    }

    @Override
    @Transactional
    public void deleteAttrById(Long attrId) {
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }
        if (attrVo.getIsPrivate() != null && attrVo.getIsPrivate().equals(1)) {
            throw new AttrDeleteDeniedException();
        }
        CiVo ciVo = ciMapper.getCiById(attrVo.getCiId());
        //FIXME 检查是否被名字表达式引用


        //删除所有attrEntity属性值，需要生成事务
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(attrVo.getCiId());
        ciEntityVo.setPageSize(500);
        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        BatchRunner<CiEntityVo> runner = new BatchRunner<>();
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        //并发清理配置项数据，最高并发10个线程
        int parallel = 10;
        runner.execute(ciEntityList, parallel, item -> {
            if (item != null) {
                //写入事务
                TransactionVo transactionVo = new TransactionVo();
                transactionVo.setCiId(item.getCiId());
                transactionMapper.insertTransaction(transactionVo);
                //写入事务分组
                transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionVo.getId());
                //写入配置项事务
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(item.getId());
                ciEntityTransactionVo.setCiId(item.getCiId());
                ciEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                ciEntityTransactionVo.setTransactionId(transactionVo.getId());
                AttrEntityTransactionVo attrEntityTransactionVo = new AttrEntityTransactionVo();
                attrEntityTransactionVo.setAttrId(attrVo.getId());
                attrEntityTransactionVo.setCiEntityId(item.getId());
                attrEntityTransactionVo.setAttrName(attrVo.getName());
                attrEntityTransactionVo.setAttrLabel(attrVo.getLabel());
                attrEntityTransactionVo.setTransactionId(transactionVo.getId());

                // 保存快照
                ciEntityService.createSnapshot(ciEntityTransactionVo);

                //写入配置项事务
                transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);

                // 写入属性事务
                transactionMapper.insertAttrEntityTransaction(attrEntityTransactionVo);

                //真正修改数据
                AttrEntityVo attrEntityVo = new AttrEntityVo(attrEntityTransactionVo);
                attrEntityMapper.deleteAttrEntity(attrEntityVo);

                transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                transactionMapper.updateTransactionStatus(transactionVo);
            }
        });
        //删除模型属性
        attrMapper.deleteAttrById(attrVo.getId());

        //删除视图数据
        //CiSchemaHandler.deleteAttr(attrVo);
    }


}
