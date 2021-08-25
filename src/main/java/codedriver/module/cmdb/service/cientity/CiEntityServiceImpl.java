/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.cientity;

import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.*;
import codedriver.framework.cmdb.dto.transaction.*;
import codedriver.framework.cmdb.enums.*;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiUniqueRuleException;
import codedriver.framework.cmdb.exception.cientity.*;
import codedriver.framework.cmdb.exception.transaction.TransactionAuthException;
import codedriver.framework.cmdb.exception.transaction.TransactionStatusIrregularException;
import codedriver.framework.cmdb.validator.core.IValidator;
import codedriver.framework.cmdb.validator.core.ValidatorFactory;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.mq.core.ITopic;
import codedriver.framework.mq.core.TopicFactory;
import codedriver.framework.transaction.core.AfterTransactionJob;
import codedriver.module.cmdb.attrexpression.AttrExpressionRebuildManager;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.utils.CiEntityBuilder;
import codedriver.module.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CiEntityServiceImpl implements CiEntityService {
    // private final static Logger logger = LoggerFactory.getLogger(CiEntityServiceImpl.class);
    private final static String EXPRESSION_TYPE = "expression";
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Resource
    private TransactionMapper transactionMapper;


    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public CiEntityVo getCiEntityBaseInfoById(Long ciEntityId) {
        return ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
    }

    @Override
    public CiEntityVo getCiEntityById(Long ciId, Long ciEntityId) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        CiEntityVo ciEntityVo = new CiEntityVo();
        List<CiVo> ciList;
        if (ciVo.getIsVirtual().equals(0)) {
            ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        } else {
            ciList = new ArrayList<>();
            ciList.add(ciVo);
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        ciEntityVo.setCiList(ciList);
        ciEntityVo.setId(ciEntityId);
        ciEntityVo.setCiId(ciVo.getId());
        ciEntityVo.setCiLabel(ciVo.getLabel());
        ciEntityVo.setCiName(ciVo.getName());

        ciEntityVo.setAttrList(attrList);
        ciEntityVo.setRelList(relList);
        List<Map<String, Object>> resultList = ciEntityMapper.getCiEntityById(ciEntityVo);
        return new CiEntityBuilder.Builder(ciEntityVo, resultList, ciVo, attrList, relList).build().getCiEntity();
    }

    @Override
    public List<CiEntityVo> getCiEntityByIdList(Long ciId, List<Long> ciEntityIdList) {
        return getCiEntityByIdList(ciId, ciEntityIdList, null);
    }

    @Override
    public List<CiEntityVo> getCiEntityByIdList(Long ciId, List<Long> ciEntityIdList, List<Long> groupIdList) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiList(ciList);
        ciEntityVo.setAttrList(attrList);
        ciEntityVo.setRelList(relList);
        ciEntityVo.setGroupIdList(groupIdList);
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            ciEntityVo.setIdList(ciEntityIdList);
            List<Map<String, Object>> resultList = ciEntityMapper.searchCiEntity(ciEntityVo);
            return new CiEntityBuilder.Builder(ciEntityVo, resultList, ciVo, attrList, relList).build().getCiEntityList();
        }
        return new ArrayList<>();
    }


    @Override
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo) {
        CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
        if (ciVo == null) {
            throw new CiNotFoundException(ciEntityVo.getCiId());
        }
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        ciEntityVo.setCiList(ciList);
        ciEntityVo.setAttrList(attrList);
        ciEntityVo.setRelList(relList);
        /*
        如果有属性过滤，则根据属性补充关键信息
         */
        if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrFilterList())) {
            Iterator<AttrFilterVo> itAttrFilter = ciEntityVo.getAttrFilterList().iterator();
            while (itAttrFilter.hasNext()) {
                AttrFilterVo attrFilterVo = itAttrFilter.next();
                boolean isExists = false;
                for (AttrVo attrVo : attrList) {
                    if (attrVo.getId().equals(attrFilterVo.getAttrId())) {
                        attrFilterVo.setCiId(attrVo.getCiId());
                        attrFilterVo.setNeedTargetCi(attrVo.isNeedTargetCi());
                        isExists = true;
                        break;
                    }
                }
                if (!isExists) {
                    itAttrFilter.remove();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(ciEntityVo.getRelFilterList())) {
            Iterator<RelFilterVo> itRelFilter = ciEntityVo.getRelFilterList().iterator();
            while (itRelFilter.hasNext()) {
                RelFilterVo relFilterVo = itRelFilter.next();
                boolean isExists = false;
                for (RelVo relVo : relList) {
                    if (relVo.getId().equals(relFilterVo.getRelId())) {
                        isExists = true;
                        break;
                    }
                }
                if (!isExists) {
                    itRelFilter.remove();
                }
            }
        }
        if (CollectionUtils.isEmpty(ciEntityVo.getIdList())) {
            ciEntityVo.setSmartSearch(true);
            int rowNum = ciEntityMapper.searchCiEntityIdCount(ciEntityVo);
            ciEntityVo.setRowNum(rowNum);
            List<Long> ciEntityIdList = ciEntityMapper.searchCiEntityId(ciEntityVo);
            if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
                ciEntityVo.setIdList(ciEntityIdList);
            }
        }
        if (CollectionUtils.isNotEmpty(ciEntityVo.getIdList())) {
            ciEntityVo.setSmartSearch(false);
            List<Map<String, Object>> resultList = ciEntityMapper.searchCiEntity(ciEntityVo);
            ciEntityVo.setIdList(null);//清除id列表，避免ciEntityVo重用时数据没法更新
            return new CiEntityBuilder.Builder(ciEntityVo, resultList, ciVo, attrList, relList).build().getCiEntityList();
        }
        return new ArrayList<>();
    }

    @Override
    public List<CiEntityVo> searchCiEntityBaseInfo(CiEntityVo ciEntityVo) {
        List<CiEntityVo> ciEntityList = ciEntityMapper.searchCiEntityBaseInfo(ciEntityVo);
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            int rowNum = ciEntityMapper.searchCiEntityBaseInfoCount(ciEntityVo);
            ciEntityVo.setRowNum(rowNum);
        }
        return ciEntityList;
    }

    /**
     * 删除配置项
     *
     * @param ciEntityVo 配置项
     * @return 事务id
     */
    @Transactional
    @Override
    public Long deleteCiEntity(CiEntityVo ciEntityVo, Boolean allowCommit) {
        return deleteCiEntity(ciEntityVo, allowCommit, new TransactionGroupVo());
    }

    /**
     * 批量删除配置项
     *
     * @param ciEntityList 配置项列表
     * @param allowCommit  是否允许提交
     * @return 事务组id
     */
    @Transactional
    @Override
    public Long deleteCiEntityList(List<CiEntityVo> ciEntityList, Boolean allowCommit) {
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
            for (CiEntityVo ciEntityVo : ciEntityList) {
                Long transactionId = deleteCiEntity(ciEntityVo, allowCommit, transactionGroupVo);
                if (transactionId > 0L) {
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionId);
                }
            }
            return transactionGroupVo.getId();
        }
        return 0L;
    }

    /**
     * 删除配置项
     *
     * @param ciEntityVo 配置项
     * @return 事务id，为0代表没有创建新事务
     */
    @Transactional
    @Override
    public Long deleteCiEntity(CiEntityVo ciEntityVo, Boolean allowCommit, TransactionGroupVo transactionGroupVo) {
        Long ciEntityId = ciEntityVo.getId();
        CiEntityVo baseCiEntityVo = this.ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
        if (baseCiEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        //检查是否有未提交的删除事务，如果有就不再创建新事务
        List<TransactionVo> transactionList = transactionMapper.getUnCommitTransactionByCiEntityIdAndAction(ciEntityId, TransactionActionType.DELETE.getValue());
        if (CollectionUtils.isNotEmpty(transactionList)) {
            return 0L;//没有创建新事务
        }

        CiEntityVo oldCiEntityVo = this.getCiEntityById(baseCiEntityVo.getCiId(), ciEntityId);

        //如果作为属性被引用，则不能删除
        List<AttrVo> attrList = ciEntityMapper.getAttrListByToCiEntityId(ciEntityId);
        if (CollectionUtils.isNotEmpty(attrList)) {
            throw new CiEntityIsInUsedException(attrList);
        }

        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(oldCiEntityVo.getCiId());
        transactionVo.setInputFrom(InputFromContext.get().getInputFrom());
        transactionVo.setStatus(TransactionStatus.UNCOMMIT.getValue());
        transactionVo.setCreateUser(UserContext.get().getUserUuid(true));
        transactionVo.setDescription(ciEntityVo.getDescription());
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(oldCiEntityVo);
        ciEntityTransactionVo.setAction(TransactionActionType.DELETE.getValue());
        ciEntityTransactionVo.setTransactionId(transactionVo.getId());
        ciEntityTransactionVo.setOldCiEntityVo(oldCiEntityVo);
        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);

        // 保存快照
        createSnapshot(ciEntityTransactionVo);

        // 写入事务
        transactionMapper.insertTransaction(transactionVo);
        // 写入配置项事务
        transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);
        if (allowCommit) {
            commitTransaction(transactionVo, transactionGroupVo);
        }

        return transactionVo.getId();

    }

    @Override
    public boolean validateCiEntity(CiEntityTransactionVo ciEntityTransactionVo) {
        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(ciEntityTransactionVo.getCiId());

        ciEntityTransactionVo.setTransactionId(transactionVo.getId());

        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);

        return validateCiEntityTransaction(ciEntityTransactionVo);
    }

    @Override
    @Transactional
    public Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList) {
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        return saveCiEntity(ciEntityTransactionList, transactionGroupVo);
    }

    @Override
    @Transactional
    public Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList, TransactionGroupVo transactionGroupVo) {
        for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
            transactionGroupVo.addExclude(ciEntityTransactionVo.getCiEntityId());
        }
        if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
            for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
                Long transactionId = saveCiEntity(ciEntityTransactionVo, transactionGroupVo);
                if (transactionId > 0L) {
                    transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), transactionId);
                }
            }
        }
        return transactionGroupVo.getId();
    }

    @Transactional
    @Override
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo) {
        return saveCiEntity(ciEntityTransactionVo, new TransactionGroupVo());
    }


    /**
     * 保存单个配置项事务
     *
     * @param ciEntityTransactionVo 事务
     * @param transactionGroupVo    事务分组
     * @return 事务id
     */
    @Transactional
    @Override
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo, TransactionGroupVo transactionGroupVo) {
        if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
            CiEntityVo oldCiEntityVo = this.getCiEntityById(ciEntityTransactionVo.getCiId(), ciEntityTransactionVo.getCiEntityId());

            // 正在编辑中的配置项，在事务提交或删除前不允许再次修改
            if (oldCiEntityVo == null) {
                throw new CiEntityNotFoundException(ciEntityTransactionVo.getCiEntityId());
            } else if (oldCiEntityVo.getIsLocked().equals(1)) {
                throw new CiEntityIsLockedException(ciEntityTransactionVo.getCiEntityId());
            }
            ciEntityTransactionVo.setOldCiEntityVo(oldCiEntityVo);

            //锁定当前配置项
            oldCiEntityVo.setIsLocked(1);
            ciEntityMapper.updateCiEntityLockById(oldCiEntityVo);
        }

        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(ciEntityTransactionVo.getCiId());
        transactionVo.setInputFrom(InputFromContext.get().getInputFrom());
        transactionVo.setStatus(TransactionStatus.UNCOMMIT.getValue());
        transactionVo.setCreateUser(UserContext.get().getUserUuid(true));
        transactionVo.setDescription(ciEntityTransactionVo.getDescription());
        ciEntityTransactionVo.setTransactionId(transactionVo.getId());

        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);
        boolean hasChange = validateCiEntityTransaction(ciEntityTransactionVo);

        if (hasChange) {
            //生成配置项名称
            createCiEntityName(ciEntityTransactionVo);

            // 保存快照
            createSnapshot(ciEntityTransactionVo);

            // 写入事务
            transactionMapper.insertTransaction(transactionVo);
            // 写入配置项事务
            transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);
            //提交事务
            if (ciEntityTransactionVo.isAllowCommit()) {
                commitTransaction(transactionVo, transactionGroupVo);
            }
            return transactionVo.getId();
        } else {
            // 没有任何变化则返回零
            return 0L;
        }
    }

    private void createCiEntityName(CiEntityTransactionVo ciEntityTransactionVo) {
        CiVo ciVo = ciMapper.getCiById(ciEntityTransactionVo.getCiId());
        List<AttrEntityTransactionVo> attrEntityList = ciEntityTransactionVo.getAttrEntityTransactionList();
        if (ciVo.getNameAttrId() != null) {
            //如果事务中存在名称属性
            Optional<AttrEntityTransactionVo> op = attrEntityList.stream().filter(attr -> attr.getAttrId().equals(ciVo.getNameAttrId())).findFirst();
            if (op.isPresent()) {
                AttrEntityTransactionVo attrEntityTransactionVo = op.get();
                if (attrEntityTransactionVo.isNeedTargetCi()) {
                    List<Long> invokeCiEntityIdList = new ArrayList<>();
                    for (int i = 0; i < attrEntityTransactionVo.getValueList().size(); i++) {
                        try {
                            invokeCiEntityIdList.add(attrEntityTransactionVo.getValueList().getLong(i));
                        } catch (Exception ignored) {

                        }
                    }
                    Set<CiEntityVo> invokeCiEntitySet = new HashSet<>();
                    if (CollectionUtils.isNotEmpty(invokeCiEntityIdList)) {
                        invokeCiEntitySet.addAll(ciEntityMapper.getCiEntityBaseInfoByIdList(invokeCiEntityIdList));
                    }
                    if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue()) && attrEntityTransactionVo.getSaveMode().equals(SaveModeType.MERGE.getValue())) {
                        invokeCiEntitySet.addAll(ciEntityMapper.getCiEntityBaseInfoByAttrIdAndFromCiEntityId(ciEntityTransactionVo.getCiEntityId(), attrEntityTransactionVo.getAttrId()));
                    }
                    if (CollectionUtils.isNotEmpty(invokeCiEntitySet)) {
                        ciEntityTransactionVo.setName(invokeCiEntitySet.stream().map(CiEntityVo::getName).collect(Collectors.joining(",")));
                    } else {
                        ciEntityTransactionVo.setName("");
                    }
                } else {
                    ciEntityTransactionVo.setName(attrEntityTransactionVo.getValue());
                }
            } else {
                //找不到直接使用旧名字
                CiEntityVo oldCiEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityTransactionVo.getCiEntityId());
                if (oldCiEntityVo != null) {
                    ciEntityTransactionVo.setName(oldCiEntityVo.getName());
                }
            }
        }
    }

    @Override
    public void updateCiEntityName(CiEntityVo ciEntityVo) {
        ciEntityMapper.updateCiEntityName(ciEntityVo);
    }

    @Override
    public void updateCiEntityNameForCi(CiVo ciVo) {
        if (ciVo.getNameAttrId() != null) {
            CiEntityVo pCiEntityVo = new CiEntityVo();
            pCiEntityVo.setCiId(ciVo.getId());
            pCiEntityVo.setPageSize(100);
            pCiEntityVo.setCurrentPage(1);
            List<CiEntityVo> ciEntityList = searchCiEntity(pCiEntityVo);
            while (CollectionUtils.isNotEmpty(ciEntityList)) {
                for (CiEntityVo ciEntityVo : ciEntityList) {
                    String ciEntityName = "";
                    for (AttrEntityVo attrEntityVo : ciEntityVo.getAttrEntityList()) {
                        if (attrEntityVo.getAttrId().equals(ciVo.getNameAttrId())) {
                            ciEntityName = attrEntityVo.getActualValueList().stream().map(Object::toString).collect(Collectors.joining(","));
                            break;
                        }
                    }
                    ciEntityVo.setName(ciEntityName);
                    ciEntityMapper.updateCiEntityName(ciEntityVo);
                }
                pCiEntityVo.setCurrentPage(pCiEntityVo.getCurrentPage() + 1);
                ciEntityList = searchCiEntity(pCiEntityVo);
            }
        }
    }

    @Override
    public void createSnapshot(CiEntityTransactionVo ciEntityTransactionVo) {
        CiEntityVo oldCiEntityVo = ciEntityTransactionVo.getOldCiEntityVo();
        if (oldCiEntityVo != null) {
            String content = JSON.toJSONString(oldCiEntityVo);
            ciEntityTransactionVo.setSnapshot(content);
        }
    }


    /**
     * 验证配置项数据是否合法
     * 属性必填校验：
     * 1、编辑模式是Global模式，不提供的属性代表要删除，需要校验是否满足必填。
     * 2、编辑模式是Partial模式，不提供的属性代表不修改，所以不需要做校验。
     * 3、如果是Merge模式，并且是引用型属性，需要先获取旧属性，如果新值旧值同时为空才需要抛异常。
     * 4、如果是replace模式，新值为空则直接抛异常。
     * <p>
     * 检查属性是否唯一：
     * 1、如果是引用值，则存在多值的可能性，需要根据保存模式来组装数据进行校验。
     * 如果是replace模式，直接用新值进行校验。
     * 如果是Merge模式，则需要先把新值和旧值合并后再进行校验。
     * 2、如果是普通属性值，只有单值的可能，不管什么模式只需要使用新值进行校验即可。
     * <p>
     * 检查唯一规则：
     * 1、如果唯一规则包含引用值，
     * <p>
     * 记录属性是新增、编辑或删除：
     * 1、如果整个attr_xxxx在snapshot中不存在，代表添加了新属性，如果attr_xxxx的valueList在snapshot中为空，代表属性添加了值。
     * 2、如果snapshot本身已存在attr_xxx和valueList，代表编辑。
     * 3、如果修改模式是Partial，attr_xxx为空，代表删除整个属性，如果只是valueList清空，代表属性删除了值，其他不提供的代表不动。
     *
     * @param ciEntityTransactionVo 配置项事务实体
     * @return true:验证成功 false:验证失败
     */
    private boolean validateCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityTransactionVo.getCiId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciEntityTransactionVo.getCiId()));
        CiVo ciVo = ciMapper.getCiById(ciEntityTransactionVo.getCiId());
        CiEntityVo oldEntity = ciEntityTransactionVo.getOldCiEntityVo();
        List<AttrEntityVo> oldAttrEntityList = null;
        List<RelEntityVo> oldRelEntityList = null;
        if (oldEntity == null) {
            //如果是单纯校验可能会没有旧配置项信息
            oldEntity = this.getCiEntityById(ciEntityTransactionVo.getCiId(), ciEntityTransactionVo.getCiEntityId());
        }
        if (oldEntity != null) {
            oldAttrEntityList = oldEntity.getAttrEntityList();
            oldRelEntityList = oldEntity.getRelEntityList();
        }
        if (oldAttrEntityList == null) {
            oldAttrEntityList = new ArrayList<>();
        }
        if (oldRelEntityList == null) {
            oldRelEntityList = new ArrayList<>();
        }

        // 清除和模型属性不匹配的属性
        if (MapUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityData())) {
            for (AttrVo attrVo : attrList) {
                JSONObject attrEntityData = ciEntityTransactionVo.getAttrEntityDataByAttrId(attrVo.getId());
                if (attrEntityData == null) {
                    ciEntityTransactionVo.removeAttrEntityData(attrVo.getId());
                } else {
                    //修正属性基本信息，多余属性不要
                    JSONArray valueList = attrEntityData.getJSONArray("valueList");
                    //进行必要的值转换，例如密码转换成密文
                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
                    handler.transferValueListToSave(attrVo, valueList);
                    attrEntityData.clear();
                    attrEntityData.put("valueList", valueList);
                    attrEntityData.put("label", attrVo.getLabel());
                    attrEntityData.put("name", attrVo.getName());
                    attrEntityData.put("type", attrVo.getType());
                    attrEntityData.put("ciId", attrVo.getCiId());
                    attrEntityData.put("targetCiId", attrVo.getTargetCiId());
                }
            }
        }

        // 消除和模型属性不匹配的关系
        if (MapUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityData())) {
            for (RelVo relVo : relList) {
                JSONObject relEntityData = ciEntityTransactionVo.getRelEntityDataByRelIdAndDirection(relVo.getId(), relVo.getDirection());
                if (relEntityData != null) {
                    if (!relEntityData.containsKey("valueList") || CollectionUtils.isEmpty(relEntityData.getJSONArray("valueList"))) {
                        ciEntityTransactionVo.removeRelEntityData(relVo.getId(), relVo.getDirection());
                    } else {
                        //补充关系基本信息
                        String direction = relVo.getDirection();
                        relEntityData.put("name", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToName() : relVo.getFromName());
                        relEntityData.put("label", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToLabel() : relVo.getFromLabel());
                        relEntityData.put("direction", direction);
                        relEntityData.put("fromCiId", relVo.getFromCiId());
                        relEntityData.put("toCiId", relVo.getToCiId());
                        for (int i = relEntityData.getJSONArray("valueList").size() - 1; i >= 0; i--) {
                            JSONObject valueObj = relEntityData.getJSONArray("valueList").getJSONObject(i);
                            if (StringUtils.isBlank(valueObj.getString("ciEntityName")) && valueObj.getLong("ciEntityId") != null) {
                                CiEntityVo cientity = ciEntityMapper.getCiEntityBaseInfoById(valueObj.getLong("ciEntityId"));
                                if (cientity != null) {
                                    valueObj.put("ciEntityName", cientity.getName());
                                } else {
                                    relEntityData.getJSONArray("valueList").remove(i);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (AttrVo attrVo : attrList) {
            if (!attrVo.getType().equals(EXPRESSION_TYPE)) {
                AttrEntityTransactionVo attrEntityTransactionVo =
                        ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrVo.getId());
                /* 属性必填校验： */
                if (attrVo.getIsRequired().equals(1)) {
                    if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue()) || ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
                        if (attrEntityTransactionVo == null) {
                            throw new AttrEntityValueEmptyException(attrVo.getLabel());
                        } else if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.REPLACE.getValue())
                                && CollectionUtils.isEmpty(attrEntityTransactionVo.getValueList())) {
                            throw new AttrEntityValueEmptyException(attrVo.getLabel());
                        } else if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.MERGE.getValue()) && attrVo.isNeedTargetCi() && CollectionUtils.isEmpty(attrEntityTransactionVo.getValueList())) {
                            List<AttrEntityVo> oldList = ciEntityMapper.getAttrEntityByAttrIdAndFromCiEntityId(ciEntityTransactionVo.getCiEntityId(), attrVo.getId());
                            if (CollectionUtils.isEmpty(oldList)) {
                                throw new AttrEntityValueEmptyException(attrVo.getLabel());
                            }
                        }
                    }
                }

                /* 检查属性是否唯一： */
                if (attrVo.getIsUnique().equals(1)) {
                    if (attrEntityTransactionVo != null
                            && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())) {
                        if (attrVo.isNeedTargetCi()) {
                            List<Long> toCiEntityIdList = new ArrayList<>();
                            for (int i = 0; i < attrEntityTransactionVo.getValueList().size(); i++) {
                                //如果不是id,则代表是新添加配置项，这时候不需要判断属性唯一
                                if (attrEntityTransactionVo.getValueList().get(i) instanceof Number) {
                                    Long tmpId = attrEntityTransactionVo.getValueList().getLong(i);
                                    if (tmpId != null) {
                                        toCiEntityIdList.add(tmpId);
                                    }
                                }
                            }
                            if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.MERGE.getValue())) {
                                //合并新老值
                                List<AttrEntityVo> oldList = ciEntityMapper.getAttrEntityByAttrIdAndFromCiEntityId(ciEntityTransactionVo.getCiEntityId(), attrVo.getId());
                                for (AttrEntityVo attrEntityVo : oldList) {
                                    if (!toCiEntityIdList.contains(attrEntityVo.getToCiEntityId())) {
                                        toCiEntityIdList.add(attrEntityVo.getToCiEntityId());
                                    }
                                }
                            }
                            //检查新值是否被别的配置项引用
                            if (CollectionUtils.isNotEmpty(toCiEntityIdList)) {
                                //FIXME 这里的SQL有问题，有空再重写
                                int attrEntityCount = ciEntityMapper.getAttrEntityCountByAttrIdAndValue(ciEntityTransactionVo.getCiEntityId(), attrVo.getId(), toCiEntityIdList);
                                if (attrEntityCount > 0) {
                                    List<CiEntityVo> toCiEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(toCiEntityIdList);
                                    throw new AttrEntityDuplicateException(attrVo.getLabel(), toCiEntityList.stream().map(CiEntityVo::getName).collect(Collectors.toList()));
                                }
                            }
                        } else {
                            //检查配置项表对应字段是否已被其他配置项使用
                            int count = ciEntityMapper.getCiEntityCountByAttrIdAndValue(ciEntityTransactionVo.getCiEntityId(), attrVo, attrEntityTransactionVo.getValueList().getString(0));
                            if (count > 0) {
                                throw new AttrEntityDuplicateException(attrVo.getLabel(), attrEntityTransactionVo.getValueList());
                            }
                        }
                    }
                }

                //检查是否允许多选
                if (attrVo.getTargetCiId() != null && MapUtils.isNotEmpty(attrVo.getConfig()) && attrVo.getConfig().containsKey("isMultiple") && attrVo.getConfig().getString("isMultiple").equals("0")) {
                    if (attrEntityTransactionVo != null && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList()) && attrEntityTransactionVo.getValueList().size() > 1) {
                        throw new AttrEntityMultipleException(attrVo);
                    }
                }

                /*  调用校验器校验数据合法性，只有非引用型属性才需要 */
                if (attrEntityTransactionVo != null && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())
                        && StringUtils.isNotBlank(attrVo.getValidatorHandler()) && !attrVo.isNeedTargetCi()) {
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
                    ciEntityTransactionVo.getRelEntityTransactionByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue());
            // 判断当前配置项处于to位置的规则
            List<RelEntityTransactionVo> toRelEntityTransactionList =
                    ciEntityTransactionVo.getRelEntityTransactionByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue());

            // 标记当前模型是在关系的上端或者下端
            boolean isFrom = false;
            boolean isTo = false;
            if (relVo.getFromCiId().equals(ciEntityTransactionVo.getCiId())) {
                isFrom = true;
            }
            if (relVo.getToCiId().equals(ciEntityTransactionVo.getCiId())) {
                isTo = true;
            }
            // 全局模式下，不存在关系信息代表删除，需要校验必填规则
            if ((ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())
                    || ciEntityTransactionVo.getAction().equals(TransactionActionType.RECOVER.getValue()))
                    || ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
                if (CollectionUtils.isEmpty(fromRelEntityTransactionList)) {
                    if (isFrom && relVo.getToIsRequired().equals(1)) {
                        throw new RelEntityNotFoundException(relVo.getToLabel());
                    }
                } else {
                    if (fromRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getToRule())) {
                            throw new RelEntityMultipleException(relVo.getToLabel());
                        }
                        if (relVo.getFromIsUnique().equals(1)) {
                            throw new RelEntityIsUsedException(RelDirectionType.FROM, relVo, false);
                        }
                    }
                    //检查关系唯一
                    if (relVo.getToIsUnique().equals(1)) {
                        for (RelEntityTransactionVo fromRelEntityVo : fromRelEntityTransactionList) {
                            List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(fromRelEntityVo.getToCiEntityId(), relVo.getId());
                            if (checkFromRelEntityList.stream().anyMatch(r -> !r.getFromCiEntityId().equals(ciEntityTransactionVo.getCiEntityId()))) {
                                throw new RelEntityIsUsedException(RelDirectionType.FROM, relVo);
                            }
                        }
                    }
                }

                // 全局模式下，不存在关系信息代表删除，需要校验必填规则
                if (CollectionUtils.isEmpty(toRelEntityTransactionList)) {
                    if (isTo && relVo.getFromIsRequired().equals(1)) {
                        throw new RelEntityNotFoundException(relVo.getFromLabel());
                    }
                } else {
                    if (toRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getFromRule())) {
                            throw new RelEntityMultipleException(relVo.getFromLabel());
                        }
                        if (relVo.getToIsUnique().equals(1)) {
                            throw new RelEntityIsUsedException(RelDirectionType.TO, relVo, false);
                        }
                    }
                    //检查关系唯一
                    if (relVo.getFromIsUnique().equals(1)) {
                        for (RelEntityTransactionVo toRelEntityVo : toRelEntityTransactionList) {
                            List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(toRelEntityVo.getFromCiEntityId(), relVo.getId());
                            if (checkFromRelEntityList.stream().anyMatch(r -> !r.getToCiEntityId().equals(ciEntityTransactionVo.getCiEntityId()))) {
                                throw new RelEntityIsUsedException(RelDirectionType.TO, relVo);
                            }
                        }
                    }
                }

            } else if (ciEntityTransactionVo.getEditMode().equals(EditModeType.PARTIAL.getValue())) {
                if (CollectionUtils.isNotEmpty(fromRelEntityTransactionList)) {
                    if (fromRelEntityTransactionList.size() == 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getToRule())) {
                            // 需要提取已有的关系信息判断是否有重复
                            List<RelEntityVo> fromRelEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(
                                    ciEntityTransactionVo.getCiEntityId(), relVo.getId());
                            if ((fromRelEntityList.size() == 1
                                    && !fromRelEntityList.contains(new RelEntityVo(fromRelEntityTransactionList.get(0))))
                                    || fromRelEntityList.size() > 1) {
                                throw new RelEntityMultipleException(relVo.getToLabel());
                            }
                        }
                    } else if (fromRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getToRule())) {
                            throw new RelEntityMultipleException(relVo.getToLabel());
                        }
                    }
                    //检查关系唯一
                    if (relVo.getToIsUnique().equals(1)) {
                        for (RelEntityTransactionVo fromRelEntityVo : fromRelEntityTransactionList) {
                            List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(fromRelEntityVo.getToCiEntityId(), relVo.getId());
                            if (checkFromRelEntityList.stream().anyMatch(r -> !r.getFromCiEntityId().equals(ciEntityTransactionVo.getCiEntityId()))) {
                                throw new RelEntityIsUsedException(RelDirectionType.FROM, relVo);
                            }
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(toRelEntityTransactionList)) {
                    if (toRelEntityTransactionList.size() == 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getFromRule())) {
                            // 需要提取已有的关系信息判断是否有重复
                            List<RelEntityVo> toRelEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(
                                    ciEntityTransactionVo.getCiEntityId(), relVo.getId());
                            if ((toRelEntityList.size() == 1
                                    && !toRelEntityList.contains(new RelEntityVo(toRelEntityTransactionList.get(0))))
                                    || toRelEntityList.size() > 1) {
                                throw new RelEntityMultipleException(relVo.getFromLabel());
                            }
                        }
                    } else if (fromRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getFromRule())) {
                            throw new RelEntityMultipleException(relVo.getFromLabel());
                        }
                    }
                    //检查关系唯一
                    if (relVo.getFromIsUnique().equals(1)) {
                        for (RelEntityTransactionVo toRelEntityVo : toRelEntityTransactionList) {
                            List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(toRelEntityVo.getFromCiEntityId(), relVo.getId());
                            if (checkFromRelEntityList.stream().anyMatch(r -> !r.getToCiEntityId().equals(ciEntityTransactionVo.getCiEntityId()))) {
                                throw new RelEntityIsUsedException(RelDirectionType.TO, relVo);
                            }
                        }
                    }
                }
            }
        }

        //校验唯一规则
        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            ciEntityConditionVo.setCiId(ciEntityTransactionVo.getCiId());
            for (Long attrId : ciVo.getUniqueAttrIdList()) {
                AttrEntityTransactionVo attrEntityTransactionVo = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrId);
                if (attrEntityTransactionVo != null) {
                    AttrFilterVo filterVo = new AttrFilterVo();
                    filterVo.setAttrId(attrId);
                    filterVo.setExpression(SearchExpression.EQ.getExpression());
                    filterVo.setValueList(attrEntityTransactionVo.getValueList().stream().map(Object::toString).collect(Collectors.toList()));
                    ciEntityConditionVo.addAttrFilter(filterVo);
                } else {
                    if (oldEntity != null) {
                        AttrEntityVo attrEntityVo = oldEntity.getAttrEntityByAttrId(attrId);
                        if (attrEntityVo != null) {
                            AttrFilterVo filterVo = new AttrFilterVo();
                            filterVo.setAttrId(attrId);
                            filterVo.setExpression(SearchExpression.EQ.getExpression());
                            filterVo.setValueList(attrEntityVo.getValueList().stream().map(Object::toString).collect(Collectors.toList()));
                            ciEntityConditionVo.addAttrFilter(filterVo);
                        }
                    }//新值没有修改
                }
            }
            if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                List<CiEntityVo> checkList = this.searchCiEntity(ciEntityConditionVo);
                for (CiEntityVo checkCiEntity : checkList) {
                    if (!checkCiEntity.getId().equals(ciEntityTransactionVo.getCiEntityId())) {
                        throw new CiUniqueRuleException();
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
                ciEntityTransactionVo.removeAttrEntityData(oldAttrEntityTransactionList);
                if (MapUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityData())) {
                    hasChange = true;
                }
            } else {
                hasChange = true;
            }
        }

        // List<RelEntityTransactionVo> newRelEntityTransactionList = ciEntityTransactionVo.getRelEntityTransactionList();


        // 全局修改模式下，事务中不包含的关系代表要删除
        List<RelEntityVo> needDeleteRelEntityList = new ArrayList<>();
        if (ciEntityTransactionVo.getEditMode().equals(EditModeType.GLOBAL.getValue())) {
            if (CollectionUtils.isNotEmpty(oldRelEntityList)) {
                //找到旧对象中存在，但在事务中不存在的关系，重新添加到事务中，并且把操作设为删除
                for (RelEntityVo relEntityVo : oldRelEntityList) {
                    Long targetId = relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relEntityVo.getToCiEntityId() : relEntityVo.getFromCiEntityId();
                    if (!ciEntityTransactionVo.containRelEntityData(relEntityVo.getRelId(), relEntityVo.getDirection(), targetId)) {
                        //先暂存在待删除列表里，等清除完没变化的关系后在添加到事务里
                        needDeleteRelEntityList.add(relEntityVo);
                    }
                }
            }
        }

        // 排除掉没变化的关系
        for (RelEntityVo relEntityVo : oldRelEntityList) {
            Long targetId = relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relEntityVo.getToCiEntityId() : relEntityVo.getFromCiEntityId();
            ciEntityTransactionVo.removeRelEntityData(relEntityVo.getRelId(), relEntityVo.getDirection(), targetId);
        }

        //补充待删除关系进事务里
        for (RelEntityVo relEntityVo : needDeleteRelEntityList) {
            Long targetCiId, targetCiEntityId;
            String targetCiEntityName;
            if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                targetCiEntityId = relEntityVo.getToCiEntityId();
                targetCiId = relEntityVo.getToCiId();
                targetCiEntityName = relEntityVo.getToCiEntityName();
            } else {
                targetCiEntityId = relEntityVo.getFromCiEntityId();
                targetCiId = relEntityVo.getFromCiId();
                targetCiEntityName = relEntityVo.getFromCiEntityName();
            }
            RelVo relVo = relMapper.getRelById(relEntityVo.getRelId());
            ciEntityTransactionVo.addRelEntityData(relVo, relEntityVo.getDirection(), targetCiId, targetCiEntityId, targetCiEntityName, RelActionType.DELETE.getValue());
        }

        if (MapUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityData())) {
            hasChange = true;
        }
        return hasChange;
    }


    /**
     * 验证事务是否能提交
     * 1、如果是删除，直接通过。
     * 2、如果是修改，判断配置项是否已经被删除。
     * <p>
     * 属性必填校验：
     * 1、删除的属性才需要校验属性是否必填。
     * <p>
     * 检查属性是否唯一：
     * 1、如果是引用值，则存在多值的可能性，需要根据保存模式来组装数据进行校验。
     * 如果是replace模式，直接用新值进行校验。
     * 如果是Merge模式，则需要先把新值和旧值合并后再进行校验。
     * 2、如果是普通属性值，只有单值的可能，不管什么模式只需要使用新值进行校验即可。
     * <p>
     * 检查唯一规则：
     * 1、如果唯一规则包含引用值，
     * <p>
     * 记录属性是新增、编辑或删除：
     * 1、如果整个attr_xxxx在snapshot中不存在，代表添加了新属性，如果attr_xxxx的valueList在snapshot中为空，代表属性添加了值。
     * 2、如果snapshot本身已存在attr_xxx和valueList，代表编辑。
     * 3、如果修改模式是Partial，attr_xxx为空，代表删除整个属性，如果只是valueList清空，代表属性删除了值，其他不提供的代表不动。
     *
     * @param ciEntityTransactionVo 配置项事务实体
     * @return true:验证成功 false:验证失败
     */
    private boolean validateCiEntityTransactionForCommit(CiEntityTransactionVo ciEntityTransactionVo) {
        if (ciEntityTransactionVo.getAction().equals(TransactionActionType.DELETE.getValue())) {
            return true;
        } else {
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityTransactionVo.getCiId());
            List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciEntityTransactionVo.getCiId()));
            CiVo ciVo = ciMapper.getCiById(ciEntityTransactionVo.getCiId());

            // 清除不存在的属性
            if (MapUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityData())) {
                List<AttrEntityTransactionVo> attrEntityList = ciEntityTransactionVo.getAttrEntityTransactionList();
                for (AttrEntityTransactionVo attrEntityTransaction : attrEntityList) {
                    if (attrList.stream().noneMatch(a -> a.getId().equals(attrEntityTransaction.getAttrId()))) {
                        ciEntityTransactionVo.removeAttrEntityData(attrEntityTransaction.getAttrId());
                    }
                }
            }

            // 消除不存在的关系
            if (MapUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityData())) {
                List<RelEntityTransactionVo> relEntityList = ciEntityTransactionVo.getRelEntityTransactionList();
                for (RelEntityTransactionVo relEntity : relEntityList) {
                    if (relList.stream().noneMatch(r -> r.getId().equals(relEntity.getRelId()) && r.getDirection().equals(relEntity.getDirection()))) {
                        ciEntityTransactionVo.removeRelEntityData(relEntity.getRelId(), relEntity.getDirection());
                    }
                }
            }

            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue()) || ciEntityTransactionVo.getAction().equals(TransactionActionType.RECOVER.getValue())) {
                //新增配置项需要全面校验属性
                for (AttrVo attrVo : attrList) {
                    if (!attrVo.getType().equals(EXPRESSION_TYPE)) {
                        AttrEntityTransactionVo attrEntityTransactionVo =
                                ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrVo.getId());
                        /* 属性必填校验： */
                        if (attrVo.getIsRequired().equals(1)) {
                            if (attrEntityTransactionVo == null || CollectionUtils.isEmpty(attrEntityTransactionVo.getValueList())) {
                                throw new AttrEntityValueEmptyException(attrVo.getLabel());
                            }
                        }
                        /* 检查属性是否唯一： */
                        if (attrVo.getIsUnique().equals(1)) {
                            if (attrEntityTransactionVo != null
                                    && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())) {
                                if (attrVo.isNeedTargetCi()) {
                                    List<Long> toCiEntityIdList = new ArrayList<>();
                                    for (int i = 0; i < attrEntityTransactionVo.getValueList().size(); i++) {
                                        if (attrEntityTransactionVo.getValueList().get(i) instanceof Number) {
                                            Long tmpId = attrEntityTransactionVo.getValueList().getLong(i);
                                            if (tmpId != null) {
                                                toCiEntityIdList.add(tmpId);
                                            }
                                        }
                                    }
                                    //检查新值是否被别的配置项引用
                                    if (CollectionUtils.isNotEmpty(toCiEntityIdList)) {
                                        int attrEntityCount = ciEntityMapper.getAttrEntityCountByAttrIdAndValue(ciEntityTransactionVo.getCiEntityId(), attrVo.getId(), toCiEntityIdList);
                                        if (attrEntityCount > 0) {
                                            List<CiEntityVo> toCiEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(toCiEntityIdList);
                                            throw new AttrEntityDuplicateException(attrVo.getLabel(), toCiEntityList.stream().map(CiEntityVo::getName).collect(Collectors.toList()));
                                        }
                                    }
                                } else {
                                    //检查配置项表对应字段是否已被其他配置项使用
                                    int count = ciEntityMapper.getCiEntityCountByAttrIdAndValue(ciEntityTransactionVo.getCiEntityId(), attrVo, attrEntityTransactionVo.getValueList().getString(0));
                                    if (count > 0) {
                                        throw new AttrEntityDuplicateException(attrVo.getLabel(), attrEntityTransactionVo.getValueList());
                                    }
                                }
                            }
                        }

                        /*  调用校验器校验数据合法性，只有非引用型属性才需要 */
                        if (attrEntityTransactionVo != null && CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())
                                && StringUtils.isNotBlank(attrVo.getValidatorHandler()) && !attrVo.isNeedTargetCi()) {
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
                            ciEntityTransactionVo.getRelEntityTransactionByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue());
                    // 判断当前配置项处于to位置的规则
                    List<RelEntityTransactionVo> toRelEntityTransactionList =
                            ciEntityTransactionVo.getRelEntityTransactionByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue());

                    // 标记当前模型是在关系的上端或者下端
                    boolean isFrom = false;
                    boolean isTo = false;
                    if (relVo.getFromCiId().equals(ciEntityTransactionVo.getCiId())) {
                        isFrom = true;
                    }
                    if (relVo.getToCiId().equals(ciEntityTransactionVo.getCiId())) {
                        isTo = true;
                    }


                    if (CollectionUtils.isEmpty(fromRelEntityTransactionList)) {
                        if (isFrom && relVo.getToIsRequired().equals(1)) {
                            throw new RelEntityNotFoundException(relVo.getToLabel());
                        }
                    } else if (fromRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getToRule())) {
                            throw new RelEntityMultipleException(relVo.getToLabel());
                        }
                    }

                    if (CollectionUtils.isEmpty(toRelEntityTransactionList)) {
                        if (isTo && relVo.getFromIsRequired().equals(1)) {
                            throw new RelEntityNotFoundException(relVo.getFromLabel());
                        }
                    } else if (toRelEntityTransactionList.size() > 1) {
                        // 检查关系是否允许重复
                        if (RelRuleType.O.getValue().equals(relVo.getFromRule())) {
                            throw new RelEntityMultipleException(relVo.getFromLabel());
                        }
                    }
                }
                //校验唯一规则
                if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
                    CiEntityVo ciEntityConditionVo = new CiEntityVo();
                    ciEntityConditionVo.setCiId(ciEntityTransactionVo.getCiId());
                    for (Long attrId : ciVo.getUniqueAttrIdList()) {
                        AttrEntityTransactionVo attrEntityTransactionVo = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrId);
                        if (attrEntityTransactionVo != null) {
                            AttrFilterVo filterVo = new AttrFilterVo();
                            filterVo.setAttrId(attrId);
                            filterVo.setExpression(SearchExpression.EQ.getExpression());
                            filterVo.setValueList(attrEntityTransactionVo.getValueList().stream().map(Object::toString).collect(Collectors.toList()));
                            ciEntityConditionVo.addAttrFilter(filterVo);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                        List<CiEntityVo> checkList = this.searchCiEntity(ciEntityConditionVo);
                        for (CiEntityVo checkCiEntity : checkList) {
                            if (!checkCiEntity.getId().equals(ciEntityTransactionVo.getCiEntityId())) {
                                throw new CiUniqueRuleException();
                            }
                        }
                    }
                }
            } else if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
                CiEntityVo oldEntity = this.getCiEntityById(ciEntityTransactionVo.getCiId(), ciEntityTransactionVo.getCiEntityId());
                if (oldEntity == null) {
                    throw new CiEntityNotFoundException(ciEntityTransactionVo.getCiEntityId());
                }
                for (AttrVo attrVo : attrList) {
                    if (!attrVo.getType().equals(EXPRESSION_TYPE)) {
                        AttrEntityTransactionVo attrEntityTransactionVo =
                                ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrVo.getId());
                        if (attrEntityTransactionVo != null) {
                            //属性是否需要删除
                            boolean isDelete = CollectionUtils.isEmpty(attrEntityTransactionVo.getValueList()) || (attrEntityTransactionVo.getValueList().size() == 1 && StringUtils.isBlank(attrEntityTransactionVo.getValueList().getString(0)));
                            /*检查必填属性：*/
                            if (attrVo.getIsRequired().equals(1) && isDelete) {
                                throw new AttrEntityValueEmptyException(attrVo.getLabel());
                            }

                            /* 检查属性是否唯一： */
                            if (!isDelete && attrVo.getIsUnique().equals(1)) {
                                if (attrVo.isNeedTargetCi()) {
                                    List<Long> toCiEntityIdList = new ArrayList<>();
                                    for (int i = 0; i < attrEntityTransactionVo.getValueList().size(); i++) {
                                        Long tmpId = attrEntityTransactionVo.getValueList().getLong(i);
                                        if (tmpId != null) {
                                            toCiEntityIdList.add(tmpId);
                                        }
                                    }
                                    if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.MERGE.getValue())) {
                                        //合并新老值
                                        List<AttrEntityVo> oldList = ciEntityMapper.getAttrEntityByAttrIdAndFromCiEntityId(ciEntityTransactionVo.getCiEntityId(), attrVo.getId());
                                        for (AttrEntityVo attrEntityVo : oldList) {
                                            if (!toCiEntityIdList.contains(attrEntityVo.getToCiEntityId())) {
                                                toCiEntityIdList.add(attrEntityVo.getToCiEntityId());
                                            }
                                        }
                                    }
                                    //检查新值是否被别的配置项引用
                                    int attrEntityCount = ciEntityMapper.getAttrEntityCountByAttrIdAndValue(ciEntityTransactionVo.getCiEntityId(), attrVo.getId(), toCiEntityIdList);
                                    if (attrEntityCount > 0) {
                                        List<CiEntityVo> toCiEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(toCiEntityIdList);
                                        throw new AttrEntityDuplicateException(attrVo.getLabel(), toCiEntityList.stream().map(CiEntityVo::getName).collect(Collectors.toList()));
                                    }
                                } else {
                                    //检查配置项表对应字段是否已被其他配置项使用
                                    int count = ciEntityMapper.getCiEntityCountByAttrIdAndValue(ciEntityTransactionVo.getCiEntityId(), attrVo, attrEntityTransactionVo.getValueList().getString(0));
                                    if (count > 0) {
                                        throw new AttrEntityDuplicateException(attrVo.getLabel(), attrEntityTransactionVo.getValueList());
                                    }
                                }
                            }

                            /*  调用校验器校验数据合法性，只有非引用型属性才需要 */
                            if (CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())
                                    && StringUtils.isNotBlank(attrVo.getValidatorHandler()) && !attrVo.isNeedTargetCi()) {
                                IValidator validator = ValidatorFactory.getValidator(attrVo.getValidatorHandler());
                                if (validator != null) {
                                    validator.valid(attrVo.getLabel(), attrEntityTransactionVo.getValueList(),
                                            attrVo.getValidatorId());
                                }
                            }
                        }
                    }
                }

                // 校验关系信息
                for (RelVo relVo : relList) {
                    // 判断当前配置项处于from位置的规则
                    List<RelEntityTransactionVo> fromRelEntityTransactionList =
                            ciEntityTransactionVo.getRelEntityTransactionByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue());
                    // 判断当前配置项处于to位置的规则
                    List<RelEntityTransactionVo> toRelEntityTransactionList =
                            ciEntityTransactionVo.getRelEntityTransactionByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue());

                    // 标记当前模型是在关系的上端或者下端
                    boolean isFrom = false;
                    boolean isTo = false;
                    if (relVo.getFromCiId().equals(ciEntityTransactionVo.getCiId())) {
                        isFrom = true;
                    }
                    if (relVo.getToCiId().equals(ciEntityTransactionVo.getCiId())) {
                        isTo = true;
                    }


                    if (CollectionUtils.isNotEmpty(fromRelEntityTransactionList)) {
                        //判断关系必填
                        if (isFrom && relVo.getToIsRequired().equals(1)) {
                            List<RelEntityTransactionVo> delEntityList = fromRelEntityTransactionList.stream().filter(r -> r.getAction().equals(RelActionType.DELETE.getValue())).collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(delEntityList)) {
                                //如果关系早已经被清空，则不需要再做必填校验
                                if (CollectionUtils.isNotEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue()))) {
                                    for (RelEntityTransactionVo relEntity : delEntityList) {
                                        oldEntity.removeRelEntityData(relEntity.getRelId(), RelDirectionType.FROM.getValue(), relEntity.getToCiEntityId());
                                    }
                                }
                                if (CollectionUtils.isEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue()))) {
                                    throw new RelEntityNotFoundException(relVo.getToLabel());
                                }
                            }
                        }
                        //判断关系多选
                        if (RelRuleType.O.getValue().equals(relVo.getToRule())) {
                            List<RelEntityTransactionVo> insertEntityList = fromRelEntityTransactionList.stream().filter(r -> r.getAction().equals(RelActionType.INSERT.getValue())).collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(insertEntityList)) {
                                if (insertEntityList.size() > 1) {
                                    throw new RelEntityMultipleException(relVo.getToLabel());
                                } else {
                                    if (CollectionUtils.isNotEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue()))) {
                                        for (RelEntityTransactionVo relEntity : insertEntityList) {
                                            oldEntity.removeRelEntityData(relEntity.getRelId(), RelDirectionType.FROM.getValue(), relEntity.getToCiEntityId());
                                        }
                                    }
                                    if (CollectionUtils.isNotEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue()))) {
                                        throw new RelEntityMultipleException(relVo.getToLabel());
                                    }
                                }
                            }
                        }
                        //判断关系唯一
                        if (relVo.getToIsUnique().equals(1)) {
                            for (RelEntityTransactionVo fromRelEntityVo : fromRelEntityTransactionList) {
                                List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(fromRelEntityVo.getToCiEntityId(), relVo.getId());
                                if (checkFromRelEntityList.stream().anyMatch(r -> !r.getFromCiEntityId().equals(ciEntityTransactionVo.getCiEntityId()))) {
                                    throw new RelEntityIsUsedException(RelDirectionType.FROM, relVo);
                                }
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(toRelEntityTransactionList)) {
                        //判断关系必填
                        if (isTo && relVo.getFromIsRequired().equals(1)) {
                            List<RelEntityTransactionVo> delEntityList = toRelEntityTransactionList.stream().filter(r -> r.getAction().equals(RelActionType.DELETE.getValue())).collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(delEntityList)) {
                                //如果关系早已经被清空，则不需要再做必填校验
                                if (CollectionUtils.isNotEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue()))) {
                                    for (RelEntityTransactionVo relEntity : delEntityList) {
                                        oldEntity.removeRelEntityData(relEntity.getRelId(), RelDirectionType.TO.getValue(), relEntity.getFromCiEntityId());
                                    }
                                }
                                if (CollectionUtils.isEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue()))) {
                                    throw new RelEntityNotFoundException(relVo.getFromLabel());
                                }
                            }
                        }

                        //判断关系多选
                        if (RelRuleType.O.getValue().equals(relVo.getFromRule())) {
                            List<RelEntityTransactionVo> insertEntityList = toRelEntityTransactionList.stream().filter(r -> r.getAction().equals(RelActionType.INSERT.getValue())).collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(insertEntityList)) {
                                if (insertEntityList.size() > 1) {
                                    throw new RelEntityMultipleException(relVo.getFromLabel());
                                } else {
                                    if (CollectionUtils.isNotEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue()))) {
                                        for (RelEntityTransactionVo relEntity : insertEntityList) {
                                            oldEntity.removeRelEntityData(relEntity.getRelId(), RelDirectionType.TO.getValue(), relEntity.getFromCiEntityId());
                                        }
                                    }
                                    if (CollectionUtils.isNotEmpty(oldEntity.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue()))) {
                                        throw new RelEntityMultipleException(relVo.getToLabel());
                                    }
                                }
                            }
                        }
                        //判断关系唯一
                        if (relVo.getFromIsUnique().equals(1)) {
                            for (RelEntityTransactionVo toRelEntityVo : toRelEntityTransactionList) {
                                List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(toRelEntityVo.getFromCiEntityId(), relVo.getId());
                                if (checkFromRelEntityList.stream().anyMatch(r -> !r.getToCiEntityId().equals(ciEntityTransactionVo.getCiEntityId()))) {
                                    throw new RelEntityIsUsedException(RelDirectionType.TO, relVo);
                                }
                            }
                        }
                    }
                }

                //校验唯一规则
                if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
                    CiEntityVo ciEntityConditionVo = new CiEntityVo();
                    ciEntityConditionVo.setCiId(ciEntityTransactionVo.getCiId());
                    for (Long attrId : ciVo.getUniqueAttrIdList()) {
                        AttrEntityTransactionVo attrEntityTransactionVo = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrId);
                        if (attrEntityTransactionVo != null) {
                            AttrFilterVo filterVo = new AttrFilterVo();
                            filterVo.setAttrId(attrId);
                            filterVo.setExpression(SearchExpression.EQ.getExpression());
                            filterVo.setValueList(attrEntityTransactionVo.getValueList().stream().map(Object::toString).collect(Collectors.toList()));
                            ciEntityConditionVo.addAttrFilter(filterVo);
                        } else {
                            AttrEntityVo attrEntityVo = oldEntity.getAttrEntityByAttrId(attrId);
                            if (attrEntityVo != null) {
                                AttrFilterVo filterVo = new AttrFilterVo();
                                filterVo.setAttrId(attrId);
                                filterVo.setExpression(SearchExpression.EQ.getExpression());
                                filterVo.setValueList(attrEntityVo.getValueList().stream().map(Object::toString).collect(Collectors.toList()));
                                ciEntityConditionVo.addAttrFilter(filterVo);
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                        List<CiEntityVo> checkList = this.searchCiEntity(ciEntityConditionVo);
                        for (CiEntityVo checkCiEntity : checkList) {
                            if (!checkCiEntity.getId().equals(ciEntityTransactionVo.getCiEntityId())) {
                                throw new CiUniqueRuleException();
                            }
                        }
                    }
                }
            }
        }
        return true;
    }


    /**
     * 提交事务 修改规则:
     * editMode针对单次修改。editMode=Global下，提供属性或关系代表修改，不提供代表删除；editMode=Partial下，提供属性或关系代表修改，不提供代表不修改
     * saveMode针对单个属性或关系，saveMode=replace，代表覆盖，最后结果是修改或删除；saveMode=merge，代表合并，最后结果是添加新成员或维持不变。
     *
     * @param transactionVo      事务
     * @param transactionGroupVo 事务分组
     * @return Long 配置项id
     */
    private Long commitTransaction(TransactionVo transactionVo, TransactionGroupVo transactionGroupVo) {
        CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
        if (ciEntityTransactionVo.getAction().equals(TransactionActionType.DELETE.getValue())) { // 删除配置项
            //补充对端配置项事务信息
            List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(ciEntityTransactionVo.getCiEntityId());
            //记录对端配置项的事务，如果同一个配置项则不需要添加多个事务
            Set<Long> ciEntityTransactionSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(relEntityList)) {
                for (RelEntityVo item : relEntityList) {
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

                        if (!ciEntityTransactionSet.contains(ciEntityId) && !transactionGroupVo.isExclude(ciEntityId)) {
                            TransactionVo toTransactionVo = new TransactionVo();
                            toTransactionVo.setCiId(ciId);
                            toTransactionVo.setInputFrom(transactionVo.getInputFrom());
                            toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                            toTransactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                            toTransactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                            CiEntityTransactionVo endCiEntityTransactionVo = new CiEntityTransactionVo();
                            CiEntityVo oldCiEntityVo = this.getCiEntityById(ciId, ciEntityId);
                            endCiEntityTransactionVo.setCiEntityId(ciEntityId);
                            endCiEntityTransactionVo.setCiId(ciId);
                            endCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            endCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                            endCiEntityTransactionVo.setName(oldCiEntityVo.getName());
                            endCiEntityTransactionVo.setOldCiEntityVo(oldCiEntityVo);
                            createSnapshot(endCiEntityTransactionVo);
                            //补充关系删除事务数据
                            RelVo relVo = relMapper.getRelById(item.getRelId());
                            //由于是补充对端关系，所以关系要取反
                            endCiEntityTransactionVo.addRelEntityData(relVo,
                                    item.getDirection().equals(RelDirectionType.FROM.getValue()) ? RelDirectionType.TO.getValue() : RelDirectionType.FROM.getValue(),
                                    item.getDirection().equals(RelDirectionType.FROM.getValue()) ? item.getFromCiId() : item.getToCiId(),
                                    item.getDirection().equals(RelDirectionType.FROM.getValue()) ? item.getFromCiEntityId() : item.getToCiEntityId(),
                                    item.getDirection().equals(RelDirectionType.FROM.getValue()) ? item.getFromCiEntityName() : item.getToCiEntityName(),
                                    TransactionActionType.DELETE.getValue());

                            transactionMapper.insertTransaction(toTransactionVo);
                            transactionMapper.insertCiEntityTransaction(endCiEntityTransactionVo);
                            transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());

                            //正式删除关系数据
                            relEntityMapper.deleteRelEntityByRelIdFromCiEntityIdToCiEntityId(item.getRelId(),
                                    item.getFromCiEntityId(), item.getToCiEntityId());

                            ciEntityTransactionSet.add(ciEntityId);
                        }
                    }
                }
            }
            CiEntityVo deleteCiEntityVo = new CiEntityVo(ciEntityTransactionVo);
            //删除之前找到所有关联配置项，可能需要更新他们的表达式属性
            this.updateInvokedExpressionAttr(deleteCiEntityVo);

            this.deleteCiEntity(deleteCiEntityVo);

            //修改事务状态
            transactionVo.setCommitUser(UserContext.get().getUserId(true));
            transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
            transactionMapper.updateTransactionStatus(transactionVo);

            //发送消息到消息队列
            ITopic<CiEntityTransactionVo> topic = TopicFactory.getTopic("cmdb/cientity/delete");
            if (topic != null) {
                topic.send(ciEntityTransactionVo);
            }
            return null;
        } else {
            /*
            写入属性信息
            1、如果是引用类型，并且是replace模式，需要先清空原来的值再写入。
             */
            CiVo ciVo = ciMapper.getCiById(ciEntityTransactionVo.getCiId());
            CiEntityVo ciEntityVo = new CiEntityVo(ciEntityTransactionVo);
            for (AttrEntityTransactionVo attrEntityTransactionVo :
                    ciEntityTransactionVo.getAttrEntityTransactionList()) {
                AttrEntityVo attrEntityVo = new AttrEntityVo(attrEntityTransactionVo);
                if (attrEntityVo.isNeedTargetCi()) {
                    if (attrEntityTransactionVo.getSaveMode().equals(SaveModeType.REPLACE.getValue())) {
                        ciEntityMapper.deleteAttrEntityByFromCiEntityIdAndAttrId(ciEntityTransactionVo.getCiEntityId(), attrEntityTransactionVo.getAttrId());
                    }
                    if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                        ciEntityMapper.insertAttrEntity(attrEntityVo);
                    }
                    //更新配置项名称
                    if (Objects.equals(ciVo.getNameAttrId(), attrEntityVo.getAttrId())) {
                        List<CiEntityVo> invokeCiEntityList = ciEntityMapper.getCiEntityBaseInfoByAttrIdAndFromCiEntityId(ciEntityVo.getId(), attrEntityVo.getAttrId());
                        if (CollectionUtils.isNotEmpty(invokeCiEntityList)) {
                            ciEntityVo.setName(invokeCiEntityList.stream().map(CiEntityVo::getName).collect(Collectors.joining(",")));
                        } else {
                            ciEntityVo.setName("");
                        }
                        updateCiEntityName(ciEntityVo);
                    }
                } else {
                    //更新配置项名称
                    if (Objects.equals(ciVo.getNameAttrId(), attrEntityVo.getAttrId())) {
                        ciEntityVo.setName(attrEntityVo.getValue());
                        updateCiEntityName(ciEntityVo);
                    }
                }
            }

            /*
            写入配置项信息
             */
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())) {
                this.insertCiEntity(ciEntityVo);
            } else if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
                this.updateCiEntity(ciEntityVo);
            } else if (ciEntityTransactionVo.getAction().equals(TransactionActionType.RECOVER.getValue())) {
                if (ciEntityMapper.getCiEntityBaseInfoById(ciEntityTransactionVo.getCiEntityId()) == null) {
                    this.insertCiEntity(ciEntityVo);
                } else {
                    throw new CiEntityIsRecoveredException(ciEntityVo.getName());
                }
            }
            /*
            写入关系信息
             */
            List<RelEntityTransactionVo> relEntityTransactionList = ciEntityTransactionVo.getRelEntityTransactionList();
            if (CollectionUtils.isNotEmpty(relEntityTransactionList)) {
                //记录对端配置项的事务，如果同一个配置项则不需要添加多个事务
                Map<Long, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
                for (RelEntityTransactionVo relEntityTransactionVo : relEntityTransactionList) {
                    //如果不是自己引用自己，则需要补充对端配置项事务，此块需要在真正删除数据前处理
                    if (!relEntityTransactionVo.getFromCiEntityId().equals(relEntityTransactionVo.getToCiEntityId())) {
                        Long ciEntityId = null, ciId = null, sourceCiEntityId = ciEntityTransactionVo.getCiEntityId(), sourceCiId = ciEntityTransactionVo.getCiId();
                        String sourceCiEntityName = ciEntityTransactionVo.getName();
                        if (relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            ciEntityId = relEntityTransactionVo.getToCiEntityId();
                            ciId = relEntityTransactionVo.getToCiId();
                        } else if (relEntityTransactionVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                            ciEntityId = relEntityTransactionVo.getFromCiEntityId();
                            ciId = relEntityTransactionVo.getFromCiId();
                        }

                        //排除掉当前事务的cientityId，不然有可能会重复插入事务
                        if (!transactionGroupVo.isExclude(ciEntityId) && !ciEntityTransactionMap.containsKey(ciEntityId)) {
                            TransactionVo toTransactionVo = new TransactionVo();
                            toTransactionVo.setCiId(ciId);
                            toTransactionVo.setInputFrom(transactionVo.getInputFrom());
                            toTransactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                            toTransactionVo.setCreateUser(UserContext.get().getUserUuid(true));
                            toTransactionVo.setCommitUser(UserContext.get().getUserUuid(true));
                            transactionMapper.insertTransaction(toTransactionVo);
                            transactionMapper.insertTransactionGroup(transactionGroupVo.getId(), toTransactionVo.getId());
                            CiEntityTransactionVo endCiEntityTransactionVo = new CiEntityTransactionVo();
                            endCiEntityTransactionVo.setCiEntityId(ciEntityId);
                            endCiEntityTransactionVo.setCiId(ciId);
                            endCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            endCiEntityTransactionVo.setTransactionId(toTransactionVo.getId());
                            endCiEntityTransactionVo.setOldCiEntityVo(this.getCiEntityById(ciId, ciEntityId));
                            createSnapshot(endCiEntityTransactionVo);
                            ciEntityTransactionMap.put(endCiEntityTransactionVo.getCiEntityId(), endCiEntityTransactionVo);
                        }

                        if (ciEntityTransactionMap.containsKey(ciEntityId)) {
                            CiEntityTransactionVo endCiEntityTransactionVo = ciEntityTransactionMap.get(ciEntityId);
                            //补充关系修改信息
                            RelVo relVo = relMapper.getRelById(relEntityTransactionVo.getRelId());
                            endCiEntityTransactionVo.addRelEntityData(relVo, relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? RelDirectionType.TO.getValue() : RelDirectionType.FROM.getValue(), sourceCiId, sourceCiEntityId, sourceCiEntityName, relEntityTransactionVo.getAction());
                        }
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
                //所有事务信息补充完毕后才能写入，因为对端配置项有可能被引用多次
                for (Long ciEntityId : ciEntityTransactionMap.keySet()) {
                    transactionMapper.insertCiEntityTransaction(ciEntityTransactionMap.get(ciEntityId));
                }
            }

            //重新计算所有表达式属性的值
            AttrExpressionRebuildManager.rebuild(new RebuildAuditVo(ciEntityVo, RebuildAuditVo.Type.INVOKE));

            //重新计算引用了当前配置项的所有表达式属性的值
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
                AttrExpressionRebuildManager.rebuild(new RebuildAuditVo(ciEntityVo, RebuildAuditVo.Type.INVOKED));
            }

            // 解除配置项修改锁定
            ciEntityVo.setIsLocked(0);
            ciEntityMapper.updateCiEntityLockById(ciEntityVo);

            //修改事务状态
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.RECOVER.getValue())) {
                transactionVo.setStatus(TransactionStatus.RECOVER.getValue());
                transactionVo.setRecoverUser(UserContext.get().getUserUuid(true));
            } else {
                transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
                transactionVo.setCommitUser(UserContext.get().getUserUuid(true));
            }
            transactionMapper.updateTransactionStatus(transactionVo);
            return ciEntityVo.getId();
        }
    }

    /**
     * 更新所有关联了当前配置配置项的表达式属性，一般用在删除
     * 1、先判断当前配置项模型是否有被表达式属性引用。
     * 2、如果有则查出所有关联配置项放入重建记录里。
     *
     * @param ciEntityVo 配置项信息
     */
    private void updateInvokedExpressionAttr(CiEntityVo ciEntityVo) {
        List<Long> ciIdList = attrMapper.getExpressionCiIdByValueCiId(ciEntityVo.getCiId());
        if (CollectionUtils.isNotEmpty(ciIdList)) {
            List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(ciEntityVo.getId());
            //排除掉没有表达式属性的模型
            relEntityList.removeIf(rel -> !ciIdList.contains(rel.getFromCiId()) && !ciIdList.contains(rel.getToCiId()));
            //排除掉自我引用
            relEntityList.removeIf(rel -> rel.getFromCiEntityId().equals(ciEntityVo.getId()) && rel.getToCiEntityId().equals(ciEntityVo.getId()));
            if (CollectionUtils.isNotEmpty(relEntityList)) {
                List<RebuildAuditVo> auditList = new ArrayList<>();
                for (RelEntityVo relEntityVo : relEntityList) {
                    RebuildAuditVo rebuildAuditVo = new RebuildAuditVo();
                    rebuildAuditVo.setCiId(relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relEntityVo.getToCiId() : relEntityVo.getFromCiId());
                    rebuildAuditVo.setCiEntityId(relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relEntityVo.getToCiEntityId() : relEntityVo.getFromCiEntityId());
                    rebuildAuditVo.setType(RebuildAuditVo.Type.INVOKE.getValue());
                    auditList.add(rebuildAuditVo);
                }
                AttrExpressionRebuildManager.rebuild(auditList);
            }
        }
    }

    /**
     * 写入配置项
     *
     * @param ciEntityVo 配置项信息
     */
    private void insertCiEntity(CiEntityVo ciEntityVo) {
        CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        ciEntityMapper.insertCiEntityBaseInfo(ciEntityVo);
        for (CiVo ci : ciList) {
            ciEntityVo.setCiId(ci.getId());
            ciEntityMapper.insertCiEntity(ciEntityVo);
        }
    }

    /**
     * 更新配置项
     *
     * @param ciEntityVo 配置项信息
     */
    @Override
    public void updateCiEntity(CiEntityVo ciEntityVo) {
        CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        ciEntityMapper.updateCiEntityBaseInfo(ciEntityVo);
        for (CiVo ci : ciList) {
            if (ciEntityVo.getAttrEntityList().stream().anyMatch(attr -> !attr.isNeedTargetCi() && attr.getFromCiId().equals(ci.getId()))) {
                ciEntityVo.setCiId(ci.getId());
                ciEntityMapper.updateCiEntity(ciEntityVo);
            }
        }
    }

    /**
     * 删除配置项
     *
     * @param ciEntityVo 配置项信息
     */
    private void deleteCiEntity(CiEntityVo ciEntityVo) {
        CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        ciEntityMapper.deleteCiEntityBaseInfo(ciEntityVo);
        for (CiVo ci : ciList) {
            ciEntityVo.setCiId(ci.getId());
            ciEntityMapper.deleteCiEntity(ciEntityVo);
        }
    }

    /**
     * 提交事务，返回配置项id
     */
    @Override
    @Transactional
    public List<TransactionStatusVo> commitTransactionGroup(TransactionGroupVo transactionGroupVo) {
        List<TransactionStatusVo> statusList = new ArrayList<>();
        for (TransactionVo transactionVo : transactionGroupVo.getTransactionList()) {
            transactionGroupVo.addExclude(transactionVo.getCiEntityTransactionVo().getCiEntityId());
        }
        for (TransactionVo transactionVo : transactionGroupVo.getTransactionList()) {
            if (transactionVo.getStatus().equals(TransactionStatus.COMMITED.getValue())) {
                throw new TransactionStatusIrregularException(TransactionStatus.COMMITED);
            } else if (transactionVo.getStatus().equals(TransactionStatus.RECOVER.getValue())) {
                throw new TransactionStatusIrregularException(TransactionStatus.RECOVER);
            }
            if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(transactionVo.getCiId()).checkIsInGroup(transactionVo.getCiEntityId(), GroupType.MAINTAIN).check()) {
                throw new TransactionAuthException();
            }
            try {
                if (validateCiEntityTransactionForCommit(transactionVo.getCiEntityTransactionVo())) {
                    this.commitTransaction(transactionVo, transactionGroupVo);
                    statusList.add(new TransactionStatusVo(transactionVo.getId(), TransactionStatus.COMMITED));
                }
            } catch (Exception ex) {
                AfterTransactionJob<TransactionVo> job = new AfterTransactionJob<>();
                job.execute(transactionVo, t -> {
                }, t -> {
                    t.setError(ex instanceof ApiRuntimeException ? ((ApiRuntimeException) ex).getMessage(true) : ex.getMessage());
                    t.setStatus(TransactionStatus.UNCOMMIT.getValue());
                    transactionMapper.updateTransactionStatus(t);
                });
                throw ex;
            }
        }
        return statusList;
    }

    @Transactional
    public void recoverCiEntity(TransactionVo transactionVo) {
        transactionVo.getCiEntityTransactionVo().restoreSnapshot();

        CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
        transactionVo.setAction(TransactionActionType.RECOVER.getValue());
        transactionVo.getCiEntityTransactionVo().setAction(TransactionActionType.RECOVER.getValue());
        if (validateCiEntityTransactionForCommit(ciEntityTransactionVo)) {
            this.commitTransaction(transactionVo, new TransactionGroupVo());
        }
        transactionMapper.updateTransactionStatus(transactionVo);
    }


}
