/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.sync;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.sync.SyncAuditVo;
import codedriver.framework.cmdb.dto.sync.SyncCiCollectionVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.sync.CollectMode;
import codedriver.framework.cmdb.enums.sync.SyncStatus;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.sync.CiEntityDuplicateException;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        private final Map<Long, CiVo> CiMap = new HashMap<>();
        private final List<SyncCiCollectionVo> syncCiCollectionList;

        public SyncHandler(List<SyncCiCollectionVo> syncCiCollectionVoList) {
            this.syncCiCollectionList = syncCiCollectionVoList;
        }

        private CiVo getCi(Long ciId) {
            if (!this.CiMap.containsKey(ciId)) {
                CiVo ciVo = ciMapper.getCiById(ciId);
                if (ciVo == null) {
                    throw new CiNotFoundException(ciId);
                }
                CiMap.put(ciId, ciVo);
            }
            return this.CiMap.get(ciId);
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
         * 根据数据生成配置项事务，由于可能出现关联配置项，因此有可能返回多个事务
         *
         * @param dataObj            数据
         * @param syncCiCollectionVo 映射配置
         * @param parentKey          上一层数据key，如果是关联数据就会有这个参数
         * @return 配置项事务
         */
        private List<CiEntityTransactionVo> generateCiEntityTransaction(JSONObject dataObj, SyncCiCollectionVo syncCiCollectionVo, String parentKey) {
            List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            Map<Long, AttrVo> attrMap = getAttrMap(syncCiCollectionVo.getCiId());
            Map<Long, RelVo> relMap = getRelMap(syncCiCollectionVo.getCiId());
            CiVo ciVo = getCi(syncCiCollectionVo.getCiId());
            ciEntityConditionVo.setCiId(ciVo.getId());
            /*
            需要使用模型的唯一规则来查找配置项，如果找不到，就不做任何更新
             */
            if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
                for (Long uniqueAttrId : ciVo.getUniqueAttrIdList()) {
                    SyncMappingVo syncMappingVo = syncCiCollectionVo.getMappingByAttrId(uniqueAttrId);
                    if (syncMappingVo.getAttrId() != null) {
                        AttrFilterVo filterVo = new AttrFilterVo();
                        AttrVo attr = attrMap.get(syncMappingVo.getAttrId());
                        if (attr != null && !attr.isNeedTargetCi()) {
                            if (dataObj.containsKey(syncMappingVo.getField(parentKey))) {
                                filterVo.setAttrId(syncMappingVo.getAttrId());
                                filterVo.setExpression(SearchExpression.EQ.getExpression());
                                String v = dataObj.getString(syncMappingVo.getField(parentKey));
                                if (StringUtils.isNotBlank(v)) {
                                    filterVo.setValueList(new ArrayList<String>() {{
                                        this.add(v);
                                    }});
                                    ciEntityConditionVo.addAttrFilter(filterVo);
                                }
                            }
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                    //使用所有非引用属性去搜索配置项，没有则添加，发现一个则就修改，发现多个就抛异常
                    List<CiEntityVo> checkList = ciEntityService.searchCiEntity(ciEntityConditionVo);
                    CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                    ciEntityTransactionVo.setCiId(ciVo.getId());
                    ciEntityTransactionVo.setAllowCommit(syncCiCollectionVo.getIsAutoCommit().equals(1));
                    if (CollectionUtils.isEmpty(checkList)) {
                        ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                    } else if (checkList.size() == 1) {
                        ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                        ciEntityTransactionVo.setCiEntityId(checkList.get(0).getId());
                    } else {
                        throw new CiEntityDuplicateException();
                    }

                    //拼凑更新字段
                    for (SyncMappingVo mappingVo : syncCiCollectionVo.getMappingList()) {
                        if (mappingVo.getAttrId() != null && attrMap.containsKey(mappingVo.getAttrId())) {
                            AttrVo attrVo = attrMap.get(mappingVo.getAttrId());
                            if (attrVo.isNeedTargetCi()) {
                                //引用属性需要引用下一层数据
                                if (dataObj.get(mappingVo.getField(parentKey)) instanceof JSONArray) {
                                    JSONArray subDataList = dataObj.getJSONArray(mappingVo.getField(parentKey));
                                    for (int i = 0; i < subDataList.size(); i++) {
                                        JSONObject subData = subDataList.getJSONObject(i);
                                        /*
                                        需要使用同一个集合下的映射关系，如果没有则不处理下一层数据，直接丢弃
                                         */
                                        SyncCiCollectionVo subSyncCiCollectionVo = syncMapper.getSyncCiCollectionByCiIdAndCollectionName(attrVo.getTargetCiId(), syncCiCollectionVo.getCollectionName());
                                        if (subSyncCiCollectionVo != null) {
                                            List<CiEntityTransactionVo> subCiEntityTransactionList = generateCiEntityTransaction(subData, subSyncCiCollectionVo, mappingVo.getField());
                                            if (CollectionUtils.isNotEmpty(subCiEntityTransactionList)) {
                                                JSONArray attrValueList = new JSONArray();
                                                for (CiEntityTransactionVo subCiEntityTransactionVo : subCiEntityTransactionList) {
                                                    ciEntityTransactionList.add(subCiEntityTransactionVo);
                                                    attrValueList.add(ciEntityTransactionVo.getId());
                                                }
                                                ciEntityTransactionVo.addAttrEntityData(mappingVo.getAttrId(), attrMap.get(mappingVo.getAttrId()), attrValueList);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (dataObj.containsKey(mappingVo.getField(parentKey))) {
                                    ciEntityTransactionVo.addAttrEntityData(mappingVo.getAttrId(), attrMap.get(mappingVo.getAttrId()), dataObj.get(mappingVo.getField(parentKey)));
                                }
                            }
                        } else if (mappingVo.getRelId() != null && relMap.containsKey(mappingVo.getRelId())) {
                            RelVo relVo = relMap.get(mappingVo.getRelId());
                            Long ciId = mappingVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId();
                            SyncCiCollectionVo subSyncCiCollectionVo = syncMapper.getSyncCiCollectionByCiIdAndCollectionName(ciId, syncCiCollectionVo.getCollectionName());
                            if (subSyncCiCollectionVo != null && dataObj.get(mappingVo.getField(parentKey)) instanceof JSONArray) {
                                JSONArray subDataList = dataObj.getJSONArray(mappingVo.getField(parentKey));
                                for (int i = 0; i < subDataList.size(); i++) {
                                    JSONObject subData = subDataList.getJSONObject(i);
                                   /*
                                     需要使用同一个集合下的映射关系，如果没有则不处理下一层数据，直接丢弃
                                     */
                                    List<CiEntityTransactionVo> subCiEntityTransactionList = generateCiEntityTransaction(subData, subSyncCiCollectionVo, mappingVo.getField());
                                    if (CollectionUtils.isNotEmpty(subCiEntityTransactionList)) {
                                        for (CiEntityTransactionVo subCiEntityTransactionVo : subCiEntityTransactionList) {
                                            ciEntityTransactionList.add(subCiEntityTransactionVo);
                                            ciEntityTransactionVo.addRelEntityData(relVo, mappingVo.getDirection(), ciId, subCiEntityTransactionVo.getCiEntityId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ciEntityTransactionList.add(ciEntityTransactionVo);
                }
            }
            return ciEntityTransactionList;
        }


        @Override
        protected void execute() {
            if (CollectionUtils.isNotEmpty(syncCiCollectionList)) {
                for (SyncCiCollectionVo syncCiCollectionVo : syncCiCollectionList) {
                    try {
                        if (CollectionUtils.isNotEmpty(syncCiCollectionVo.getMappingList())) {
                            Set<String> fieldList = new HashSet<>();
                            for (SyncMappingVo syncMappingVo : syncCiCollectionVo.getMappingList()) {
                                if (StringUtils.isNotBlank(syncMappingVo.getField())) {
                                    fieldList.add(syncMappingVo.getField().trim());
                                }
                            }
                            if (CollectionUtils.isNotEmpty(fieldList)) {
                                int pageSize = 100;
                                long currentPage = 1;
                                Query query = new Query();
                                if (syncCiCollectionVo.getSyncPolicy() != null) {
                                    query = syncCiCollectionVo.getSyncPolicy().getQuery();
                                }
                                query.limit(pageSize);
                                List<JSONObject> dataList = mongoTemplate.find(query, JSONObject.class, syncCiCollectionVo.getCollectionName());
                                if (CollectionUtils.isNotEmpty(dataList)) {
                                    while (CollectionUtils.isNotEmpty(dataList)) {
                                        for (JSONObject orgDataObj : dataList) {
                                            JSONArray tmpDataList = new JSONArray();
                                            tmpDataList.add(orgDataObj);
                                            JSONArray finalDataList = flattenJson(tmpDataList, fieldList, null);
                                            for (int i = 0; i < finalDataList.size(); i++) {
                                                JSONObject dataObj = finalDataList.getJSONObject(i);
                                                List<CiEntityTransactionVo> ciEntityTransactionList = this.generateCiEntityTransaction(dataObj, syncCiCollectionVo, null);
                                                try {
                                                    ciEntityService.saveCiEntity(ciEntityTransactionList, syncCiCollectionVo.getTransactionGroup());
                                                } catch (Exception ex) {
                                                    syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                                                    if (!(ex instanceof ApiRuntimeException)) {
                                                        logger.error(ex.getMessage(), ex);
                                                    }
                                                }
                                            }
                                        }

                                        currentPage += 1;
                                        query.skip(pageSize * (currentPage - 1));
                                        dataList = mongoTemplate.find(query, JSONObject.class, syncCiCollectionVo.getCollectionName());
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        syncCiCollectionVo.getSyncAudit().appendError(ex.getMessage());
                        if (!(ex instanceof ApiRuntimeException)) {
                            logger.error(ex.getMessage(), ex);
                        }
                    } finally {
                        syncCiCollectionVo.getSyncAudit().setStatus(SyncStatus.DONE.getValue());
                        syncAuditMapper.updateSyncAuditStatus(syncCiCollectionVo.getSyncAudit());
                    }
                }
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
                    if (!(data.get(key) instanceof JSONArray)) {
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
                }
                if (MapUtils.isNotEmpty(returnData)) {
                    returnDataList.add(returnData);
                }
                for (String key : data.keySet()) {
                    if (data.get(key) instanceof JSONArray) {
                        if (fieldList.contains(StringUtils.isNotBlank(parentKey) ? parentKey + "." + key : key)) {
                            returnData.put(StringUtils.isNotBlank(parentKey) ? parentKey + "." + key : key, data.get(key));
                        }
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


    public static void main(String[] argvs) {
        JSONObject jsonObj = JSONObject.parseObject("" +
                "{\n" +
                "    \"VCENTER_IP\": \"192.168.0.48\",\n" +
                "    \"IP_ADDRS\": [\n" +
                "        {\n" +
                "            \"NAME\": \"192.168.0.26\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"SWAP_FREE\": 1979.281,\n" +
                "    \"IS_VIRTUAL\": 1,\n" +
                "    \"IPV6_ADDRS\": [\n" +
                "        {\n" +
                "            \"NAME\": \"fe80::250:56ff:fea1:ebd2\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"MEM_BUFFERS\": 0.023,\n" +
                "    \"MACHINE_ID\": \"99eebc4953394a6f8b5ae644778cbb1a\",\n" +
                "    \"FIREWALL_ENABLE\": 0,\n" +
                "    \"MAX_OPEN_FILES\": 1610558,\n" +
                "    \"OS_TYPE\": \"Linux\",\n" +
                "    \"IP\": \"192.168.0.26\",\n" +
                "    \"MACHINE_UUID\": \"4c4c4544-004a-4610-8046-c4c04f463358\",\n" +
                "    \"SYS_VENDOR\": \"VMware, Inc.\",\n" +
                "    \"MGMT_IP\": \"192.168.0.26\",\n" +
                "    \"PRODUCT_NAME\": \"VMware Virtual Platform\",\n" +
                "    \"NAME\": \"0.26_demo_prd\",\n" +
                "    \"MEM_AVAILABLE\": 5900.934,\n" +
                "    \"NTP_SERVERS\": [\n" +
                "        {\n" +
                "            \"NAME\": \"202.112.10.36\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"_lasttime\": 1629302873000,\n" +
                "    \"DISKS\": [\n" +
                "        {\n" +
                "            \"TYPE\": \"local\",\n" +
                "            \"UNIT\": \"GB\",\n" +
                "            \"NAME\": \"/dev/sda\",\n" +
                "            \"CAPACITY\": 53.7\n" +
                "        },\n" +
                "        {\n" +
                "            \"NAME\": \"/dev/sdb\",\n" +
                "            \"CAPACITY\": 107.4,\n" +
                "            \"UNIT\": \"GB\",\n" +
                "            \"TYPE\": \"local\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"TYPE\": \"lvm\",\n" +
                "            \"UNIT\": \"GB\",\n" +
                "            \"CAPACITY\": 157.8,\n" +
                "            \"NAME\": \"/dev/mapper/cl-root\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"TYPE\": \"lvm\",\n" +
                "            \"UNIT\": \"GB\",\n" +
                "            \"NAME\": \"/dev/mapper/cl-swap\",\n" +
                "            \"CAPACITY\": 2.147\n" +
                "        }\n" +
                "    ],\n" +
                "    \"CPU_CORES\": 4,\n" +
                "    \"_id\": {\n" +
                "        \"date\": 1629171794000,\n" +
                "        \"timestamp\": 1629171794\n" +
                "    },\n" +
                "    \"USERS\": [\n" +
                "        {\n" +
                "            \"HOME\": \"/root\",\n" +
                "            \"NAME\": \"root\",\n" +
                "            \"GID\": \"0\",\n" +
                "            \"UID\": \"0\",\n" +
                "            \"SHELL\": \"/bin/bash\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"HOME\": \"/\",\n" +
                "            \"NAME\": \"systemd-bus-proxy\",\n" +
                "            \"UID\": \"999\",\n" +
                "            \"SHELL\": \"/sbin/nologin\",\n" +
                "            \"GID\": \"998\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"NAME\": \"polkitd\",\n" +
                "            \"HOME\": \"/\",\n" +
                "            \"GID\": \"997\",\n" +
                "            \"UID\": \"998\",\n" +
                "            \"SHELL\": \"/sbin/nologin\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"NAME\": \"chrony\",\n" +
                "            \"HOME\": \"/var/lib/chrony\",\n" +
                "            \"UID\": \"997\",\n" +
                "            \"SHELL\": \"/sbin/nologin\",\n" +
                "            \"GID\": \"995\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"HOME\": \"/var/lib/nfs\",\n" +
                "            \"NAME\": \"nfsnobody\",\n" +
                "            \"GID\": \"65534\",\n" +
                "            \"SHELL\": \"/sbin/nologin\",\n" +
                "            \"UID\": \"65534\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"GID\": \"500\",\n" +
                "            \"UID\": \"500\",\n" +
                "            \"SHELL\": \"/bin/bash\",\n" +
                "            \"HOME\": \"/home/app\",\n" +
                "            \"NAME\": \"app\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"GID\": \"1000\",\n" +
                "            \"UID\": \"1000\",\n" +
                "            \"SHELL\": \"/sbin/nologin\",\n" +
                "            \"HOME\": \"/home/nginx\",\n" +
                "            \"NAME\": \"nginx\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"HOME\": \"/home/postgres\",\n" +
                "            \"NAME\": \"postgres\",\n" +
                "            \"GID\": \"1001\",\n" +
                "            \"SHELL\": \"/bin/bash\",\n" +
                "            \"UID\": \"1001\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"NAME\": \"deploydemo\",\n" +
                "            \"HOME\": \"/home/deploydemo\",\n" +
                "            \"GID\": \"1002\",\n" +
                "            \"SHELL\": \"/bin/bash\",\n" +
                "            \"UID\": \"1002\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"GID\": \"1003\",\n" +
                "            \"SHELL\": \"/bin/bash\",\n" +
                "            \"UID\": \"1003\",\n" +
                "            \"HOME\": \"/home/wenhb\",\n" +
                "            \"NAME\": \"wenhb\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"MGMT_PORT\": 3939,\n" +
                "    \"MEM_FREE\": 1734.059,\n" +
                "    \"VM_ID\": \"vm-56\",\n" +
                "    \"STATE\": \"poweredOn\",\n" +
                "    \"NIC_BOND\": 0,\n" +
                "    \"OBJECT_TYPE\": \"OS\",\n" +
                "    \"NFS_MOUNTED\": 0,\n" +
                "    \"CPU_COUNT\": 4,\n" +
                "    \"NTP_ENABLE\": 1,\n" +
                "    \"DNS_SERVERS\": [\n" +
                "        {\n" +
                "            \"NAME\": \"192.168.1.188\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"NETWORKMANAGER_ENABLE\": 1,\n" +
                "    \"OS_ID\": \"111\",\n" +
                "    \"MACHINE_SN\": \"DJFFF3X\",\n" +
                "    \"MEM_CACHED\": 4549.301,\n" +
                "    \"OPENSSL_VERSION\": \"1.0.1e-fips\",\n" +
                "    \"MEM_UNIT\": \"MB\",\n" +
                "    \"SSH_VERSION\": \"OpenSSH_6.6.1p1\",\n" +
                "    \"SWAP_TOTAL\": 2047.996,\n" +
                "    \"CPU_ARCH\": \"x86_64\",\n" +
                "    \"PRODUCT_UUID\": \"42213814-A3BC-2393-7625-FED86156C6C2\",\n" +
                "    \"SELINUX_STATUS\": \"permissive\",\n" +
                "    \"NET_INTERFACES\": [\n" +
                "        {\n" +
                "            \"NAME\": \"ens160\",\n" +
                "            \"UNIT\": \"Mb/s\",\n" +
                "            \"SPEED\": 10000,\n" +
                "            \"LINK_STATE\": \"up\",\n" +
                "            \"MAC\": \"00:50:56:a1:eb:d2\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"HOSTNAME\": \"centos7base\",\n" +
                "    \"VERSION\": \"CentOS Linux release 7.3.1611 (Core) \",\n" +
                "    \"MOUNT_POINTS\": [\n" +
                "        {\n" +
                "            \"FS_TYPE\": \"xfs\",\n" +
                "            \"MOUNT_POINT\": \"/\",\n" +
                "            \"CAPACITY\": 146.979,\n" +
                "            \"USED\": 114.278,\n" +
                "            \"USED%\": 78,\n" +
                "            \"UNIT\": \"GB\",\n" +
                "            \"DEVICE\": \"/dev/mapper/cl-root\",\n" +
                "            \"AVAILABLE\": 32.701\n" +
                "        },\n" +
                "        {\n" +
                "            \"USED\": 0.136,\n" +
                "            \"USED%\": 14,\n" +
                "            \"CAPACITY\": 0.99,\n" +
                "            \"FS_TYPE\": \"xfs\",\n" +
                "            \"MOUNT_POINT\": \"/boot\",\n" +
                "            \"AVAILABLE\": 0.855,\n" +
                "            \"DEVICE\": \"/dev/sdb1\",\n" +
                "            \"UNIT\": \"GB\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"PK\": [\n" +
                "        \"MGMT_IP\"\n" +
                "    ],\n" +
                "    \"KERNEL_VERSION\": \"3.10.0-514.el7.x86_64\",\n" +
                "    \"_updatetime\": 1629302873000,\n" +
                "    \"MEM_TOTAL\": 15886.957\n" +
                "}"
        );
        Set<String> fieldList = new HashSet<>();
        fieldList.add("STATE");
        fieldList.add("NET_INTERFACES.NAME");
        fieldList.add("NET_INTERFACES.MAC");
        fieldList.add("MOUNT_POINTS.USED");
        //fieldList.add("MEM_CACHED");
        //fieldList.add("NET_INTERFACES");
        //System.out.println(j);
        JSONArray dataList = new JSONArray();
        dataList.add(jsonObj);
        System.out.println(flattenJson(dataList, fieldList, null).toString(SerializerFeature.DisableCircularReferenceDetect));
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
