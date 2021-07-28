/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.ci;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.exception.attr.AttrIsUsedInExpressionException;
import codedriver.framework.cmdb.exception.attr.AttrIsUsedInNameAttrException;
import codedriver.framework.cmdb.exception.attr.AttrIsUsedInUniqueRuleException;
import codedriver.framework.cmdb.exception.ci.*;
import codedriver.framework.exception.database.DataBaseNotFoundException;
import codedriver.framework.lrcode.LRCodeManager;
import codedriver.framework.lrcode.constvalue.MoveType;
import codedriver.framework.transaction.core.AfterTransactionJob;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.service.rel.RelService;
import codedriver.module.cmdb.utils.RelUtil;
import codedriver.module.cmdb.utils.VirtualCiSqlBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }

        int lft = LRCodeManager.beforeAddTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getParentCiId());
        ciVo.setLft(lft);
        ciVo.setRht(lft + 1);
        ciMapper.insertCi(ciVo);

        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        } else if (Objects.equals(ciVo.getIsVirtual(), 0)) {
            EscapeTransactionJob.State s = buildCiSchema(ciVo);
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
            }
        }
    }

    private EscapeTransactionJob.State buildCiSchema(CiVo ciVo) {
        return new EscapeTransactionJob(() -> {
            if (ciSchemaMapper.checkSchemaIsExists(TenantContext.get().getDataDbName()) > 0) {
                if (ciSchemaMapper.checkTableIsExists(TenantContext.get().getDataDbName(), "cmdb_" + ciVo.getId()) <= 0) {
                    //创建配置项表
                    ciSchemaMapper.insertCiTable(ciVo.getCiTableName());
                } else {
                    //如果已存在但没有数据，重建表
                    if (ciSchemaMapper.checkTableHasData(ciVo.getCiTableName()) <= 0) {
                        ciSchemaMapper.deleteCiTable(ciVo.getCiTableName());
                        ciSchemaMapper.insertCiTable(ciVo.getCiTableName());
                        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                        for (AttrVo attrVo : attrList) {
                            //这里的attrlist包含了所有集成模型的属性，不是自己模型的属性就不要添加
                            if (attrVo.getCiId().equals(ciVo.getId()) && attrVo.getTargetCiId() == null) {
                                ciSchemaMapper.insertAttrToCiTable(ciVo.getCiTableName(), attrVo);
                            }
                        }
                    }
                }
            } else {
                throw new DataBaseNotFoundException();
            }
        }).execute();
    }

    private void buildCiView(CiVo ciVo) {
        VirtualCiSqlBuilder viewBuilder = new VirtualCiSqlBuilder(ciVo.getViewXml());
        viewBuilder.setCiId(ciVo.getId());

        if (viewBuilder.valid()) {
            //测试一下语句是否能正常执行
            try {
                ciSchemaMapper.testCiViewSql(viewBuilder.getTestSql());
            } catch (Exception ex) {
                throw new CiViewSqlIrregularException(ex);
            }
            List<AttrVo> attrList = viewBuilder.getAttrList();
            if (CollectionUtils.isNotEmpty(attrList)) {
                Map<String, Long> attrIdMap = new HashMap<>();
                for (AttrVo attrVo : attrList) {
                    attrVo.setCiId(ciVo.getId());
                    attrMapper.insertAttr(attrVo);
                    attrIdMap.put(attrVo.getName(), attrVo.getId());
                }
                viewBuilder.setAttrIdMap(attrIdMap);
                EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                    if (ciSchemaMapper.checkSchemaIsExists(TenantContext.get().getDataDbName()) > 0) {
                        //创建配置项表
                        ciSchemaMapper.insertCiView(viewBuilder.getCreateViewSql());
                    } else {
                        throw new DataBaseNotFoundException();
                    }
                }).execute();
                if (!s.isSucceed()) {
                    throw new CreateCiSchemaException(ciVo.getName(), true);
                }
            }
        }
    }

    @Override
    @Transactional
    public void updateCiUnique(Long ciId, List<Long> attrIdList) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        ciMapper.deleteCiUniqueByCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrIdList)) {
            for (Long attrId : attrIdList) {
                if (attrList.stream().noneMatch(attr -> attr.getId().equals(attrId))) {
                    throw new CiUniqueRuleHasNotExistsAttrException(attrId);
                }
                ciMapper.insertCiUnique(ciId, attrId);
            }
        }
    }

    @Override
    @Transactional
    public void updateCiNameAttrId(CiVo ciVo) {
        ciMapper.updateCiNameAttrId(ciVo);
        AfterTransactionJob<CiVo> job = new AfterTransactionJob<>();
        job.execute(ciVo, dataCiVo -> {
            Thread.currentThread().setName("UPDATE-CIENTITY-NAME-" + dataCiVo.getId());
            ciEntityService.updateCiEntityNameForCi(dataCiVo);
        });
    }

    @Override
    @Transactional
    public void updateCiNameExpression(Long ciId, String nameExpression) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        CiVo ciVo = ciMapper.getCiById(ciId);
        //ciMapper.deleteCiNameExpressionByCiId(ciId);
       /* if (!nameExpression.equals(ciVo.getNameExpression())) {
            if (StringUtils.isNotEmpty(nameExpression)) {
                //检查表达式中所有属性是否存在当前模型的属性列表里
                String regex = "\\{([^}]+?)}";
                Matcher matcher = Pattern.compile(regex).matcher(nameExpression);
                Set<String> labelSet = new HashSet<>();
                while (matcher.find()) {
                    labelSet.add(matcher.group(1));
                }
                for (String label : labelSet) {
                    Optional<AttrVo> opAttr = attrList.stream().filter(attr -> attr.getName().equalsIgnoreCase(label)).findFirst();
                    if (!opAttr.isPresent()) {
                        throw new CiNameExpressionHasNotExistsAttrException(label);
                    } else {
                        AttrVo attrVo = opAttr.get();
                        if (!attrVo.getType().equals("text")) {
                            throw new CiNameExpressionAttrTypeNotSupportedException(label);
                        }
                        ciMapper.insertCiNameExpression(ciId, opAttr.get().getId());
                    }
                }
            }
            ciMapper.updateCiNameExpression(ciId, nameExpression);
            //修正配置项的名字表达式
            ciVo.setNameExpression(nameExpression);
            AfterTransactionJob<CiVo> job = new AfterTransactionJob<>();
            job.execute(ciVo, dataCiVo -> {
                Thread.currentThread().setName("UPDATE-CIENTITY-NAME-" + dataCiVo.getId());
                ciEntityService.updateCiEntityName(dataCiVo);
            });
        }*/

    }

    @Override
    @Transactional
    public void updateCi(CiVo ciVo) {
        CiVo checkCiVo = ciMapper.getCiById(ciVo.getId());
        if (checkCiVo == null) {
            throw new CiNotFoundException(ciVo.getId());
        }
        if (!Objects.equals(checkCiVo.getParentCiId(), ciVo.getParentCiId())) {
            //如果继承发生改变需要检查是否有配置项数据，有数据不允许变更
            int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
            if (ciEntityCount > 0) {
                throw new CiParentCanNotBeChangedException(ciVo.getName(), ciEntityCount);
            }

            List<AttrVo> parentAttrList = attrMapper.getAttrByCiId(checkCiVo.getParentCiId());
            if (CollectionUtils.isNotEmpty(parentAttrList)) {
                //检查子模型的表达式属性是否引用了父模型的属性
                List<AttrVo> attrExpressionList = attrMapper.getExpressionAttrByValueCiIdAndAttrIdList(ciVo.getId(), parentAttrList.stream().map(AttrVo::getId).collect(Collectors.toList()));
                attrExpressionList.removeIf(attr -> !attr.getCiId().equals(ciVo.getId()));
                if (CollectionUtils.isNotEmpty(attrExpressionList)) {
                    throw new AttrIsUsedInExpressionException(attrExpressionList);
                }

                //检查唯一规则是否有被引用
                if (CollectionUtils.isNotEmpty(checkCiVo.getUniqueAttrIdList()) && checkCiVo.getUniqueAttrIdList().stream().anyMatch(id -> parentAttrList.stream().anyMatch(attr -> attr.getId().equals(id)))) {
                    throw new AttrIsUsedInUniqueRuleException();
                }

                //检查名称属性是否有被引用
                if (checkCiVo.getNameAttrId() != null && parentAttrList.stream().anyMatch(a -> a.getId().equals(checkCiVo.getNameAttrId()))) {
                    throw new AttrIsUsedInNameAttrException();
                }
            }


            LRCodeManager.moveTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getId(), MoveType.INNER, ciVo.getParentCiId());
        }
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }
        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            buildCiView(ciVo);
        } else if (Objects.equals(ciVo.getIsVirtual(), 0)) {
            EscapeTransactionJob.State s = buildCiSchema(ciVo);
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
            }
        }
        ciMapper.updateCi(ciVo);
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
        List<CiVo> toCiList = ciMapper.getCiByFromCiId(ciId);
        if (CollectionUtils.isNotEmpty(toCiList)) {
            throw new CiHasRelException(
                    toCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查是否被属性引用
        List<CiVo> attrCiList = ciMapper.getCiByTargetCiId(ciId);
        if (CollectionUtils.isNotEmpty(attrCiList)) {
            throw new CiHasAttrException(
                    attrCiList.stream().map(CiVo::getLabel).collect(Collectors.joining("、")));
        }
        //检查模型以及子模型是否有数据
        int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
        if (ciEntityCount > 0) {
            throw new CiIsNotEmptyException(ciId, ciEntityCount);
        }

        if (StringUtils.isNotBlank(ciVo.getCiTableName())) {
            if (ciVo.getIsVirtual().equals(0)) {
                ciSchemaMapper.deleteCiTable(ciVo.getCiTableName());
            } else {
                ciSchemaMapper.deleteCiView(ciVo.getCiTableName());
            }
        }

        //清除模型数据
        LRCodeManager.beforeDeleteTreeNode("cmdb_ci", "id", "parent_ci_id", ciId);
        ciMapper.deleteCiById(ciId);
        return 0;
    }


    @Override
    public CiVo getCiById(Long id) {
        CiVo ciVo = ciMapper.getCiById(id);
        if (ciVo.getIsVirtual().equals(0)) {
            ciVo.setUpwardCiList(ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(id);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(id));
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        return ciVo;
    }

    @Override
    public CiVo getCiByName(String ciName) {
        CiVo ciVo = ciMapper.getCiByName(ciName);
        if (ciVo == null) {
            throw new CiNotFoundException(ciName);
        }
        if (ciVo.getIsVirtual().equals(0)) {
            ciVo.setUpwardCiList(ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht()));
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
        return ciVo;
    }
}
