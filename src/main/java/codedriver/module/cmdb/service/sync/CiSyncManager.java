/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.sync;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.sync.SyncAuditVo;
import codedriver.framework.cmdb.dto.sync.SyncConfigVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.sync.CiEntityDuplicateException;
import codedriver.framework.cmdb.exception.sync.CiSyncIsDoingException;
import codedriver.framework.cmdb.exception.sync.UniqueMappingNotFoundException;
import codedriver.framework.asynchronization.threadlocal.InputFromContext;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.sync.SyncAuditMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CiSyncManager {
    private static MongoTemplate mongoTemplate;
    private static CiEntityService ciEntityService;
    private static AttrMapper attrMapper;
    private static RelMapper relMapper;
    private static SyncAuditMapper syncAuditMapper;

    public CiSyncManager(MongoTemplate _mongoTemplate, CiEntityService _ciEntityService, AttrMapper _attrMapper, SyncAuditMapper _syncAuditMapper, RelMapper _relMapper) {
        mongoTemplate = _mongoTemplate;
        ciEntityService = _ciEntityService;
        attrMapper = _attrMapper;
        relMapper = _relMapper;
        syncAuditMapper = _syncAuditMapper;
    }

    public static class SyncHandler extends CodeDriverThread {
        private final SyncConfigVo syncConfigVo;
        private final TransactionGroupVo transactionGroupVo;

        public SyncHandler(SyncConfigVo syncConfigVo, TransactionGroupVo transactionGroupVo) {
            super.setThreadName("CI-SYNC-" + syncConfigVo.getId());
            this.syncConfigVo = syncConfigVo;
            this.transactionGroupVo = transactionGroupVo;
        }

        @Override
        protected void execute() {
            int pageSize = 100;
            long currentPage = 1;
            Query query = syncConfigVo.getQuery();
            query.limit(pageSize);
            List<JSONObject> dataList = mongoTemplate.find(query, JSONObject.class, syncConfigVo.getCollectionName());
            List<AttrVo> attrList = attrMapper.getAttrByCiId(syncConfigVo.getCiId());
            List<RelVo> relList = relMapper.getRelByCiId(syncConfigVo.getCiId());
            while (CollectionUtils.isNotEmpty(dataList)) {
                for (JSONObject dataObj : dataList) {
                    CiEntityVo ciEntityConditionVo = new CiEntityVo();
                    for (Long attrId : syncConfigVo.getCiVo().getUniqueAttrIdList()) {
                        SyncMappingVo mappingVo = syncConfigVo.getMappingByAttrId(attrId);
                        if (mappingVo != null) {
                            AttrFilterVo filterVo = new AttrFilterVo();
                            filterVo.setAttrId(attrId);
                            filterVo.setExpression(SearchExpression.EQ.getExpression());
                            filterVo.setValueList(getValueListFromData(dataObj, mappingVo.getField()));
                            ciEntityConditionVo.addAttrFilter(filterVo);
                        } else {
                            throw new UniqueMappingNotFoundException(attrId);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                        List<CiEntityVo> checkList = ciEntityService.searchCiEntity(ciEntityConditionVo);
                        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                        if (CollectionUtils.isEmpty(checkList)) {
                            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
                        } else if (checkList.size() == 1) {
                            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                            ciEntityTransactionVo.setCiEntityId(checkList.get(0).getId());
                        } else {
                            throw new CiEntityDuplicateException();
                        }
                        for (SyncMappingVo mappingVo : syncConfigVo.getMappingList()) {
                            //ciEntityTransactionVo.addAttrEntityData(mappingVo.getAttrId(), attr, valueList);
                        }

                        ciEntityService.saveCiEntity(ciEntityTransactionVo, transactionGroupVo);
                    }
                }

                currentPage += 1;
                query.skip(pageSize * (currentPage - 1));
                dataList = mongoTemplate.find(query, JSONObject.class, syncConfigVo.getCollectionName());
            }
        }
    }


    public static void doSync(SyncConfigVo syncConfigVo) {
        List<SyncAuditVo> auditList = syncAuditMapper.getDoingSyncByCiId(syncConfigVo.getCiId());
        if (CollectionUtils.isNotEmpty(auditList)) {
            throw new CiSyncIsDoingException(syncConfigVo.getCiVo());
        }
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        SyncAuditVo syncAuditVo = new SyncAuditVo();
        syncAuditVo.setSyncConfigId(syncConfigVo.getId());
        syncAuditVo.setTransactionGroupId(transactionGroupVo.getId());
        syncAuditVo.setExecType(InputFromContext.get().getInputFrom());
        syncAuditMapper.insertSyncAudit(syncAuditVo);
        SyncHandler handler = new SyncHandler(syncConfigVo, transactionGroupVo);
        CachedThreadPool.execute(handler);
    }

    public static void main(String[] argvs) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("a", "abc");
        jsonObj.put("b", "cde");
        System.out.println(JSONPath.read(jsonObj.toJSONString(), "a"));
    }

    private static List<String> getValueListFromData(JSONObject dataObj, String jsonpath) {
        Object v = JSONPath.read(dataObj.toJSONString(), jsonpath);
        if (v != null) {
            List<String> valueList = new ArrayList<>();
            valueList.add(v.toString());
            return valueList;
        }
        return null;
    }
/*
    private int checkCiEntityIsExists(List<AttrFilterVo> attrFilterList) {
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
    }*/


}
