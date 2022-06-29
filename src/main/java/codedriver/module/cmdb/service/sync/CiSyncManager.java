/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.sync;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.sync.CollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncAuditVo;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.sync.CollectMode;
import codedriver.framework.cmdb.enums.sync.SyncStatus;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.*;
import codedriver.framework.cmdb.exception.collection.CollectionDataIrregularException;
import codedriver.framework.cmdb.exception.sync.CiEntityDuplicateException;
import codedriver.framework.cmdb.exception.sync.CollectionNotFoundException;
import codedriver.framework.cmdb.utils.RelUtil;
import codedriver.framework.common.constvalue.InputFrom;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.mongodb.client.MongoCursor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 配置项和mongodb同步管理类
 * FIXME 遗留问题：1、searchCiEntityWithCache会缓存还没真正添加到数据库的配置项，如果这个配置项最终添加失败，就会导致后续的引用数据出现错误数据，但这种错误不影响查询，暂时先不处理。
 *  后续要处理需要做比较大的改造，例如生成全局的hashcode和cientityId的关系表，但这样也没法保证数据正确，因为配置项的添加不一定严格按照引用顺序来添加，如果引用方在被引用方前写入数据库，一样会出现类似问题。
 *  目前能想到比较好的做法是查询时能屏蔽不完整数据，允许数据库中能出现一定程度的错误数据，例如关系或引用属性引用了不存在的ciEntityId。
 */
@Service
public class CiSyncManager {
    private static MongoTemplate mongoTemplate;
    private static CiEntityService ciEntityService;
    private static CiMapper ciMapper;
    private static AttrMapper attrMapper;
    private static RelMapper relMapper;
    private static SyncAuditMapper syncAuditMapper;
    private static SyncMapper syncMapper;

    @Autowired
    public CiSyncManager(MongoTemplate _mongoTemplate, CiEntityService _ciEntityService, AttrMapper _attrMapper, SyncAuditMapper _syncAuditMapper, RelMapper _relMapper, CiMapper _ciMapper, SyncMapper _syncMapper) {
        mongoTemplate = _mongoTemplate;
        ciEntityService = _ciEntityService;
        attrMapper = _attrMapper;
        relMapper = _relMapper;
        syncAuditMapper = _syncAuditMapper;
        ciMapper = _ciMapper;
        syncMapper = _syncMapper;
    }

    public static class SyncHandler extends CodeDriverThread {
        private final static Logger logger = LoggerFactory.getLogger(SyncHandler.class);
        private final Map<Long, Map<Long, AttrVo>> CiAttrMap = new HashMap<>();
        private final Map<Long, Map<Long, RelVo>> CiRelMap = new HashMap<>();
        private final Map<Long, CiVo> CiMap = new HashMap<>();//模型缓存
        private final Map<Long, List<CiVo>> DownwardCiMap = new HashMap<>();//子模型缓存
        private final Map<String, SyncCiCollectionVo> InitiativeSyncCiCollectionMap = new HashMap<>();//主动采集集合映射缓存
        private final Map<String, SyncCiCollectionVo> syncCiCollectionMap = new HashMap<>();//普通集合映射缓存
        private final List<SyncCiCollectionVo> syncCiCollectionList;
        private final List<CollectionVo> collectionList;
        private final HashMap<Integer, Object> filterLock = new HashMap<>();
        private JSONObject singleDataObj;
        private final String mode;//如果是batch模式，代表批量更新，如果是single模式，接受单条数据更新
        int CAPACITY = 5000;//缓存大小
        private final Map<Integer, List<CiEntityVo>> ciEntityCache = new LinkedHashMap<Integer, List<CiEntityVo>>(CAPACITY, 0.75F, true) {//用户缓存检索数据，提升效率
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > CAPACITY;
            }
        };


        private List<CiEntityVo> searchCiEntityWithCache(CiEntityVo conditionVo) {
            Object lock;
            int hash = Objects.hash(conditionVo.getCiId(), CollectionUtils.isNotEmpty(conditionVo.getAttrFilterList()) ? JSONObject.toJSONString(conditionVo.getAttrFilterList()) : "");
            synchronized (filterLock) {
                if (!filterLock.containsKey(hash)) {
                    filterLock.put(hash, new Object());
                }
                lock = filterLock.get(hash);
            }
            synchronized (lock) {
                List<CiEntityVo> ciEntityList = ciEntityCache.get(hash);
                //数据不为空才会写入cache
                if (CollectionUtils.isEmpty(ciEntityList)) {
                    ciEntityList = ciEntityService.searchCiEntity(conditionVo);
                    if (CollectionUtils.isNotEmpty(ciEntityList)) {
                        synchronized (ciEntityCache) {
                            ciEntityCache.put(hash, ciEntityList);
                        }
                    } else {
                        //用一个新的list去存放新的配置项对象是为了确保第一次为空时能返回一个空的列表，让系统能正常Insert第一个空配置项，后续添加都是使用cache
                        List<CiEntityVo> tmpCiEntityList = new ArrayList<>();
                        tmpCiEntityList.add(conditionVo);
                        synchronized (ciEntityCache) {
                            ciEntityCache.put(hash, tmpCiEntityList);
                        }
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("缓存命中，当前缓存大小：" + ciEntityCache.size());
                    }
                }
                return ciEntityList;
            }
        }

        public SyncHandler(List<SyncCiCollectionVo> syncCiCollectionVoList) {
            super("COLLECTION-CIENTITY-SYNC-BATCH-HANDLER");
            this.mode = "batch";
            this.syncCiCollectionList = syncCiCollectionVoList;
            //获取所有集合列表
            List<CollectionVo> tmpList = mongoTemplate.find(new Query(), CollectionVo.class, "_dictionary");
            this.collectionList = tmpList.stream().distinct().collect(Collectors.toList());
        }

        public SyncHandler(JSONObject dataObj, List<SyncCiCollectionVo> syncCiCollectionVoList) {
            super("COLLECTION-CIENTITY-SYNC-SINGLE-HANDLER");
            this.mode = "single";
            this.singleDataObj = dataObj;
            this.syncCiCollectionList = syncCiCollectionVoList;
            //获取所有集合列表
            List<CollectionVo> tmpList = mongoTemplate.find(new Query(), CollectionVo.class, "_dictionary");
            this.collectionList = tmpList.stream().distinct().collect(Collectors.toList());
        }

