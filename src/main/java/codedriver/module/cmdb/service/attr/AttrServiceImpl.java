/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.attr;

import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.TransactionStatus;
import codedriver.framework.cmdb.exception.attr.AttrIsInvokedByExpressionException;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.attr.InsertAttrToSchemaException;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    private CiMapper ciMapper;
    @Autowired
    private AttrMapper attrMapper;


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
        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        if (!handler.isNeedTargetCi()) {
            //由于以下操作是DDL操作，所以需要使用EscapeTransactionJob避开当前事务
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> ciSchemaMapper.insertAttrToCiSchema(ciVo.getCiTableName(), attrVo)).execute();
            if (!s.isSucceed()) {
                throw new InsertAttrToSchemaException(attrVo.getName());
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

    /**
     * 删除模型属性，删除先需要补充所有配置项的修改事务
     * 1、计算属性所属模型是否有子模型
     * 2、如果有子模型则需要补充所有子模型配置项的事务数据
     *
     * @param attrVo 属性对象
     */
    @Override
    @Transactional
    public void deleteAttr(AttrVo attrVo) {
        CiVo attrCi = ciMapper.getCiById(attrVo.getCiId());
        //j检查是否被名字表达式引用
        if (StringUtils.isNotBlank(attrCi.getNameExpression()) && (attrCi.getNameExpression().contains("{" + attrVo.getName() + "}") || attrCi.getNameExpression().contains("{" + attrVo.getLabel() + "}"))) {
            throw new AttrIsInvokedByExpressionException();
        }
        //补充配置项删除当前属性的事务数据
        List<CiVo> ciList = ciMapper.getDownwardCiListByLR(attrCi.getLft(), attrCi.getRht());
        for (CiVo ciVo : ciList) {
            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setCiId(ciVo.getId());
            ciEntityVo.setPageSize(500);
            List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            BatchRunner<CiEntityVo> runner = new BatchRunner<>();
            TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
            //并发清理配置项数据，最高并发10个线程
            int parallel = 10;
            while (CollectionUtils.isNotEmpty(ciEntityList)) {
                runner.execute(ciEntityList, parallel, item -> {
                    if (item != null) {
                        //写入事务
                        TransactionVo transactionVo = new TransactionVo();
                        transactionVo.setCiId(item.getCiId());
                        transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
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
                    }
                });
                ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
                ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            }
        }
        //删除引用属性数据
        ciEntityMapper.deleteAttrEntityByAttrId(attrVo.getId());
        //删除模型属性
        attrMapper.deleteAttrById(attrVo.getId());
        //物理删除字段
        if (!attrVo.isNeedTargetCi()) {
            ciSchemaMapper.deleteAttrFromCiSchema(attrCi.getCiTableName(), attrVo);
        }
    }


}
