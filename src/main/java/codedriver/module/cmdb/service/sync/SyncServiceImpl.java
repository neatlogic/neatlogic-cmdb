/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.sync;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.sync.SyncConfigVo;
import codedriver.framework.cmdb.dto.sync.SyncMappingVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionGroupVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.sync.CiEntityDuplicateException;
import codedriver.framework.cmdb.exception.sync.UniqueMappingNotFoundException;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class SyncServiceImpl implements SyncService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public void doSync(SyncConfigVo syncConfigVo) {
        int pageSize = 100;
        long currentPage = 1;
        Query query = syncConfigVo.getQuery();
        query.limit(pageSize);
        List<JSONObject> dataList = mongoTemplate.find(query, JSONObject.class, syncConfigVo.getCollectionName());
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
        List<AttrVo> uniqueAttrList = attrMapper.getAttrByIdList(syncConfigVo.getCiVo().getUniqueAttrIdList());
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
                    ciEntityService.saveCiEntity(ciEntityTransactionVo, transactionGroupVo);
                }
            }

            currentPage += 1;
            query.skip(pageSize * (currentPage - 1));
            dataList = mongoTemplate.find(query, JSONObject.class, syncConfigVo.getCollectionName());
        }
    }

    public static void main(String[] argvs) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("a", "abc");
        jsonObj.put("b", "cde");
        System.out.println(JSONPath.read(jsonObj.toJSONString(), "a"));
    }

    private List<String> getValueListFromData(JSONObject dataObj, String jsonpath) {
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