        private SyncCiCollectionVo getSyncCiCollection(Long ciId, String collectionName, String parentKey) {
            String pk = (parentKey == null ? "" : parentKey);
            if (!syncCiCollectionMap.containsKey(ciId + "#" + collectionName + "#" + pk)) {
                List<SyncCiCollectionVo> syncCiCollectionList = syncMapper.getPassiveSyncCiCollectionByCiId(ciId);
                if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
                    //优先使用parentKey来匹配映射配置，避免匹配到没有配置parentKey的映射配置
                    Optional<SyncCiCollectionVo> op = syncCiCollectionList.stream().filter(d -> d.getCollectionName().equals(collectionName) && StringUtils.isNotBlank(d.getParentKey()) && d.getParentKey().equals(pk)).findFirst();
                    if (op.isPresent()) {
                        syncCiCollectionMap.put(ciId + "#" + collectionName + "#" + pk, op.get());
                    } else {
                        op = syncCiCollectionList.stream().filter(d -> d.getCollectionName().equals(collectionName)).findFirst();
                        if (op.isPresent()) {
                            syncCiCollectionMap.put(ciId + "#" + collectionName + "#" + pk, op.get());
                        } else {
                            //如果在当前的逻辑集合collectionName找不到映射配置，则根据ciId随便找一个物理集合相同的被动映射关系
                            // （之所以这样做是假设同一个模型在不同逻辑集合上的配置应该是一致的，所以只要物理集合一致即可）
                            op = syncCiCollectionList.stream().filter(d -> getCollectionByName(d.getCollectionName()).getCollection().equals(getCollectionByName(collectionName).getCollection())).findFirst();
                            op.ifPresent(ciCollectionVo -> syncCiCollectionMap.put(ciId + "#" + collectionName + "#" + pk, ciCollectionVo));
                        }
                    }
                }
            }
            return syncCiCollectionMap.get(ciId + "#" + collectionName + "#" + pk);
        }

        private CiVo getCi(Long ciId) {
            if (!CiMap.containsKey(ciId)) {
                CiVo ciVo = ciMapper.getCiById(ciId);
                if (ciVo == null) {
                    throw new CiNotFoundException(ciId);
                }
                CiMap.put(ciId, ciVo);
            }
            return CiMap.get(ciId);
        }

        private List<CiVo> getDownwardCiList(Long ciId) {
            if (!DownwardCiMap.containsKey(ciId)) {
                CiVo ciVo = getCi(ciId);
                List<CiVo> downCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                DownwardCiMap.put(ciId, downCiList);
            }
            return DownwardCiMap.get(ciId);
        }

        private SyncCiCollectionVo getInitiativeSyncCiCollection(String collectionName) {
            if (!InitiativeSyncCiCollectionMap.containsKey(collectionName)) {
                SyncCiCollectionVo syncCiCollectionVo = syncMapper.getInitiativeSyncCiCollectionByCollectName(collectionName);
                if (syncCiCollectionVo != null) {
                    InitiativeSyncCiCollectionMap.put(collectionName, syncCiCollectionVo);
                } /*else {
                    throw new InitiativeSyncCiCollectionNotFoundException(collectionName);
                }*/
            }
            return InitiativeSyncCiCollectionMap.get(collectionName);
        }

        private Map<Long, AttrVo> getAttrMap(Long ciId) {
            if (!this.CiAttrMap.containsKey(ciId)) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
                Map<Long, AttrVo> tmpAttrMap = new HashMap<>();
                attrList.forEach(attr -> tmpAttrMap.put(attr.getId(), attr));
                this.CiAttrMap.put(ciId, tmpAttrMap);
            }
            return this.CiAttrMap.get(ciId);
        }

        private Map<Long, RelVo> getRelMap(Long ciId) {
            if (!this.CiRelMap.containsKey(ciId)) {
                List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
                Map<Long, RelVo> tmpRelMap = new HashMap<>();
                relList.forEach(rel -> tmpRelMap.put(rel.getId(), rel));
                this.CiRelMap.put(ciId, tmpRelMap);
            }
            return this.CiRelMap.get(ciId);
        }


        /**
         * 根据数据生成配置项事务
         *
         * @param dataObj            数据
         * @param syncCiCollectionVo 映射配置
         * @param parentKey          上一层数据key，如果是关联数据就会有这个参数
         * @return 配置项事务
         */
        private CiEntityTransactionVo generateCiEntityTransaction(JSONObject dataObj, SyncCiCollectionVo syncCiCollectionVo, Map<Integer, CiEntityTransactionVo> ciEntityTransactionMap, String parentKey) {
            //List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            Map<Long, AttrVo> attrMap = getAttrMap(syncCiCollectionVo.getCiId());
            Map<Long, RelVo> relMap = getRelMap(syncCiCollectionVo.getCiId());
            CiVo ciVo = getCi(syncCiCollectionVo.getCiId());
            ciEntityConditionVo.setCiId(ciVo.getId());
            /*
            需要使用模型的唯一规则来查找配置项，如果找不到，就不做任何更新
             */
            //用自动采集设置中的唯一规则替换掉模型的唯一规则，后面的逻辑都不需要修改了
            if (CollectionUtils.isNotEmpty(syncCiCollectionVo.getUniqueAttrIdList())) {
                ciVo.setUniqueAttrIdList(syncCiCollectionVo.getUniqueAttrIdList());
            }
            if (CollectionUtils.isEmpty(ciVo.getUniqueAttrIdList())) {
                throw new CiUniqueRuleNotFoundException(ciVo);
            }
            for (Long uniqueAttrId : ciVo.getUniqueAttrIdList()) {
                SyncMappingVo syncMappingVo = syncCiCollectionVo.getMappingByAttrId(uniqueAttrId);
                AttrVo attr = attrMap.get(uniqueAttrId);
                if (syncMappingVo == null) {
                    throw new CiUniqueAttrNotFoundException(ciVo, attr);
                }

                if (attr != null) {
                    if (dataObj.containsKey(syncMappingVo.getField(parentKey))) {
                        AttrFilterVo filterVo = new AttrFilterVo();
                        filterVo.setAttrId(syncMappingVo.getAttrId());
                        filterVo.setExpression(SearchExpression.EQ.getExpression());
                        String v = dataObj.getString(syncMappingVo.getField(parentKey));
                        if (!attr.isNeedTargetCi()) {
                            if (StringUtils.isNotBlank(v)) {
                                filterVo.setValueList(new ArrayList<String>() {{
                                    this.add(v);
                                }});
                            } else {
                                throw new CiUniqueAttrNotFoundException(syncCiCollectionVo, ciVo, syncMappingVo.getField(parentKey), dataObj);
                            }
                            ciEntityConditionVo.addAttrFilter(filterVo);
                        } else {
                            //如果是引用属性，需要被引用模型的唯一属性只有一个才能成功定位引用配置项
                            CiVo targetCiVo = getCi(attr.getTargetCiId());
                            if (targetCiVo == null) {
                                throw new CiNotFoundException(attr.getTargetCiId());
                            }
                            if (CollectionUtils.isEmpty(targetCiVo.getUniqueAttrIdList())) {
                                throw new CiUniqueRuleNotFoundException(targetCiVo);
                            }
                            if (targetCiVo.getUniqueAttrIdList().size() > 1) {
                                throw new CiMultipleUniqueRuleException(targetCiVo);
                            }
                            Long uAttrId = targetCiVo.getUniqueAttrIdList().get(0);
                            AttrVo targetNameAttrVo = attrMapper.getAttrById(uAttrId);
                            //如果引用了非subset数据，需要检查目标的名称属性是否可以写入数据（非表达式和引用属性），否则则放弃这个属性的导入
                            if (!targetNameAttrVo.getType().equals("expression") && !targetNameAttrVo.isNeedTargetCi()) {
                                CiEntityVo attrConditionVo = new CiEntityVo();
                                attrConditionVo.setCiId(attr.getTargetCiId());
                                attrConditionVo.setAttrIdList(new ArrayList<Long>() {{
                                    this.add(0L);
                                }});
                                attrConditionVo.setRelIdList(new ArrayList<Long>() {{
                                    this.add(0L);
                                }});
                                AttrFilterVo targetFilterVo = new AttrFilterVo();
                                targetFilterVo.setAttrId(uAttrId);
                                targetFilterVo.setExpression(SearchExpression.EQ.getExpression());
                                targetFilterVo.setValueList(new ArrayList<String>() {{
                                    this.add(v);
                                }});
                                attrConditionVo.addAttrFilter(targetFilterVo);
                                List<CiEntityVo> attrCiCheckList = searchCiEntityWithCache(attrConditionVo);//ciEntityService.searchCiEntity(attrConditionVo);
                                if (CollectionUtils.isNotEmpty(attrCiCheckList)) {
                                    List<String> valueList = attrCiCheckList.stream().map(d -> d.getId().toString()).collect(Collectors.toList());
                                    filterVo.setValueList(valueList);
                                } else {
                                    //如果没有找到目标值，则需要放一个不可能存在的值进去，代表当前配置项是不存在的，否则会缺了一个条件导致匹配出错误数据
                                    filterVo.setValueList(new ArrayList<String>() {{
                                        this.add("0");
                                    }});
                                }
                                ciEntityConditionVo.addAttrFilter(filterVo);
                            } else {
                                throw new CiUniqueRuleAttrTypeIrregularException(targetCiVo, targetNameAttrVo);
                            }
                        }
                    } else {
                        throw new CiUniqueAttrNotFoundException(syncCiCollectionVo, ciVo, syncMappingVo.getField(parentKey), dataObj);
                    }
                } else {
                    throw new AttrNotFoundException(uniqueAttrId);
                }
            }

            if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                //使用所有非引用属性去搜索配置项，没有则添加，发现一个则就修改，发现多个就抛异常
                List<CiEntityVo> checkList = searchCiEntityWithCache(ciEntityConditionVo);
                if (CollectionUtils.isNotEmpty(checkList) && checkList.size() > 1) {
                    //补充异常信息
                    CiVo c = ciMapper.getCiById(ciEntityConditionVo.getCiId());
                    ciEntityConditionVo.setCiName(c.getName());
                    ciEntityConditionVo.setCiLabel(c.getLabel());
                    for (AttrFilterVo filter : ciEntityConditionVo.getAttrFilterList()) {
                        AttrVo a = attrMapper.getAttrById(filter.getAttrId());
                        filter.setLabel(a.getLabel());
                        filter.setName(a.getName());
                    }
                    throw new CiEntityDuplicateException(ciEntityConditionVo, dataObj);
                }
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiId(ciVo.getId());
                ciEntityTransactionVo.setAllowCommit(syncCiCollectionVo.getIsAutoCommit().equals(1));
                ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());//局部更新模式
                if (CollectionUtils.isEmpty(checkList)) {
                    ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                } else if (checkList.size() == 1) {
                    ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                    ciEntityTransactionVo.setCiEntityId(checkList.get(0).getId());
                }

                //拼凑更新字段
                for (SyncMappingVo mappingVo : syncCiCollectionVo.getMappingList()) {
                    if (mappingVo.getAttrId() != null && attrMap.containsKey(mappingVo.getAttrId())) {
                        AttrVo attrVo = attrMap.get(mappingVo.getAttrId());
                        if (dataObj.containsKey(mappingVo.getField(parentKey))) {
                            if (attrVo.isNeedTargetCi()) {
                                //引用属性需要引用包含subset的数据
                                if (dataObj.get(mappingVo.getField(parentKey)) instanceof JSONArray) {
                                    JSONArray subDataList = dataObj.getJSONArray(mappingVo.getField(parentKey));
                                    JSONArray attrValueList = new JSONArray();
                                    for (int i = 0; i < subDataList.size(); i++) {
                                        if (!(subDataList.get(i) instanceof JSONObject)) {
                                            throw new CollectionDataIrregularException(mappingVo.getField(parentKey), "json");
                                        }
                                        JSONObject subData = subDataList.getJSONObject(i);
                                        /*
                                        需要使用同一个集合下的映射关系，如果没有则不处理下一层数据，直接丢弃
                                         */
                                        SyncCiCollectionVo subSyncCiCollectionVo = getSyncCiCollection(attrVo.getTargetCiId(), syncCiCollectionVo.getCollectionName(), mappingVo.getField(parentKey));
                                        if (subSyncCiCollectionVo != null) {
                                            CiVo subCiVo = getCi(subSyncCiCollectionVo.getCiId());
                                            if (subCiVo.getIsVirtual().equals(1)) {
                                                throw new CiIsVirtualException(subCiVo.getLabel() + "(" + subCiVo.getName() + ")");
                                            }
                                            if (subCiVo.getIsAbstract().equals(1)) {
                                                throw new CiIsAbstractedException(subCiVo.getLabel() + "(" + subCiVo.getName() + ")");
                                            }
                                            //补充所有普通值数据进数据集，方便子对象引用父模型属性
                                            JSONObject subDataWithPK = new JSONObject();
                                            for (String subKey : subData.keySet()) {
                                                subDataWithPK.put(mappingVo.getField(parentKey) + "." + subKey, subData.get(subKey));
                                            }

                                            for (String key : dataObj.keySet()) {
                                                if (!(dataObj.get(key) instanceof JSONArray)) {
                                                    subDataWithPK.put(key, dataObj.get(key));
                                                }
                                            }
                                            CiEntityTransactionVo subCiEntityTransactionVo = generateCiEntityTransaction(subDataWithPK, subSyncCiCollectionVo, ciEntityTransactionMap, mappingVo.getField());
                                            if (subCiEntityTransactionVo != null) {
                                                if (ciEntityTransactionMap.containsKey(subCiEntityTransactionVo.getHash())) {
                                                    Long ceId = ciEntityTransactionMap.get(subCiEntityTransactionVo.getHash()).getCiEntityId();
                                                    if (!attrValueList.contains(ceId)) {
                                                        attrValueList.add(ceId);
                                                    }
                                                } else {
                                                    ciEntityTransactionMap.put(subCiEntityTransactionVo.getHash(), subCiEntityTransactionVo);
                                                    //ciEntityTransactionList.add(subCiEntityTransactionVo);
                                                    Long ceId = subCiEntityTransactionVo.getCiEntityId();
                                                    if (!attrValueList.contains(ceId)) {
                                                        attrValueList.add(ceId);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (CollectionUtils.isNotEmpty(attrValueList)) {
                                        ciEntityTransactionVo.addAttrEntityData(attrMap.get(mappingVo.getAttrId()), attrValueList);
                                    }
                                } else {
                                    //需要使用模型的唯一规则来查找配置项，如果找不到，或找到多个唯一表达式，就不做任何更新
                                    CiVo targetCiVo = getCi(attrVo.getTargetCiId());
                                    if (targetCiVo == null) {
                                        throw new CiNotFoundException(attrVo.getTargetCiId());
                                    }
                                    if (CollectionUtils.isEmpty(targetCiVo.getUniqueAttrIdList())) {
                                        throw new CiUniqueRuleNotFoundException(targetCiVo);
                                    }
                                    if (targetCiVo.getUniqueAttrIdList().size() > 1) {
                                        throw new CiMultipleUniqueRuleException(targetCiVo);
                                    }
                                    Long uniqueAttrId = targetCiVo.getUniqueAttrIdList().get(0);
                                    AttrVo targetNameAttrVo = attrMapper.getAttrById(uniqueAttrId);
                                    //如果引用了非subset数据，需要检查目标的名称属性是否可以写入数据（非表达式和引用属性），否则则放弃这个属性的导入
                                    if (!targetNameAttrVo.getType().equals("expression") && !targetNameAttrVo.isNeedTargetCi()) {
                                        CiEntityVo attrConditionVo = new CiEntityVo();
                                        attrConditionVo.setCiId(attrVo.getTargetCiId());
                                        AttrFilterVo filterVo = new AttrFilterVo();
                                        filterVo.setAttrId(uniqueAttrId);
                                        filterVo.setExpression(SearchExpression.EQ.getExpression());
                                        filterVo.setValueList(new ArrayList<String>() {{
                                            this.add(dataObj.getString(mappingVo.getField(parentKey)));
                                        }});
                                        attrConditionVo.addAttrFilter(filterVo);
                                        List<CiEntityVo> attrCiCheckList = searchCiEntityWithCache(attrConditionVo);//ciEntityService.searchCiEntity(attrConditionVo);
                                        if (CollectionUtils.isNotEmpty(attrCiCheckList) && attrCiCheckList.size() > 1) {
                                            //补充异常信息
                                            CiVo c = ciMapper.getCiById(attrConditionVo.getCiId());
                                            attrConditionVo.setCiName(c.getName());
                                            attrConditionVo.setCiLabel(c.getLabel());
                                            for (AttrFilterVo filter : attrConditionVo.getAttrFilterList()) {
                                                AttrVo a = attrMapper.getAttrById(filter.getAttrId());
                                                filter.setLabel(a.getLabel());
                                                filter.setName(a.getName());
                                            }
                                            throw new CiEntityDuplicateException(attrConditionVo, dataObj);
                                        }
                                        //添加目标属性事务
                                        CiEntityTransactionVo attrCiEntityTransactionVo = new CiEntityTransactionVo();
                                        attrCiEntityTransactionVo.setCiId(targetCiVo.getId());
                                        attrCiEntityTransactionVo.setAllowCommit(syncCiCollectionVo.getIsAutoCommit().equals(1));
                                        attrCiEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());//局部更新模式

                                        if (CollectionUtils.isEmpty(attrCiCheckList)) {
                                            attrCiEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                                        } else if (attrCiCheckList.size() == 1) {
                                            attrCiEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                                            attrCiEntityTransactionVo.setCiEntityId(attrCiCheckList.get(0).getId());
                                        }
                                        JSONArray targetAttrValueList = new JSONArray();
                                        targetAttrValueList.add(dataObj.getString(mappingVo.getField(parentKey)));
                                        attrCiEntityTransactionVo.addAttrEntityData(targetNameAttrVo, targetAttrValueList);
                                        attrCiEntityTransactionVo.setUniqueAttrIdList(getCi(attrCiEntityTransactionVo.getCiId()).getUniqueAttrIdList());

                                        JSONArray attrValueList = new JSONArray();
                                        if (ciEntityTransactionMap.containsKey(attrCiEntityTransactionVo.getHash())) {
                                            attrValueList.add(ciEntityTransactionMap.get(attrCiEntityTransactionVo.getHash()).getCiEntityId());
                                        } else {
                                            ciEntityTransactionMap.put(attrCiEntityTransactionVo.getHash(), attrCiEntityTransactionVo);
                                            attrValueList.add(attrCiEntityTransactionVo.getCiEntityId());
                                        }

                                        ciEntityTransactionVo.addAttrEntityData(attrMap.get(mappingVo.getAttrId()), attrValueList);
                                    } else {
                                        throw new CiUniqueRuleAttrTypeIrregularException(targetCiVo, targetNameAttrVo);
                                    }
                                }
                            } else {
                                ciEntityTransactionVo.addAttrEntityData(attrMap.get(mappingVo.getAttrId()), dataObj.get(mappingVo.getField(parentKey)));
                            }
                        }
                    } else if (mappingVo.getRelId() != null && relMap.containsKey(mappingVo.getRelId())) {
                        if (dataObj.containsKey(mappingVo.getField(parentKey))) {
                        /*
                        由于模型关系的对端模型可能是父模型，而采集数据有可能是各个不同的子模型，因此需要做如下处理：
                        1、通过jsonArray数据成员中的_OBJ_TYPE找到相应的主动采集配置模型（已经规定一个集合只能关联一个主动采集配置模型）。
                        2、如果找到模型，则检查找到的模型是否属于关系对端模型的子模型（子模型列表包括自己）。
                        3、如果2成立，则检查关系对端模型（可能是父模型）是否配置了当前集合的被动采集配置。
                        4、如果有3成立，则把采集配置的父模型id切换成第一步中真正的子模型id。
                        5、把修改后的映射配置传下去继续下一步数据同步操作。
                        以上任意一步不满足或找不到，则这部分数据不再同步
                         */
                            RelVo relVo = relMap.get(mappingVo.getRelId());
                            Long ciId = mappingVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId();
                            JSONArray subDataList = null;
                            //兼容一下jsonObject，这个是mongodb中数据结构不统一导致，正常情况不会出现jsonObject
                            if (dataObj.get(mappingVo.getField(parentKey)) instanceof JSONArray) {
                                subDataList = dataObj.getJSONArray(mappingVo.getField(parentKey));
                            } else if (dataObj.get(mappingVo.getField(parentKey)) instanceof JSONObject) {
                                subDataList = new JSONArray();
                                subDataList.add(dataObj.getJSONObject(mappingVo.getField(parentKey)));
                            }
                            if (subDataList != null) {
                                for (int i = 0; i < subDataList.size(); i++) {
                                    JSONObject subData = subDataList.getJSONObject(i);
                                   /*
                                     需要使用同一个集合下的映射关系，如果没有则不处理下一层数据，直接丢弃
                                     */
                                    String subCollectionName = subData.getString("_OBJ_TYPE");
                                    if (StringUtils.isNotBlank(subCollectionName)) {
                                        SyncCiCollectionVo subInitiativeSyncCiCollection = getInitiativeSyncCiCollection(subCollectionName);
                                        if (subInitiativeSyncCiCollection != null) {
                                            Long subCiId = subInitiativeSyncCiCollection.getCiId();
                                            List<CiVo> downCiList = getDownwardCiList(ciId);
                                            if (downCiList.stream().anyMatch(d -> d.getId().equals(subCiId))) {
                                                //找到关系原始模型id在当前集合下的映射关系
                                                SyncCiCollectionVo subSyncCiCollectionVo = getSyncCiCollection(subCiId, syncCiCollectionVo.getCollectionName(), mappingVo.getField(parentKey));
                                                //先根据obj_type匹配到模型id去寻找是否配了被动采集，如果没有，就用到关系的原始模型（父模型）去寻找是否有配置被动采集
                                                if (subSyncCiCollectionVo == null) {
                                                    subSyncCiCollectionVo = getSyncCiCollection(ciId, syncCiCollectionVo.getCollectionName(), mappingVo.getField(parentKey));
                                                }
                                                if (subSyncCiCollectionVo != null) {
                                                    CiVo subCiVo = getCi(subSyncCiCollectionVo.getCiId());
                                                    if (subCiVo.getIsVirtual().equals(1)) {
                                                        throw new CiIsVirtualException(subCiVo.getLabel() + "(" + subCiVo.getName() + ")");
                                                    }
                                                    if (subCiVo.getIsAbstract().equals(1)) {
                                                        throw new CiIsAbstractedException(subCiVo.getLabel() + "(" + subCiVo.getName() + ")");
                                                    }
                                                    JSONObject subDataWithPK = new JSONObject();
                                                    for (String subKey : subData.keySet()) {
                                                        subDataWithPK.put(mappingVo.getField(parentKey) + "." + subKey, subData.get(subKey));
                                                    }
                                                    //补充所有普通值数据进数据集，方便子对象引用父模型属性
                                                    for (String key : dataObj.keySet()) {
                                                        if (!(dataObj.get(key) instanceof JSONArray)) {
                                                            subDataWithPK.put(key, dataObj.get(key));
                                                        }
                                                    }
                                                    //切换关系原始模型id为真正的子模型id，否则数据找不到真正的模型id
                                                    subSyncCiCollectionVo.setCiId(subCiId);
                                                    CiEntityTransactionVo subCiEntityTransactionVo = generateCiEntityTransaction(subDataWithPK, subSyncCiCollectionVo, ciEntityTransactionMap, mappingVo.getField());
                                                    if (subCiEntityTransactionVo != null) {
                                                        if (ciEntityTransactionMap.containsKey(subCiEntityTransactionVo.getHash())) {
                                                            ciEntityTransactionVo.addRelEntityData(relVo, mappingVo.getDirection(), subCiId, ciEntityTransactionMap.get(subCiEntityTransactionVo.getHash()).getCiEntityId(), mappingVo.getAction());
                                                        } else {
                                                            ciEntityTransactionMap.put(subCiEntityTransactionVo.getHash(), subCiEntityTransactionVo);
                                                            ciEntityTransactionVo.addRelEntityData(relVo, mappingVo.getDirection(), subCiId, subCiEntityTransactionVo.getCiEntityId(), mappingVo.getAction());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //设置唯一属性列表，用来生成CiEntityTransaction哈希用
                ciEntityTransactionVo.setUniqueAttrIdList(getCi(ciEntityTransactionVo.getCiId()).getUniqueAttrIdList());
                return ciEntityTransactionVo;
            }
            return null;
        }

        private CollectionVo getCollectionByName(String name) {
            if (CollectionUtils.isNotEmpty(collectionList)) {
                Optional<CollectionVo> op = collectionList.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
                if (op.isPresent()) {
                    return op.get();
                }
            }
            throw new CollectionNotFoundException(name);
        }

        /**
         * 单个执行方式
         */
        private void executeBySingleMode() {
            Set<String> fieldList = new HashSet<>();
            if (CollectionUtils.isNotEmpty(syncCiCollectionList) && MapUtils.isNotEmpty(singleDataObj)) {
                for (SyncCiCollectionVo syncCiCollectionVo : syncCiCollectionList) {
                    try {
                        for (SyncMappingVo syncMappingVo : syncCiCollectionVo.getMappingList()) {
                            if (StringUtils.isNotBlank(syncMappingVo.getField())) {
                                fieldList.add(syncMappingVo.getField().trim());
                            }
                        }
                        JSONArray tmpDataList = new JSONArray();
                        tmpDataList.add(singleDataObj);
                        JSONArray finalDataList = flattenJson(tmpDataList, fieldList, null);
                        for (int i = 0; i < finalDataList.size(); i++) {
                            JSONObject dataObj = finalDataList.getJSONObject(i);
                            //用于存放一样的配置项事务，当关联到相同的配置项时只会增加一次
                            Map<Integer, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
                            try {
                                CiEntityTransactionVo ciEntityTransactionVo = generateCiEntityTransaction(dataObj, syncCiCollectionVo, ciEntityTransactionMap, null);
                                if (ciEntityTransactionVo != null && !ciEntityTransactionMap.containsKey(ciEntityTransactionVo.getHash())) {
                                    ciEntityTransactionMap.put(ciEntityTransactionVo.getHash(), ciEntityTransactionVo);
                                }
                                List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                                for (Integer key : ciEntityTransactionMap.keySet()) {
                                    ciEntityTransactionList.add(ciEntityTransactionMap.get(key));
                                }
                                ciEntityService.saveCiEntityWithoutTransaction(ciEntityTransactionList, syncCiCollectionVo.getTransactionGroup());
                            } catch (ApiRuntimeException ex) {
                                logger.warn(ex.getMessage(), ex);
                                syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                                syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                            }
                        }
                    } catch (ApiRuntimeException ex) {
                        logger.warn(ex.getMessage(), ex);
                        syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());

                    } finally {
                        syncCiCollectionVo.getSyncAudit().setStatus(SyncStatus.DONE.getValue());
                        syncAuditMapper.updateSyncAuditStatus(syncCiCollectionVo.getSyncAudit());
                    }
                }
            }
        }

        private Date convertToIsoDate(Date date) {
            Date finalDate = null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                finalDate = sdf.parse(sdf.format(date));
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            return finalDate;
        }


        /**
         * 批量执行方式
         */
        private void executeByBatchMode() {
            if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
                for (SyncCiCollectionVo syncCiCollectionVo : syncCiCollectionList) {
                    try {
                        CollectionVo collectionVo = getCollectionByName(syncCiCollectionVo.getCollectionName());

                        CiVo ciVo = getCi(syncCiCollectionVo.getCiId());
                        if (ciVo.getIsVirtual().equals(1)) {
                            throw new CiIsVirtualException(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                        }
                        if (ciVo.getIsAbstract().equals(1)) {
                            throw new CiIsAbstractedException(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                        }

                        Criteria finalCriteria = new Criteria();
                        List<Criteria> criteriaList = new ArrayList<>();
                        criteriaList.add(collectionVo.getFilterCriteria());

                        if (CollectionUtils.isNotEmpty(syncCiCollectionVo.getMappingList())) {
                            Set<String> fieldList = new HashSet<>();
                            for (SyncMappingVo syncMappingVo : syncCiCollectionVo.getMappingList()) {
                                if (StringUtils.isNotBlank(syncMappingVo.getField())) {
                                    fieldList.add(syncMappingVo.getField().trim());
                                }
                            }
                            if (CollectionUtils.isNotEmpty(fieldList)) {
                                Query query = new Query();
                                if (syncCiCollectionVo.getSyncPolicy() != null) {
                                    criteriaList.add(syncCiCollectionVo.getSyncPolicy().getCriteria());
                                }
                                //只更新上次同步以来新的集合数据进行同步
                                if (syncCiCollectionVo.getLastSyncDate() != null) {
                                    criteriaList.add(Criteria.where("_updatetime").gt(convertToIsoDate(syncCiCollectionVo.getLastSyncDate())));
                                }
                                //#############测试用条件，使用后注释掉
                                //criteriaList.add(Criteria.where("SERVICE_NAME").is("sgcrdb"));
                                //#############测试用条件
                                finalCriteria.andOperator(criteriaList);
                                query.addCriteria(finalCriteria);
                                int batchSize = 500;//游标每次读取500条数据
                                AtomicInteger count = new AtomicInteger(0);
                                int counter = 0;
                                try (MongoCursor<Document> cursor = mongoTemplate.getCollection(collectionVo.getCollection()).find(query.getQueryObject()).noCursorTimeout(true).batchSize(batchSize).cursor()) {
                                    List<JSONObject> dataList = new ArrayList<>();
                                    while (cursor.hasNext()) {
                                        counter += 1;
                                        String jsonStr = cursor.next().toJson();

                                        if (StringUtils.isNotBlank(collectionVo.getDocroot())) {
                                            JSONArray objList = (JSONArray) JSONPath.read(jsonStr, "$." + collectionVo.getDocroot());
                                            for (int i = 0; i < objList.size(); i++) {
                                                dataList.add(objList.getJSONObject(i));
                                            }
                                        } else {
                                            JSONObject orgDataObj = JSONObject.parseObject(jsonStr);
                                            dataList.add(orgDataObj);
                                        }

                                        //到达batchSize先处理一部分
                                        if (counter == batchSize) {
                                            dealWithDataBatch(syncCiCollectionVo, fieldList, dataList, count);
                                            dataList = new ArrayList<>();
                                            counter = 0;
                                        }
                                    }
                                    //处理剩余的数据
                                    if (CollectionUtils.isNotEmpty(dataList)) {
                                        dealWithDataBatch(syncCiCollectionVo, fieldList, dataList, count);
                                    }
                                    syncCiCollectionVo.getSyncAudit().setDataCount(count.get());
                                }
                            }
                        }
                    } catch (ApiRuntimeException ex) {
                        logger.warn(ex.getMessage(), ex);
                        syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        if (StringUtils.isNotBlank(ex.getMessage())) {
                            syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                        } else {
                            syncCiCollectionVo.getSyncAudit().appendError(ExceptionUtils.getStackTrace(ex));
                        }
                    } finally {
                        syncCiCollectionVo.getSyncAudit().setStatus(SyncStatus.DONE.getValue());
                        syncAuditMapper.updateSyncAuditStatus(syncCiCollectionVo.getSyncAudit());
                        //如果没有异常才会更新最后同步时间，作为下一次同步的过滤
                        if (StringUtils.isBlank(syncCiCollectionVo.getSyncAudit().getError())) {
                            syncMapper.updateSyncCiCollectionLastSyncDate(syncCiCollectionVo.getId());
                        }
                    }
                }
            }
        }

        /**
         * 并发处理所有采集数据
         */
        private void dealWithDataBatch(SyncCiCollectionVo syncCiCollectionVo, Set<String> fieldList, List<JSONObject> dataList, AtomicInteger count) {
            if (CollectionUtils.isNotEmpty(dataList)) {
                BatchRunner<JSONObject> batchRunner = new BatchRunner<>();
                BatchRunner.State state = batchRunner.execute(dataList, 3, data -> {
                    int tmpCount = count.addAndGet(1);
                    long startTime = 0L;
                    if (logger.isInfoEnabled()) {
                        logger.info("开始处理第" + tmpCount + "条数据");
                        startTime = System.currentTimeMillis();
                        logger.info("mongodb游标数据读取耗时：" + (System.currentTimeMillis() - startTime) + "ms");
                    }

                    JSONArray tmpDataList = new JSONArray();
                    tmpDataList.add(data);
                    JSONArray finalDataList = flattenJson(tmpDataList, fieldList, null);
                    for (int i = 0; i < finalDataList.size(); i++) {
                        JSONObject dataObj = finalDataList.getJSONObject(i);
                        //需要严格按照写入的先后顺序生成list，否则后期写入关系数据时，会因为被引用配置项还不存在而导致清除掉关系。
                        Map<Integer, CiEntityTransactionVo> ciEntityTransactionVoMap = new LinkedHashMap<>();

                        try {
                            if (logger.isInfoEnabled()) {
                                startTime = System.currentTimeMillis();
                            }
                            CiEntityTransactionVo ciEntityTransactionVo = this.generateCiEntityTransaction(dataObj, syncCiCollectionVo, ciEntityTransactionVoMap, null);
                            if (ciEntityTransactionVo != null && !ciEntityTransactionVoMap.containsKey(ciEntityTransactionVo.getHash())) {
                                ciEntityTransactionVoMap.put(ciEntityTransactionVo.getHash(), ciEntityTransactionVo);
                            }
                            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                            for (Integer key : ciEntityTransactionVoMap.keySet()) {
                                ciEntityTransactionList.add(ciEntityTransactionVoMap.get(key));
                            }
                            if (logger.isInfoEnabled()) {
                                logger.info("创建了" + ciEntityTransactionList.size() + "个事务，耗时：" + (System.currentTimeMillis() - startTime) + "ms");
                            }

                            if (logger.isInfoEnabled()) {
                                startTime = System.currentTimeMillis();
                            }

                            ciEntityService.saveCiEntityWithoutTransaction(ciEntityTransactionList, syncCiCollectionVo.getTransactionGroup());
                            if (logger.isInfoEnabled()) {
                                logger.info("处理了" + ciEntityTransactionList.size() + "个事务，耗时：" + (System.currentTimeMillis() - startTime) + "ms");
                            }
                        } catch (ApiRuntimeException ex) {
                            logger.warn(ex.getMessage(), ex);
                            syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                        }
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("第" + tmpCount + "条数据处理完成，耗时：" + (System.currentTimeMillis() - startTime) + "ms");
                    }
                }, "SYNC-BATCH-HANDLER");
                if (!state.isSucceed()) {
                    if (state.getException() != null) {
                        throw new ApiRuntimeException(state.getException().getMessage());
                    }
                }
            }
        }

        @Override
        protected void execute() {
            InputFromContext.init(InputFrom.AUTOEXEC);
            if (mode.equals("batch")) {
                executeByBatchMode();
            } else {
                executeBySingleMode();
            }
        }
    }


    /**
     * 根据mapping字段扁平化json
     * 例如原json是：
     * [{"a":"A","b":[{"subB":"BA"},{"subB":"BB"}]}]
     * 使用mapping字段"a","b.subB"转化后得到：
     * [{"a":"A","b.subB":"BA"},{"a":"A","b.subB":"BB"}]
     *
     * @param dataList  原始数据
     * @param fieldList 字段列表
     * @param parentKey 上层key
     * @return 扁平化后的数据
     */
    private static JSONArray flattenJson(JSONArray dataList, Set<String> fieldList, String parentKey) {
        Set<Object> finalDataList = new HashSet<>();
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i) instanceof JSONObject) {
                JSONObject data = dataList.getJSONObject(i);
                JSONObject returnData = new JSONObject();
                JSONArray returnDataList = new JSONArray();

                for (String key : data.keySet()) {
                    if (StringUtils.isNotBlank(parentKey)) {
                        if (fieldList.contains(parentKey + "." + key)) {
                            returnData.put(parentKey + "." + key, data.get(key));
                        }
                    } else {
                        if (fieldList.contains(key)) {
                            returnData.put(key, data.get(key));
                        }
                    }
                }

                if (MapUtils.isNotEmpty(returnData)) {
                    returnDataList.add(returnData);
                }

                for (String key : data.keySet()) {
                    if (data.get(key) instanceof JSONArray) {
                        JSONArray subDataList = flattenJson(data.getJSONArray(key), fieldList, StringUtils.isNotBlank(parentKey) ? parentKey + "." + key : key);
                        JSONArray tmpList = new JSONArray();
                        if (CollectionUtils.isNotEmpty(returnDataList)) {
                            for (int r = 0; r < returnDataList.size(); r++) {
                                JSONObject rData = JSONObject.parseObject(returnDataList.getJSONObject(r).toJSONString());
                                if (CollectionUtils.isNotEmpty(subDataList)) {
                                    for (int s = 0; s < subDataList.size(); s++) {
                                        JSONObject subData = JSONObject.parseObject(subDataList.getJSONObject(s).toJSONString());
                                        subData.putAll(rData);
                                        if (MapUtils.isNotEmpty(subData)) {
                                            tmpList.add(subData);
                                        }
                                    }
                                } else {
                                    if (MapUtils.isNotEmpty(rData)) {
                                        tmpList.add(rData);
                                    }
                                }
                            }
                        } else {
                            if (CollectionUtils.isNotEmpty(subDataList)) {
                                for (int s = 0; s < subDataList.size(); s++) {
                                    JSONObject subData = JSONObject.parseObject(subDataList.getJSONObject(s).toJSONString());
                                    if (MapUtils.isNotEmpty(subData)) {
                                        tmpList.add(subData);
                                    }
                                }
                            }
                        }
                        returnDataList = tmpList;
                    }
                }
                finalDataList.addAll(returnDataList);
            }
        }
        return JSONArray.parseArray(JSONArray.toJSONString(finalDataList));
    }

    public static void doSync(JSONObject dataObj, List<SyncCiCollectionVo> syncCiCollectionList) {
        if (CollectionUtils.isNotEmpty(syncCiCollectionList) && MapUtils.isNotEmpty(dataObj)) {
            List<SyncCiCollectionVo> syncList = new ArrayList<>();
            Set<String> keys = dataObj.keySet();
            COLLECTION:
            for (SyncCiCollectionVo syncCiCollectionVo : syncCiCollectionList) {
                    /*
                    检查当前映射是否包含目标模型的所有唯一属性，并且数据中叶包含这些属性的对应值，是就开始同步，否则视为同步条件不满足
                     */
                Long ciId = syncCiCollectionVo.getCiId();
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId).stream().filter(d -> d.getIsCiUnique().equals(1)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(attrList) && CollectionUtils.isNotEmpty(syncCiCollectionVo.getMappingList())) {
                    for (AttrVo attrVo : attrList) {
                        Optional<SyncMappingVo> op = syncCiCollectionVo.getMappingList().stream().filter(d -> d.getAttrId().equals(attrVo.getId())).findFirst();
                        if (!op.isPresent() || !keys.contains(op.get().getField())) {
                            continue COLLECTION;
                        }
                    }

                    TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                    SyncAuditVo syncAuditVo = new SyncAuditVo();
                    syncAuditVo.setStatus(SyncStatus.DOING.getValue());
                    syncAuditVo.setCiCollectionId(syncCiCollectionVo.getId());
                    syncAuditVo.setTransactionGroupId(transactionGroupVo.getId());
                    syncAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                    syncAuditMapper.insertSyncAudit(syncAuditVo);
                    syncCiCollectionVo.setSyncAudit(syncAuditVo);
                    syncCiCollectionVo.setTransactionGroup(transactionGroupVo);
                    syncList.add(syncCiCollectionVo);
                }
            }

            if (CollectionUtils.isNotEmpty(syncList)) {
                SyncHandler handler = new SyncHandler(dataObj, syncList);
                CachedThreadPool.execute(handler);
            }
        }
    }

    public static void doSync(List<SyncCiCollectionVo> syncCiCollectionList) {
        if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
            List<SyncCiCollectionVo> initiativeCollectList = syncCiCollectionList.stream().filter(s -> s.getCollectMode().equals(CollectMode.INITIATIVE.getValue())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(initiativeCollectList)) {
                List<SyncCiCollectionVo> syncList = new ArrayList<>();
                for (SyncCiCollectionVo syncCiCollectionVo : initiativeCollectList) {
                    if (CollectionUtils.isEmpty(syncAuditMapper.getDoingSyncByCiId(syncCiCollectionVo.getCiId()))) {
                        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
                        SyncAuditVo syncAuditVo = new SyncAuditVo();
                        syncAuditVo.setStatus(SyncStatus.DOING.getValue());
                        syncAuditVo.setCiCollectionId(syncCiCollectionVo.getId());
                        syncAuditVo.setTransactionGroupId(transactionGroupVo.getId());
                        syncAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                        syncAuditMapper.insertSyncAudit(syncAuditVo);
                        syncCiCollectionVo.setSyncAudit(syncAuditVo);
                        syncCiCollectionVo.setTransactionGroup(transactionGroupVo);
                        syncList.add(syncCiCollectionVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(syncList)) {
                    SyncHandler handler = new SyncHandler(syncList);
                    CachedThreadPool.execute(handler);
                }
            }
        }
    }
}
