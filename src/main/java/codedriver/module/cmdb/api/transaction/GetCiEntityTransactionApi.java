package codedriver.module.cmdb.api.transaction;

import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntitySnapshotMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private CiEntitySnapshotMapper ciEntitySnapshotMapper;

    @Autowired
    private RelMapper relMapper;

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

    @Input({@Param(name = "ciEntityId", isRequired = true, type = ApiParamType.LONG, desc = "配置项id"),
            @Param(name = "transactionId", type = ApiParamType.LONG, desc = "事务id"),
            @Param(name = "transactionIdList", type = ApiParamType.JSONARRAY, desc = "事务id列表")})
    @Output({@Param(explode = CiEntityTransactionVo.class)})
    @Description(desc = "获取配置项事务详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long transactionId = jsonObj.getLong("transactionId");
        JSONArray transactionIdList = jsonObj.getJSONArray("transactionIdList");
        if (transactionId == null && CollectionUtils.isEmpty(transactionIdList)) {
            throw new ApiRuntimeException("请提供参数transactionId或transactionIdList");
        }
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();

        if (transactionId != null) {
            CiEntityTransactionVo ciEntityTransactionVo =
                    transactionMapper.getCiEntityTransactionByTransactionIdAndCiEntityId(transactionId, ciEntityId);
            if (ciEntityTransactionVo != null) {
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
        } else if (CollectionUtils.isNotEmpty(transactionIdList)) {
            List<Long> tidList = new ArrayList<>();
            for (int i = 0; i < transactionIdList.size(); i++) {
                tidList.add(transactionIdList.getLong(i));
            }
            if (CollectionUtils.isNotEmpty(tidList)) {
                ciEntityTransactionList.addAll(transactionMapper.getCiEntityTransactionByTransactionIdList(tidList, ciEntityId));
            }
        }
        JSONArray dataList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
            for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
                if (ciEntityTransactionVo != null) {
                    JSONObject oldAttrEntityData = null;
                    JSONObject oldRelEntityData = null;
                    if (StringUtils.isNotBlank(ciEntityTransactionVo.getSnapshot())) {
                        JSONObject oldCiEntityObj = JSONObject.parseObject(ciEntityTransactionVo.getSnapshot());
                        oldAttrEntityData = oldCiEntityObj.getJSONObject("attrEntityData");
                        oldRelEntityData = oldCiEntityObj.getJSONObject("relEntityData");
                    }
                    for (AttrEntityTransactionVo attrEntityTransactionVo : ciEntityTransactionVo.getAttrEntityTransactionList()) {
                        JSONObject dataObj = new JSONObject();
                        dataObj.put("id", "attr_" + attrEntityTransactionVo.getAttrId());
                        dataObj.put("label", attrEntityTransactionVo.getAttrLabel());
                        dataObj.put("type", "attr");
                        dataObj.put("saveMode", attrEntityTransactionVo.getSaveMode());
                        dataObj.put("newValueList", attrEntityTransactionVo.getValueList());
                        if (oldAttrEntityData != null
                                && oldAttrEntityData.containsKey("attr_" + attrEntityTransactionVo.getAttrId())) {
                            dataObj.put("oldValueList",
                                    oldAttrEntityData.getJSONObject("attr_" + attrEntityTransactionVo.getAttrId())
                                            .getJSONArray("valueList"));
                        }
                        dataList.add(dataObj);
                    }
                    if (CollectionUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityTransactionList())) {
                        Map<String, List<RelEntityTransactionVo>> relGroupMap = ciEntityTransactionVo.getRelEntityTransactionList().stream()
                                .collect(Collectors.groupingBy(GetCiEntityTransactionApi::getRelGroupKey));
                        for (String key : relGroupMap.keySet()) {
                            String[] keys = key.split("#");
                            long relId = Long.parseLong(keys[0].trim());
                            String direction = keys[1].trim();
                            String endName = keys[2].trim();
                            String relName = keys[3].trim();
                            List<RelEntityTransactionVo> relList = relGroupMap.get(key);
                            JSONArray newValueList = new JSONArray();
                            JSONObject dataObj = new JSONObject();

                            for (RelEntityTransactionVo relEntity : relList) {
                                JSONObject vObj = new JSONObject();
                                if (direction.equals(RelDirectionType.FROM.getValue())) {
                                    vObj.put("ciEntityName", relEntity.getToCiEntityName());
                                    vObj.put("ciEntityId", relEntity.getToCiEntityId());
                                    vObj.put("ciId", relEntity.getToCiId());
                                    vObj.put("action", relEntity.getAction());
                                    vObj.put("actionText", relEntity.getActionText());
                                    newValueList.add(vObj);
                                        /*由于对端模型id是从关系表中获取，所以如果关系删除，则模型id也会删除，所以需要先看relId是否存在，
                                        如果relId不存在，是关系删除的场景，如果relId存在且ciId不存在，才是模型删除的场景
                                         */
                                    RelVo relVo = relMapper.getRelBaseInfoById(relEntity.getRelId());
                                    if (relVo == null) {
                                        dataObj.put("relDeleted", true);
                                    } else if (relEntity.getToCiId() == null) {
                                        dataObj.put("ciDeleted", true);
                                    } else if (relEntity.getToCiEntityId() == null) {
                                        dataObj.put("ciEntityDeleted", true);
                                    }
                                } else if (direction.equals(RelDirectionType.TO.getValue())) {
                                    vObj.put("ciEntityName", relEntity.getFromCiEntityName());
                                    vObj.put("ciEntityId", relEntity.getFromCiEntityId());
                                    vObj.put("ciId", relEntity.getFromCiId());
                                    vObj.put("action", relEntity.getAction());
                                    vObj.put("actionText", relEntity.getActionText());
                                    newValueList.add(vObj);
                                    RelVo relVo = relMapper.getRelBaseInfoById(relEntity.getRelId());
                                    if (relVo == null) {
                                        dataObj.put("relDeleted", true);
                                    } else if (relEntity.getFromCiId() == null) {
                                        dataObj.put("ciDeleted", true);
                                    } else if (relEntity.getFromCiEntityId() == null) {
                                        dataObj.put("ciEntityDeleted", true);
                                    }
                                }
                            }

                            dataObj.put("id", "rel" + direction + "_" + relId);
                            dataObj.put("relName", relName);
                            dataObj.put("endName", endName);
                            dataObj.put("type", "rel");
                            dataObj.put("newValueList", newValueList);
                            if (oldRelEntityData != null
                                    && oldRelEntityData.containsKey("rel" + direction + "_" + relId)) {
                                dataObj.put("oldValueList",
                                        oldRelEntityData.getJSONObject("rel" + direction + "_" + relId).getJSONArray("valueList"));
                            }
                            dataList.add(dataObj);
                        }
                    }
                }
            }
        }
        return dataList;
    }

    private static String getRelGroupKey(RelEntityTransactionVo relEntityTransactionVo) {
        //如果值为空则使用空格作占位符
        if (relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
            return (relEntityTransactionVo.getRelId() != null ? relEntityTransactionVo.getRelId() : " ") + "#" + (relEntityTransactionVo.getDirection() != null ? relEntityTransactionVo.getDirection() : " ") + "#"
                    + (relEntityTransactionVo.getToLabel() != null ? relEntityTransactionVo.getToLabel() : " ") + "#" + (relEntityTransactionVo.getTypeId() != null ? relEntityTransactionVo.getTypeId() : " ");
        } else {
            return (relEntityTransactionVo.getRelId() != null ? relEntityTransactionVo.getRelId() : " ") + "#" + (relEntityTransactionVo.getDirection() != null ? relEntityTransactionVo.getDirection() : " ") + "#"
                    + (relEntityTransactionVo.getFromLabel() != null ? relEntityTransactionVo.getFromLabel() : " ") + "#" + (relEntityTransactionVo.getTypeId() != null ? relEntityTransactionVo.getTypeId() : " ");
        }
    }
}
