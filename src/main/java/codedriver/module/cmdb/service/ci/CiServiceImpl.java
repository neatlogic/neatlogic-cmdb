/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.ci;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.batch.BatchJob;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.cmdb.constvalue.TransactionStatus;
import codedriver.framework.exception.database.DataBaseNotFoundException;
import codedriver.framework.lrcode.LRCodeManager;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.module.cmdb.dto.transaction.TransactionVo;
import codedriver.module.cmdb.exception.ci.CiHasBeenExtendedException;
import codedriver.module.cmdb.exception.ci.CiHasRelException;
import codedriver.module.cmdb.exception.ci.CiIsNotEmptyException;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.service.rel.RelService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    private CiSchemaMapper ciSchemaMapper;

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    @Transactional
    public void insertCi(CiVo ciVo) {
        int lft = LRCodeManager.beforeAddTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getParentCiId());
        ciVo.setLft(lft);
        ciVo.setRht(lft + 1);
        ciMapper.insertCi(ciVo);

        //创建新表
        if (ciSchemaMapper.checkDatabaseIsExists(TenantContext.get().getDataDbName()) > 0) {
            //创建配置项表
            ciSchemaMapper.insertCiSchema(ciVo.getCiTableName());
            //创建配置项复杂属性表（不一定会用，先创建）
            //ciSchemaMapper.insertCiAttrSchema(ciVo.getAttrTableName());
        } else {
            throw new DataBaseNotFoundException();
        }
    }

    @Override
    public int deleteCi(Long ciId) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        //检查当前模型是否被继承
        List<CiVo> childCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        if (childCiList.size() > 1) {
            throw new CiHasBeenExtendedException(ciVo.getLabel(), childCiList.subList(1, childCiList.size()));
        }
        // 检查当前模型是否有被引用
        List<CiVo> fromCiList = ciMapper.getCiByToCiId(ciId);
        if (CollectionUtils.isNotEmpty(fromCiList)) {
            throw new CiHasRelException(
                    fromCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查模型是否有数据
        int ciEntityCount = ciEntityMapper.getCiEntityCountByCiId(ciId);
        if (ciEntityCount > 0) {
            throw new CiIsNotEmptyException(ciId, ciEntityCount);
        }

        //清理属性表
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        if (StringUtils.isNotBlank(ciVo.getCiTableName())) {
            ciSchemaMapper.deleteSchema(ciVo.getCiTableName());
        }

        //清楚模型数据
        ciMapper.deleteCiById(ciId);
        return 0;
    }

    @Deprecated
    public int deleteCi_bak(Long ciId) {
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
        ciSchemaMapper.deleteSchema(TenantContext.get().getDataDbName() + ".`cmdb_cientity_" + ciVo.getName().toLowerCase() + "`");
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
