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

package neatlogic.module.cmdb.service.attr;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.TransactionStatus;
import neatlogic.framework.cmdb.exception.attr.*;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.transaction.core.EscapeTransactionJob;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AttrServiceImpl implements AttrService {
    @Resource
    private CiMapper ciMapper;
    @Resource
    private AttrMapper attrMapper;

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiSchemaMapper ciSchemaMapper;


    @Override
    @Transactional
    public void insertAttr(AttrVo attrVo) {
        if (attrMapper.checkAttrNameIsRepeat(attrVo) > 0) {
            throw new AttrNameRepeatException(attrVo.getName());
        }
        attrMapper.insertAttr(attrVo);
        CiVo ciVo = ciMapper.getCiById(attrVo.getCiId());
        //把模型信息设进去为了可以生成属性表名
        attrVo.setCiVo(ciVo);
        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        handler.afterInsert(attrVo);
        if (!handler.isNeedTargetCi()) {
            //由于以下操作是DDL操作，所以需要使用EscapeTransactionJob避开当前事务，否则在进行DDL操作之前事务就会提交，如果DDL出错，则上面的事务就无法回滚了
            int indexCount = ciSchemaMapper.getIndexCount(TenantContext.get().getDataDbName(), attrVo.getCiId());
            if (indexCount <= 60) {
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> ciSchemaMapper.insertAttrToCiTable(ciVo.getId(), ciVo.getCiTableName(), attrVo)).execute();
                if (!s.isSucceed()) {
                    throw new InsertAttrToSchemaException(attrVo.getName());
                }
            } else {
                throw new InsertAttrToSchemaException(attrVo.getName(), 60);
            }
        }
    }

    @Override
    @Transactional
    public void updateAttr(AttrVo attrVo) {
        AttrVo oldAttrVo = attrMapper.getAttrById(attrVo.getId());
        if (oldAttrVo == null) {
            throw new AttrNotFoundException(attrVo.getId());
        }
        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        if (handler.isNeedTargetCi() && !oldAttrVo.getTargetCiId().equals(attrVo.getTargetCiId())) {
            if (ciEntityMapper.getAttrEntityCountByFromCiIdAndAttrId(attrVo.getCiId(), attrVo.getId()) > 0) {
                throw new AttrIsUsedInTargetException();
            }
        }
        attrMapper.updateAttr(attrVo);
        CiVo ciVo = ciMapper.getCiById(attrVo.getCiId());
        //把模型信息设进去为了可以生成属性表名
        attrVo.setCiVo(ciVo);

        handler.afterUpdate(attrVo);
        if (!handler.isNeedTargetCi()) {
            //由于以下操作是DDL操作，所以需要使用EscapeTransactionJob避开当前事务
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                try {
                    if (ciSchemaMapper.checkColumnIsExists(TenantContext.get().getDataDbName(), attrVo.getCiId(), attrVo.getId()) == 0) {
                        ciSchemaMapper.insertAttrToCiTable(ciVo.getId(), ciVo.getCiTableName(), attrVo);
                    } else {
                        if (Objects.equals(attrVo.getIsSearchAble(), 1)) {
                            if (ciSchemaMapper.checkIndexIsExists(TenantContext.get().getDataDbName(), attrVo.getCiId(), attrVo.getId()) == 0) {
                                //创建索引
                                int indexCount = ciSchemaMapper.getIndexCount(TenantContext.get().getDataDbName(), attrVo.getCiId());
                                if (indexCount <= 60) {
                                    ciSchemaMapper.addAttrIndex(attrVo.getCiTableName(), attrVo.getId());
                                } else {
                                    throw new InsertAttrToSchemaException(attrVo.getName(), 60);
                                }
                            }
                        } else {
                            if (ciSchemaMapper.checkIndexIsExists(TenantContext.get().getDataDbName(), attrVo.getCiId(), attrVo.getId()) > 0) {
                                //删除索引
                                ciSchemaMapper.deleteAttrIndex(attrVo.getCiTableName(), attrVo.getId());
                            }
                        }
                    }
                } catch (Exception ex) {
                    //如果报重复列异常，代表列已存在，这种异常无需处理
                    if (!ex.getMessage().contains("Duplicate")) {
                        throw ex;
                    }
                }
            }).execute();
            //编辑属性只是尝试创建字段，如果创建不成功代表字段已经存在，所以无需处理执行结果
            if (!s.isSucceed()) {
                throw new ApiRuntimeException(s.getError());
            }
        }
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
        //检查是否被表达式属性引用
        List<AttrVo> attrList = attrMapper.getExpressionAttrByValueAttrId(attrVo.getId());
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (AttrVo eAttrVo : attrList) {
                if (eAttrVo.getCiId().equals(attrVo.getCiId())) {
                    throw new AttrIsUsedInExpressionException(eAttrVo);
                } else {
                    CiVo ciVo = ciMapper.getCiById(eAttrVo.getCiId());
                    throw new AttrIsUsedInExpressionException(ciVo, eAttrVo);
                }
            }
        }

        //所有操作确认无误后再异步补充其他配置项的删除事务数据
        List<CiVo> ciList = ciMapper.getDownwardCiListByLR(attrCi.getLft(), attrCi.getRht());
        for (CiVo ciVo : ciList) {
            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setCiId(ciVo.getId());
            ciEntityVo.setPageSize(100);
            ciEntityVo.setAttrIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
            ciEntityVo.setRelIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
            List<AttrFilterVo> attrFilterList = new ArrayList<>();
            AttrFilterVo attrFilterVo = new AttrFilterVo();
            attrFilterVo.setAttrId(attrVo.getId());
            attrFilterVo.setExpression(SearchExpression.NOTNULL.getExpression());
            attrFilterList.add(attrFilterVo);
            ciEntityVo.setAttrFilterList(attrFilterList);
            List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            while (CollectionUtils.isNotEmpty(ciEntityList)) {
                BatchRunner<CiEntityVo> runner = new BatchRunner<>();
                TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                //并发清理配置项数据，最高并发3个线程
                int parallel = 3;
                runner.execute(ciEntityList, parallel, item -> {
                    if (item != null && item.getAttrEntityData().containsKey("attr_" + attrVo.getId())) {
                        //写入事务
                        TransactionVo transactionVo = new TransactionVo();
                        transactionVo.setCiId(item.getCiId());
                        transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                        transactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                        transactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                        transactionMapper.insertTransaction(transactionVo);
                        //写入事务分组
                        transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionVo.getId());
                        //写入配置项事务
                        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                        ciEntityTransactionVo.setCiEntityId(item.getId());
                        ciEntityTransactionVo.setCiId(item.getCiId());
                        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                        ciEntityTransactionVo.setTransactionId(transactionVo.getId());
                        //不用join的目的是避免触碰到mysql的join数量上限
                        ciEntityTransactionVo.setOldCiEntityVo(ciEntityService.getCiEntityById(item.getCiId(), item.getId()));
                        //必须使用局部修改模式，这样不需要提供其他属性
                        ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
                        //创建修改信息
                        ciEntityTransactionVo.addAttrEntityData(attrVo);
                        // 创建旧配置项快照
                        ciEntityService.createSnapshot(ciEntityTransactionVo);

                        //写入配置项事务
                        transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);
                    }
                }, "ATTRENTITY-BATCH-DELETER");
                ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
                ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            }
        }

        //删除引用属性数据
        ciEntityMapper.deleteAttrEntityByAttrId(attrVo.getId());

        //删除模型属性
        attrMapper.deleteAttrById(attrVo.getId());

        //某些类型的属性可能有删除后续操作
        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        handler.afterDelete(attrVo);

        //物理删除字段
        //由于以上事务中的dml操作包含了以下ddl操作的表，如果使用 EscapeTransactionJob会导致事务等待产生死锁，所以这里不再使用EscapeTransactionJob去保证事务一致性。即使ddl删除字段失败，以上事务也会提交
        if (!attrVo.isNeedTargetCi()) {
            ciSchemaMapper.deleteAttrFromCiTable(attrVo.getCiId(), attrCi.getCiTableName(), attrVo);
        }


    }


}
