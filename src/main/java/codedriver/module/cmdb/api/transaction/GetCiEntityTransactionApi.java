package codedriver.module.cmdb.api.transaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntitySnapshotMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;

@Service
public class GetCiEntityTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CiEntitySnapshotMapper ciEntitySnapshotMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientitytransaction/get";
    }

    @Override
    public String getName() {
        return "获取配置项事务详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "transactionId", type = ApiParamType.LONG, desc = "事务id"),
        @Param(name = "transactionIdList", type = ApiParamType.JSONARRAY, desc = "事务id列表")})
    @Output({@Param(explode = CiEntityTransactionVo.class)})
    @Description(desc = "获取配置项事务详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("transactionId");
        JSONArray transactionIdList = jsonObj.getJSONArray("transactionIdList");
        if (transactionId == null && CollectionUtils.isEmpty(transactionIdList)) {
            throw new ApiRuntimeException("请提供参数transactionId或transactionIdList");
        }
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();

        if (transactionId != null) {
            CiEntityTransactionVo ciEntityTransactionVo =
                transactionMapper.getCiEntityTransactionByTransactionId(transactionId);
            if (ciEntityTransactionVo != null) {
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
        } else if (CollectionUtils.isNotEmpty(transactionIdList)) {
            List<Long> tidList = new ArrayList<>();
            for (int i = 0; i < transactionIdList.size(); i++) {
                tidList.add(transactionIdList.getLong(i));
            }
            if (CollectionUtils.isNotEmpty(tidList)) {
                ciEntityTransactionList.addAll(transactionMapper.getCiEntityTransactionByTransactionIdList(tidList));
            }
        }
        JSONArray dataList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
            for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
                if (ciEntityTransactionVo != null) {
                    List<AttrEntityTransactionVo> attrEntityTransactionList =
                        transactionMapper.getAttrEntityTransactionByTransactionIdAndCiEntityId(transactionId,
                            ciEntityTransactionVo.getCiEntityId());
                    List<RelEntityTransactionVo> relEntityTransactionList =
                        transactionMapper.getRelEntityTransactionByTransactionIdAndCiEntityId(transactionId,
                            ciEntityTransactionVo.getCiEntityId());
                    if (CollectionUtils.isNotEmpty(attrEntityTransactionList)
                        || CollectionUtils.isNotEmpty(relEntityTransactionList)) {

                        String snapshot =
                            ciEntitySnapshotMapper.getSnapshotContentByHash(ciEntityTransactionVo.getSnapshotHash());
                        JSONObject oldAttrEntityData = null;
                        JSONObject oldRelEntityData = null;
                        if (StringUtils.isNotBlank(snapshot)) {
                            JSONObject oldCiEntityObj = JSONObject.parseObject(snapshot);
                            oldAttrEntityData = oldCiEntityObj.getJSONObject("attrEntityData");
                            oldRelEntityData = oldCiEntityObj.getJSONObject("relEntityData");
                        }
                        for (AttrEntityTransactionVo attrEntityTransactionVo : attrEntityTransactionList) {
                            JSONObject dataObj = new JSONObject();
                            dataObj.put("id", "attr_" + attrEntityTransactionVo.getAttrId());
                            dataObj.put("label", attrEntityTransactionVo.getAttrLabel());
                            dataObj.put("type", "attr");
                            dataObj.put("saveMode", attrEntityTransactionVo.getSaveMode());
                            dataObj.put("propId", attrEntityTransactionVo.getPropId());
                            dataObj.put("propHandler", attrEntityTransactionVo.getPropHandler());
                            dataObj.put("newValueList", attrEntityTransactionVo.getValueList());
                            if (oldAttrEntityData != null
                                && oldAttrEntityData.containsKey("attr_" + attrEntityTransactionVo.getAttrId())) {
                                dataObj.put("oldValueList",
                                    oldAttrEntityData.getJSONObject("attr_" + attrEntityTransactionVo.getAttrId())
                                        .getJSONArray("valueList"));
                            }
                            dataList.add(dataObj);
                        }
                        if (CollectionUtils.isNotEmpty(relEntityTransactionList)) {
                            Map<String, List<RelEntityTransactionVo>> relGroupMap = relEntityTransactionList.stream()
                                .collect(Collectors.groupingBy(e -> getRelGroupKey(e)));
                            Iterator<String> keyit = relGroupMap.keySet().iterator();
                            while (keyit.hasNext()) {
                                String key = keyit.next();
                                String[] keys = key.split("#");
                                Long relId = Long.parseLong(keys[0]);
                                String direction = keys[1];
                                String endName = keys[2];
                                String relName = keys[3];
                                List<RelEntityTransactionVo> relList = relGroupMap.get(key);
                                JSONArray newValueList = new JSONArray();
                                for (RelEntityTransactionVo relEntity : relList) {
                                    JSONObject vObj = new JSONObject();
                                    if (direction.equals(RelDirectionType.FROM.getValue())) {
                                        vObj.put("ciEntityName", relEntity.getToCiEntityName());
                                        vObj.put("ciEntityId", relEntity.getToCiEntityId());
                                        vObj.put("ciId", relEntity.getToCiId());
                                        vObj.put("action", relEntity.getAction());
                                        vObj.put("actionText", relEntity.getActionText());
                                        newValueList.add(vObj);
                                    } else if (direction.equals(RelDirectionType.TO.getValue())) {
                                        vObj.put("ciEntityName", relEntity.getFromCiEntityName());
                                        vObj.put("ciEntityId", relEntity.getFromCiEntityId());
                                        vObj.put("ciId", relEntity.getFromCiId());
                                        vObj.put("action", relEntity.getAction());
                                        vObj.put("actionText", relEntity.getActionText());
                                        newValueList.add(vObj);
                                    }
                                }

                                JSONObject dataObj = new JSONObject();
                                dataObj.put("id", "rel" + direction + "_" + relId);
                                dataObj.put("relName", relName);
                                dataObj.put("endName", endName);
                                dataObj.put("type", "rel");
                                dataObj.put("newValueList", newValueList);
                                if (oldRelEntityData != null
                                    && oldRelEntityData.containsKey("rel" + direction + "_" + relId)) {
                                    dataObj.put("oldValueList",
                                        oldRelEntityData.getJSONArray("rel" + direction + "_" + relId));
                                }
                                dataList.add(dataObj);
                            }
                        }
                    }
                }
            }
        }
        return dataList;
    }

    private static String getRelGroupKey(RelEntityTransactionVo relEntityTransactionVo) {
        if (relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
            return relEntityTransactionVo.getRelId() + "#" + relEntityTransactionVo.getDirection() + "#"
                + relEntityTransactionVo.getToLabel() + "#" + relEntityTransactionVo.getTypeId();
        } else {
            return relEntityTransactionVo.getRelId() + "#" + relEntityTransactionVo.getDirection() + "#"
                + relEntityTransactionVo.getFromLabel() + "#" + relEntityTransactionVo.getTypeId();
        }
    }
}
