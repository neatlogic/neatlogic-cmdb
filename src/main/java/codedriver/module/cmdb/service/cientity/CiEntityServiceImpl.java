package codedriver.module.cmdb.service.cientity;

import codedriver.framework.batch.BatchJob;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.constvalue.*;
import codedriver.framework.cmdb.validator.core.IValidator;
import codedriver.framework.cmdb.validator.core.ValidatorFactory;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.util.Md5Util;
import codedriver.module.cmdb.cischema.CiSchemaHandler;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntitySnapshotMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.dto.transaction.*;
import codedriver.module.cmdb.exception.cientity.*;
import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CiEntityServiceImpl implements CiEntityService {
    // private final static Logger logger = LoggerFactory.getLogger(CiEntityServiceImpl.class);

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CiEntitySnapshotMapper ciEntitySnapshotMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

    @Override
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo) {
        List<Long> ciEntityIdList = ciEntityMapper.searchCiEntityId(ciEntityVo);
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            if (ciEntityVo.getNeedPage()) {
                int rowNum = ciEntityMapper.searchCiEntityIdCount(ciEntityVo);
                ciEntityVo.setRowNum(rowNum);
                ciEntityVo.setPageCount(PageUtil.getPageCount(rowNum, ciEntityVo.getPageSize()));
            }
            ciEntityList = searchCiEntityByIds(ciEntityIdList, ciEntityVo);
        }
        return ciEntityList;
    }

    @Override
    public List<CiEntityVo> searchCiEntityByIds(List<Long> ciEntityIdList, CiEntityVo ciEntityVo) {
        List<CiEntityVo> ciEntityList = ciEntityMapper.searchCiEntityByIdList(ciEntityIdList);

        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            List<AttrEntityVo> attrEntityList =
                    attrEntityMapper.searchAttrEntityByCiEntityIdList(ciEntityIdList, null);
            List<RelEntityVo> relEntityList =
                    relEntityMapper.searchRelEntityByCiEntityIdList(ciEntityIdList, null);
            for (CiEntityVo entity : ciEntityList) {
                Iterator<AttrEntityVo> itAttrEntity = attrEntityList.iterator();
                while (itAttrEntity.hasNext()) {
                    AttrEntityVo attrEntity = itAttrEntity.next();
                    if (attrEntity.getCiEntityId().equals(entity.getId())) {
                        entity.addAttrEntity(attrEntity);
                        itAttrEntity.remove();
                    }
                }
                // 一个关系可能被多个配置项引用，所以不能使用属性的处理方式来处理
                for (RelEntityVo relEntity : relEntityList) {
                    if (relEntity.getFromCiEntityId().equals(entity.getId())
                            && relEntity.getDirection().equals(RelDirectionType.FROM.getValue())
                            || relEntity.getToCiEntityId().equals(entity.getId())
                            && relEntity.getDirection().equals(RelDirectionType.TO.getValue())) {
                        entity.addRelEntity(relEntity);
                    }
                }
            }
        }
        return ciEntityList;
    }

    @Transactional
    @Override
    public Long deleteCiEntity(Long ciEntityId) {
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityById(ciEntityId);

        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        //List<AttrEntityVo> attrEntityList = attrEntityMapper.getAttrEntityByCiEntityId(ciEntityId);
        //List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(ciEntityId);

        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(ciEntityVo.getCiId());
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(ciEntityVo);
        ciEntityTransactionVo.setAction(TransactionActionType.DELETE.getValue());
        ciEntityTransactionVo.setTransactionId(transactionVo.getId());
        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);
        /*// 写入旧属性值到事务对象中
        List<AttrEntityTransactionVo> attrEntityTransactionList = new ArrayList<>();
        for (AttrEntityVo attrEntityVo : attrEntityList) {
            attrEntityTransactionList.add(new AttrEntityTransactionVo(attrEntityVo));
        }
        ciEntityTransactionVo.setAttrEntityTransactionList(attrEntityTransactionList);

        // 写入旧关系到事务对象中
        List<RelEntityTransactionVo> relEntityTransactionList = new ArrayList<>();
        for (RelEntityVo relEntityVo : relEntityList) {
            relEntityTransactionList.add(new RelEntityTransactionVo(relEntityVo));
        }
        ciEntityTransactionVo.setRelEntityTransactionList(relEntityTransactionList);*/

        // 保存快照
        createSnapshot(ciEntityTransactionVo);

        // 写入事务
        transactionMapper.insertTransaction(transactionVo);
        // 写入配置项事务
        transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);
        commitTransaction(transactionVo, new TransactionGroupVo());

        //清理视图数据
        CiSchemaHandler.deleteCiEntity(ciEntityVo);

        return transactionVo.getId();

    }

    @Override
    public boolean validateCiEntity(CiEntityTransactionVo ciEntityTransactionVo) {
        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(ciEntityTransactionVo.getCiId());

        ciEntityTransactionVo.setAction(ciEntityTransactionVo.getTransactionMode().getValue());
        ciEntityTransactionVo.setTransactionId(transactionVo.getId());

        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);

        return validateCiEntityTransaction(ciEntityTransactionVo);
    }

    @Override
    @Transactional
    public Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList) {
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
            for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
                Long transactionId = saveCiEntity(ciEntityTransactionVo, transactionGroupVo);
                transactionGroupVo.addTransactionId(transactionId);
            }
        }
        if (CollectionUtils.isNotEmpty(transactionGroupVo.getTransactionIdList())) {
            for (Long transactionId : transactionGroupVo.getTransactionIdList()) {
                transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionId);
            }
            return transactionGroupVo.getId();
        }
        return 0L;
    }

    @Transactional
    @Override
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo) {
        return saveCiEntity(ciEntityTransactionVo, new TransactionGroupVo());
    }


    private Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo, TransactionGroupVo transactionGroupVo) {
        TransactionActionType action = ciEntityTransactionVo.getTransactionMode();
        if (action.equals(TransactionActionType.UPDATE)) {
            CiEntityVo checkCiEntityVo = ciEntityMapper.getCiEntityById(ciEntityTransactionVo.getCiEntityId());
            // 正在编辑中的配置项，在事务提交或删除前不允许再次修改
            if (checkCiEntityVo != null && checkCiEntityVo.getIsLocked().equals(1)) {
                throw new CiEntityIsLockedException(ciEntityTransactionVo.getCiEntityId());
            }
        }

        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(ciEntityTransactionVo.getCiId());

        ciEntityTransactionVo.setAction(action.getValue());
        ciEntityTransactionVo.setTransactionId(transactionVo.getId());

        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);
        boolean hasChange = validateCiEntityTransaction(ciEntityTransactionVo);

        if (hasChange) {
            // 计算新的配置项名称
            createCiEntityName(ciEntityTransactionVo);

            // 保存快照
            createSnapshot(ciEntityTransactionVo);

            // 写入事务
            transactionMapper.insertTransaction(transactionVo);
            // 写入配置项事务
            transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);
            // 写入属性事务
            if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityTransactionList())) {
                for (AttrEntityTransactionVo attrEntityTransactionVo : ciEntityTransactionVo
                        .getAttrEntityTransactionList()) {
                    attrEntityTransactionVo.setTransactionId(transactionVo.getId());
                    transactionMapper.insertAttrEntityTransaction(attrEntityTransactionVo);
                }
            }
            // 写入关系事务
            if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityTransactionList())) {
                for (RelEntityTransactionVo relEntityTransactionVo : ciEntityTransactionVo
                        .getRelEntityTransactionList()) {
                    relEntityTransactionVo.setTransactionId(transactionVo.getId());
                    transactionMapper.insertRelEntityTransaction(relEntityTransactionVo);
                }
            }
            commitTransaction(transactionVo, transactionGroupVo);
            //更新视图数据
            CiSchemaHandler.updateCiEntity(new CiEntityVo(ciEntityTransactionVo));
            return transactionVo.getId();
        } else {
            // 没有任何变化则返回零
            return 0L;
        }
    }

    @Override
    public void createCiEntityName(CiEntityTransactionVo ciEntityTransactionVo) {
        CiEntityVo ciEntityVo = new CiEntityVo(ciEntityTransactionVo);
        ciEntityVo.setAttrEntityList(new ArrayList<>());
        List<AttrEntityVo> attrEntityList =
                attrEntityMapper.getAttrEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId());
        if (CollectionUtils.isNotEmpty(attrEntityList)) {
            for (AttrEntityTransactionVo entity : ciEntityTransactionVo.getAttrEntityTransactionList()) {
                AttrEntityVo oldEntity = null;
                Optional<AttrEntityVo> optionAttrEntity =
                        attrEntityList.stream().filter(e -> e.getAttrId().equals(entity.getAttrId())).findFirst();
                if (optionAttrEntity.isPresent()) {
                    oldEntity = optionAttrEntity.get();
                }
                AttrEntityVo newEntity = new AttrEntityVo(entity);
                if (oldEntity != null) {
                    newEntity.setAttrType(oldEntity.getAttrType());
                } else {
                    AttrVo attr = attrMapper.getAttrById(entity.getAttrId());
                    newEntity.setAttrType(attr.getType());
                }
                ciEntityVo.getAttrEntityList().add(newEntity);
            }
        }
        ciEntityTransactionVo.setName(ciEntityVo.getName());
    }

    @Override
    public void createSnapshot(CiEntityTransactionVo ciEntityTransactionVo) {
        CiEntityVo ciEntityVo = new CiEntityVo(ciEntityTransactionVo);
        ciEntityVo.setAttrEntityList(attrEntityMapper.getAttrEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId()));
        ciEntityVo.setRelEntityList(relEntityMapper.getRelEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId()));
        String content = JSON.toJSONString(ciEntityVo);
        String hash = Md5Util.encryptMD5(content);
        ciEntitySnapshotMapper.replaceSnapshotContent(hash, content);
        ciEntityTransactionVo.setSnapshotHash(hash);
    }

    private boolean validateCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityTransactionVo.getCiId());
        List<RelVo> relList = relMapper.getRelByCiId(ciEntityTransactionVo.getCiId());

        List<AttrEntityVo> oldAttrEntityList =
                attrEntityMapper.getAttrEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId());

        List<RelEntityVo> oldRelEntityList =
                relEntityMapper.getRelEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId());

        // 清除和模型属性不匹配的属性
        if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityTransactionList())) {
            Iterator<AttrEntityTransactionVo> itAttr = ciEntityTransactionVo.getAttrEntityTransactionList().iterator();
            while (itAttr.hasNext()) {
                boolean isExists = false;
                AttrEntityTransactionVo attrEntityVo = itAttr.next();
                for (AttrVo attrVo : attrList) {
                    if (attrVo.getId().equals(attrEntityVo.getAttrId())) {
                        isExists = true;
                        // 补充attrName和attrLabel
                        attrEntityVo.setAttrName(attrVo.getName());
                        attrEntityVo.setAttrLabel(attrVo.getLabel());
                        // 补充属性处理器信息（后面需要用处理器根据原始value生成值hash）
                        attrEntityVo.setPropHandler(attrVo.getPropHandler());
                        break;
                    }
                }
                if (!isExists) {
                    itAttr.remove();
                }
            }
        }

        // 消除和模型属性不匹配的关系
        if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityTransactionList())) {
            Iterator<RelEntityTransactionVo> itRel = ciEntityTransactionVo.getRelEntityTransactionList().iterator();
            while (itRel.hasNext()) {
                boolean isExists = false;
                RelEntityTransactionVo relEntityVo = itRel.next();
                for (RelVo relVo : relList) {
                    if (relVo.getId().equals(relEntityVo.getRelId())) {
                        isExists = true;
                        //补充信息，反向生成配置项事务时需要用到
                        relEntityVo.setFromCiId(relVo.getFromCiId());
                        relEntityVo.setToCiId(relVo.getToCiId());
                        break;
                    }
                }
                if (!isExists) {
                    itRel.remove();
                }
            }
        }

        for (AttrVo attrVo : attrList) {
            // 属性值类型是自定义或属性定义的才需要进行值校验
            if (attrVo.getType().equals(AttrType.CUSTOM.getValue())
                    || attrVo.getType().equals(AttrType.PROPERTY.getValue())) {
                AttrEntityTransactionVo attrEntityTransactionVo =
                        ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrVo.getId());
                // 属性必填校验
                if (attrVo.getIsRequired().equals(1)) {
                    // 全局模式如果新属性不存在代表删除，如果发现没值直接抛出异常
                    if (ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
                        if (attrEntityTransactionVo == null
                                || (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.REPLACE.getValue())
                                && CollectionUtils.isEmpty(attrEntityTransactionVo.getValueList()))) {
                            throw new AttrEntityNotFoundException(attrVo.getLabel());
                        }
                    }
                }

                // 检查属性是否唯一
                if (attrVo.getIsUnique().equals(1)) {
                    if (attrEntityTransactionVo != null
                            && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())) {
                        // 替换模式，直接用新属性检查
                        if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.REPLACE.getValue())) {
                            AttrEntityVo newAttrEntityVo = new AttrEntityVo(attrEntityTransactionVo);
                            List<AttrEntityVo> otherAttrEntityList =
                                    attrEntityMapper.getAttrEntityByAttrIdAndValue(newAttrEntityVo);
                            if (otherAttrEntityList.contains(newAttrEntityVo)) {
                                throw new AttrEntityDuplicateException(attrVo.getLabel(),
                                        attrEntityTransactionVo.getValueList());
                            }
                            // 合并模式，使用旧值+新值检查
                        } else if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.MERGE.getValue())) {
                            AttrEntityVo oldAttrEntityVo = attrEntityMapper.getAttrEntityByCiEntityIdAndAttrId(
                                    attrEntityTransactionVo.getCiEntityId(), attrEntityTransactionVo.getAttrId());
                            if (oldAttrEntityVo != null) {
                                // 把老属性的值和事务添加的值进行Merge，后面比较是否有变化
                                for (String ov : oldAttrEntityVo.getValueList()) {
                                    attrEntityTransactionVo.addValue(ov);
                                }

                                AttrEntityVo newAttrEntityVo = new AttrEntityVo(attrEntityTransactionVo);
                                List<AttrEntityVo> otherAttrEntityList =
                                        attrEntityMapper.getAttrEntityByAttrIdAndValue(newAttrEntityVo);
                                if (otherAttrEntityList.contains(newAttrEntityVo)) {
                                    throw new AttrEntityDuplicateException(attrVo.getLabel(),
                                            attrEntityTransactionVo.getValueList());
                                }
                            }
                        }
                    }
                }
                // 调用校验器校验数据合法性
                if (attrEntityTransactionVo != null && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())
                        && StringUtils.isNotBlank(attrVo.getValidatorHandler())) {
                    IValidator validator = ValidatorFactory.getValidator(attrVo.getValidatorHandler());
                    if (validator != null) {
                        validator.valid(attrVo.getLabel(), attrEntityTransactionVo.getValueList(),
                                attrVo.getValidatorId());
                    }
                }
            }
        }

        // 校验关系信息
        for (RelVo relVo : relList) {
            // 判断当前配置项处于from位置的规则
            List<RelEntityTransactionVo> fromRelEntityTransactionList =
                    ciEntityTransactionVo.getRelEntityTransactionByRelId(relVo.getId(), RelDirectionType.FROM.getValue());
            // 判断当前配置项处于to位置的规则
            List<RelEntityTransactionVo> toRelEntityTransactionList =
                    ciEntityTransactionVo.getRelEntityTransactionByRelId(relVo.getId(), RelDirectionType.TO.getValue());

            // 标记当前模型是在关系的上端或者下端
            boolean isFrom = false;
            boolean isTo = false;
            if (relVo.getFromCiId().equals(ciEntityTransactionVo.getCiId())) {
                isFrom = true;
            }
            if (relVo.getToCiId().equals(ciEntityTransactionVo.getCiId())) {
                isTo = true;
            }

            if (ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {

                // 全局模式下，不存在关系信息代表删除，需要校验必填规则
                if (CollectionUtils.isEmpty(fromRelEntityTransactionList)) {
                    if (isFrom) {
                        if (RelRuleType.OO.getValue().equals(relVo.getToRule())
                                || RelRuleType.ON.getValue().equals(relVo.getToRule())) {
                            throw new RelEntityNotFoundException(relVo.getToLabel());
                        }
                    }
                } else if (fromRelEntityTransactionList.size() > 1) {
                    // 检查关系是否允许重复
                    if (RelRuleType.ZO.getValue().equals(relVo.getToRule())
                            || RelRuleType.OO.getValue().equals(relVo.getToRule())) {
                        throw new RelEntityMutipleException(relVo.getToLabel());
                    }
                }

                // 全局模式下，不存在关系信息代表删除，需要校验必填规则
                if (CollectionUtils.isEmpty(toRelEntityTransactionList)) {
                    if (isTo) {
                        if (RelRuleType.OO.getValue().equals(relVo.getFromRule())
                                || RelRuleType.ON.getValue().equals(relVo.getFromRule())) {
                            throw new RelEntityNotFoundException(relVo.getFromLabel());
                        }
                    }
                } else if (toRelEntityTransactionList.size() > 1) {
                    // 检查关系是否允许重复
                    if (RelRuleType.ZO.getValue().equals(relVo.getFromRule())
                            || RelRuleType.OO.getValue().equals(relVo.getFromRule())) {
                        throw new RelEntityMutipleException(relVo.getFromLabel());
                    }
                }
            } else if (ciEntityTransactionVo.getEditMode().equals(EditModeType.PARTIAL.getValue())) {
                if (CollectionUtils.isNotEmpty(fromRelEntityTransactionList)) {
                    if (fromRelEntityTransactionList.size() == 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.ZO.getValue().equals(relVo.getToRule())
                                || RelRuleType.OO.getValue().equals(relVo.getToRule())) {
                            // 需要提取已有的关系信息判断是否有重复
                            List<RelEntityVo> fromRelEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(
                                    ciEntityTransactionVo.getCiEntityId(), relVo.getId());
                            if ((fromRelEntityList.size() == 1
                                    && !fromRelEntityList.contains(new RelEntityVo(fromRelEntityTransactionList.get(0))))
                                    || fromRelEntityList.size() > 1) {
                                throw new RelEntityMutipleException(relVo.getToLabel());
                            }
                        }
                    } else if (fromRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.ZO.getValue().equals(relVo.getToRule())
                                || RelRuleType.OO.getValue().equals(relVo.getToRule())) {
                            throw new RelEntityMutipleException(relVo.getToLabel());
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(toRelEntityTransactionList)) {
                    if (toRelEntityTransactionList.size() == 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.ZO.getValue().equals(relVo.getFromRule())
                                || RelRuleType.OO.getValue().equals(relVo.getFromRule())) {
                            // 需要提取已有的关系信息判断是否有重复
                            List<RelEntityVo> toRelEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(
                                    ciEntityTransactionVo.getCiEntityId(), relVo.getId());
                            if ((toRelEntityList.size() == 1
                                    && !toRelEntityList.contains(new RelEntityVo(toRelEntityTransactionList.get(0))))
                                    || toRelEntityList.size() > 1) {
                                throw new RelEntityMutipleException(relVo.getFromLabel());
                            }
                        }
                    } else if (fromRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.ZO.getValue().equals(relVo.getFromRule())
                                || RelRuleType.OO.getValue().equals(relVo.getFromRule())) {
                            throw new RelEntityMutipleException(relVo.getFromLabel());
                        }
                    }
                }
            }
        }

        // 去掉没变化的属性修改
        boolean hasChange = false;
        if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityTransactionList())) {
            if (CollectionUtils.isNotEmpty(oldAttrEntityList)) {
                List<AttrEntityTransactionVo> oldAttrEntityTransactionList =
                        oldAttrEntityList.stream().map(AttrEntityTransactionVo::new).collect(Collectors.toList());
                // 去掉没变化的修改
                ciEntityTransactionVo.getAttrEntityTransactionList().removeAll(oldAttrEntityTransactionList);
                if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityTransactionList())) {
                    hasChange = true;
                }

            } else {
                hasChange = true;
            }
        }

        List<RelEntityTransactionVo> newRelEntityTransactionList = ciEntityTransactionVo.getRelEntityTransactionList();
        if (newRelEntityTransactionList == null) {
            newRelEntityTransactionList = new ArrayList<>();
            ciEntityTransactionVo.setRelEntityTransactionList(newRelEntityTransactionList);
        }

        // 需要删除的关系列表
        List<RelEntityTransactionVo> needDeleteRelEntityTransactionList = null;
        // 全局修改模式下，事务中不包含的关系代表要删除
        if (ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
            if (CollectionUtils.isNotEmpty(oldRelEntityList)) {
                // 先标记所有旧关系为需删除
                needDeleteRelEntityTransactionList = oldRelEntityList.stream()
                        .map(t -> new RelEntityTransactionVo(t, RelActionType.DELETE)).collect(Collectors.toList());
                // 事务中存在的关系则排除出需删除列表
                needDeleteRelEntityTransactionList.removeAll(newRelEntityTransactionList);
            }
        }

        // 排除掉没变化的关系
        List<RelEntityTransactionVo> sameRelEntityTransactionList =
                oldRelEntityList.stream().map(RelEntityTransactionVo::new).collect(Collectors.toList());
        newRelEntityTransactionList.removeAll(sameRelEntityTransactionList);

        if (CollectionUtils.isNotEmpty(needDeleteRelEntityTransactionList)) {
            newRelEntityTransactionList.addAll(needDeleteRelEntityTransactionList);
        }

        if (CollectionUtils.isNotEmpty(newRelEntityTransactionList)) {
            //补充关系两端cientityName
            for (RelEntityTransactionVo relEntityTransactionVo : newRelEntityTransactionList) {
                if (relEntityTransactionVo.getFromCiEntityId() != null) {
                    CiEntityVo cientity = ciEntityMapper.getCiEntityById(relEntityTransactionVo.getFromCiEntityId());
                    if (cientity != null) {
                        relEntityTransactionVo.setFromCiEntityName(cientity.getName());
                    }
                }
                if (relEntityTransactionVo.getToCiEntityId() != null) {
                    CiEntityVo cientity = ciEntityMapper.getCiEntityById(relEntityTransactionVo.getToCiEntityId());
                    if (cientity != null) {
                        relEntityTransactionVo.setToCiEntityName(cientity.getName());
                    }
                }
            }

            hasChange = true;
        }
        return hasChange;
    }

    /**
     * @param @param  transactionVo
     * @param @return
     * @return Long 配置项id
     * @Author: chenqiwei
     * @Time: Sep 2, 2020
     * @Description: 提交事务 修改规则:
     * editMode针对单次修改。editMode=Global下，提供属性或关系代表修改，不提供代表删除；editMode=Partial下，提供属性或关系代表修改，不提供代表不修改
     * saveMode针对单个属性或关系，saveMode=replace，代表覆盖，最后结果是修改或删除；saveMode=merge，代表合并，最后结果是添加新成员或维持不变。
     */
    private Long commitTransaction(TransactionVo transactionVo, TransactionGroupVo transactionGroupVo) {
        CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
        // 删除配置项
        if (ciEntityTransactionVo.getAction().equals(TransactionActionType.DELETE.getValue())) {
            //补充对端配置项事务信息
            List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId());
            BatchRunner<RelEntityVo> runner = new BatchRunner<>();
            int parallel = 10;
            //记录对端配置项的事务，如果同一个配置项则不需要添加多个事务
            Map<Long, Long> ciEntityTransactionMap = new HashMap<>();
            runner.execute(relEntityList, parallel, new BatchJob<RelEntityVo>() {
                @Override
                public void execute(RelEntityVo item) {
                    //如果不是自己引用自己，则需要补充对端配置项事务，此块需要在真正删除数据前处理
                    if (!item.getFromCiEntityId().equals(item.getToCiEntityId())) {
                        Long ciEntityId = null, ciId = null;

                        if (item.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            ciEntityId = item.getToCiEntityId();
                            ciId = item.getToCiId();
                        } else if (item.getDirection().equals(RelDirectionType.TO.getValue())) {
                            ciEntityId = item.getFromCiEntityId();
                            ciId = item.getFromCiId();
                        }

                        if (!ciEntityTransactionMap.containsKey(ciEntityId)) {
                            TransactionVo toTransactionVo = new TransactionVo();
                            toTransactionVo.setCiId(ciId);
                            toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());

                            CiEntityTransactionVo endCiEntityTransactionVo = new CiEntityTransactionVo();
                            endCiEntityTransactionVo.setCiEntityId(ciEntityId);
                            endCiEntityTransactionVo.setCiId(ciId);
                            endCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            endCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                            createSnapshot(endCiEntityTransactionVo);

                            transactionMapper.insertTransaction(toTransactionVo);
                            transactionMapper.insertCiEntityTransaction(endCiEntityTransactionVo);
                            transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());

                            ciEntityTransactionMap.put(endCiEntityTransactionVo.getCiEntityId(), toTransactionVo.getId());
                        }

                        RelEntityTransactionVo endRelEntityVo = new RelEntityTransactionVo(item, RelActionType.DELETE);
                        endRelEntityVo.setTransactionId(ciEntityTransactionMap.get(ciEntityId));
                        transactionMapper.insertRelEntityTransaction(endRelEntityVo);
                    }
                }
            });
            ciEntityMapper.deleteCiEntityById(ciEntityTransactionVo.getCiEntityId());

            transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
            transactionMapper.updateTransactionStatus(transactionVo);
            return null;
        } else {//添加或编辑配置项
            // 写入属性
            List<AttrEntityTransactionVo> attrEntityTransactionList =
                    ciEntityTransactionVo.getAttrEntityTransactionList();
            if (CollectionUtils.isNotEmpty(attrEntityTransactionList)) {
                for (AttrEntityTransactionVo attrEntityTransactionVo : attrEntityTransactionList) {
                    AttrEntityVo attrEntityVo = new AttrEntityVo(attrEntityTransactionVo);
                    if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.REPLACE.getValue())) {
                        attrEntityMapper.deleteAttrEntity(attrEntityVo);
                    }
                    if (CollectionUtils.isNotEmpty(attrEntityVo.getValueHashList())) {
                        attrEntityMapper.insertAttrEntity(attrEntityVo);
                    }
                }
            }
            // 写入关系
            List<RelEntityTransactionVo> relEntityTransactionList = ciEntityTransactionVo.getRelEntityTransactionList();
            if (CollectionUtils.isNotEmpty(relEntityTransactionList)) {
                //记录对端配置项的事务，如果同一个配置项则不需要添加多个事务
                Map<Long, Long> ciEntityTransactionMap = new HashMap<>();
                for (RelEntityTransactionVo relEntityTransactionVo : relEntityTransactionList) {
                    //如果不是自己引用自己，则需要补充对端配置项事务，此块需要在真正删除数据前处理
                    if (!relEntityTransactionVo.getFromCiEntityId().equals(relEntityTransactionVo.getToCiEntityId())) {
                        Long ciEntityId = null, ciId = null;

                        if (relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            ciEntityId = relEntityTransactionVo.getToCiEntityId();
                            ciId = relEntityTransactionVo.getToCiId();
                        } else if (relEntityTransactionVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                            ciEntityId = relEntityTransactionVo.getFromCiEntityId();
                            ciId = relEntityTransactionVo.getFromCiId();
                        }

                        if (!ciEntityTransactionMap.containsKey(ciEntityId)) {
                            TransactionVo toTransactionVo = new TransactionVo();
                            toTransactionVo.setCiId(ciId);
                            toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());

                            CiEntityTransactionVo endCiEntityTransactionVo = new CiEntityTransactionVo();
                            endCiEntityTransactionVo.setCiEntityId(ciEntityId);
                            endCiEntityTransactionVo.setCiId(ciId);
                            endCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            endCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                            createSnapshot(endCiEntityTransactionVo);

                            transactionMapper.insertTransaction(toTransactionVo);
                            transactionMapper.insertCiEntityTransaction(endCiEntityTransactionVo);
                            transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());

                            ciEntityTransactionMap.put(endCiEntityTransactionVo.getCiEntityId(), toTransactionVo.getId());
                        }

                        RelEntityTransactionVo endRelEntityVo = new RelEntityTransactionVo(relEntityTransactionVo);
                        endRelEntityVo.setTransactionId(ciEntityTransactionMap.get(ciEntityId));
                        transactionMapper.insertRelEntityTransaction(endRelEntityVo);
                    }

                    if (relEntityTransactionVo.getAction().equals(RelActionType.DELETE.getValue())) {
                        relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(relEntityTransactionVo.getRelId(),
                                relEntityTransactionVo.getFromCiEntityId(), relEntityTransactionVo.getToCiEntityId());
                    } else if (relEntityTransactionVo.getAction().equals(RelActionType.INSERT.getValue())) {
                        RelEntityVo newRelEntityVo = new RelEntityVo(relEntityTransactionVo);
                        if (relEntityMapper.checkRelEntityIsExists(newRelEntityVo) == 0) {
                            relEntityMapper.insertRelEntity(newRelEntityVo);
                        }
                    }
                }
            }

            CiEntityVo ciEntityVo = new CiEntityVo(ciEntityTransactionVo);
            // 最后写入配置项，因为cientityname需要依赖所有属性
            ciEntityVo.setAttrEntityList(attrEntityMapper.getAttrEntityByCiEntityId(ciEntityVo.getId()));
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())) {
                ciEntityMapper.insertCiEntity(ciEntityVo);
            } else if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
                // 解除配置项修改锁定
                ciEntityVo.setIsLocked(0);
                ciEntityMapper.updateCiEntityLockById(ciEntityVo);
            }
            transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
            transactionMapper.updateTransactionStatus(transactionVo);
            return ciEntityVo.getId();
        }
    }

    /**
     * 提交事务，返回配置项id
     */
    @Override
    public Long commitTransaction(Long transactionId) {
        TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
        if (transactionVo != null) {
            CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
            ciEntityTransactionVo.setAttrEntityTransactionList(
                    transactionMapper.getAttrEntityTransactionByTransactionIdAndCiEntityId(transactionId,
                            ciEntityTransactionVo.getCiEntityId()));
            ciEntityTransactionVo.setRelEntityTransactionList(
                    transactionMapper.getRelEntityTransactionByTransactionIdAndCiEntityId(transactionId,
                            ciEntityTransactionVo.getCiEntityId()));
            // 单纯保存事务时由于不一定会修改所有属性和关系，为了通过必填属性校验，所以要将编辑模式改成局部模式
            ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
            boolean hasChange = validateCiEntityTransaction(ciEntityTransactionVo);
            if (hasChange) {
                return this.commitTransaction(transactionVo, new TransactionGroupVo());
            }
        }
        return null;
    }

    @Override
    public CiEntityVo getCiEntityDetailById(Long ciEntityId) {
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityById(ciEntityId);
        if (ciEntityVo != null) {
            ciEntityVo.setAttrEntityList(attrEntityMapper.getAttrEntityByCiEntityId(ciEntityId));
            ciEntityVo.setRelEntityList(relEntityMapper.getRelEntityByCiEntityId(ciEntityId));
        }
        return ciEntityVo;
    }

}
