/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.ci;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.exception.ci.*;
import codedriver.framework.exception.database.DataBaseNotFoundException;
import codedriver.framework.lrcode.LRCodeManager;
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
import codedriver.module.cmdb.utils.ViewSqlBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            ViewSqlBuilder viewBuilder = new ViewSqlBuilder(ciVo.getViewXml());
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
                        if (ciSchemaMapper.checkDatabaseIsExists(TenantContext.get().getDataDbName()) > 0) {
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
        } else {
            EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
                if (ciSchemaMapper.checkDatabaseIsExists(TenantContext.get().getDataDbName()) > 0) {
                    //创建配置项表
                    ciSchemaMapper.insertCiSchema(ciVo.getCiTableName());
                } else {
                    throw new DataBaseNotFoundException();
                }
            }).execute();
            if (!s.isSucceed()) {
                throw new CreateCiSchemaException(ciVo.getName());
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
    public void updateCiNameExpression(Long ciId, String nameExpression) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        CiVo ciVo = ciMapper.getCiById(ciId);
        ciMapper.deleteCiNameExpressionByCiId(ciId);
        if (!nameExpression.equals(ciVo.getNameExpression())) {
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
        }

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
        }
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }
        int lft = LRCodeManager.beforeAddTreeNode("cmdb_ci", "id", "parent_ci_id", ciVo.getParentCiId());
        ciVo.setLft(lft);
        ciVo.setRht(lft + 1);
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
        //检查模型以及子模型是否有数据
        int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(ciVo.getLft(), ciVo.getRht());
        if (ciEntityCount > 0) {
            throw new CiIsNotEmptyException(ciId, ciEntityCount);
        }

        if (StringUtils.isNotBlank(ciVo.getCiTableName())) {
            if (ciVo.getIsVirtual().equals(0)) {
                ciSchemaMapper.deleteSchema(ciVo.getCiTableName());
            } else {
                ciSchemaMapper.deleteView(ciVo.getCiTableName());
            }
        }

        //清楚模型数据
        ciMapper.deleteCiById(ciId);
        return 0;
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
