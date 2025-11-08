/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.synccientity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.ADMIN;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.crossover.*;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.AttrEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.enums.*;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import neatlogic.framework.cmdb.exception.attrtype.AttrTypeNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiUniqueAttrNotFoundException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrNotFoundException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrValueIrregularException;
import neatlogic.framework.cmdb.exception.rel.RelNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.integration.IntegrationHandlerNotFoundException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.integration.core.IIntegrationHandler;
import neatlogic.framework.integration.core.IntegrationHandlerFactory;
import neatlogic.framework.integration.dao.mapper.IntegrationMapper;
import neatlogic.framework.integration.dto.IntegrationResultVo;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.framework.matrix.exception.MatrixExternalAccessException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.framework.integration.handler.FrameworkRequestFrom;
import neatlogic.module.framework.scheduler.IntegrationJob;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = ADMIN.class)
@AuthUser(SystemUser.SYSTEM)
@OperationType(type = OperationTypeEnum.OPERATE)
//@Transactional
public class SyncCiEntityDataForBalantFlowApi extends PrivateApiComponentBase {

    private final static Logger logger = LoggerFactory.getLogger(IntegrationJob.class);

    @Resource
    private IntegrationMapper integrationMapper;
    @Resource
    private CiEntityService ciEntityService;
    @Resource
    private CiMapper ciMapper;
    @Resource
    private AttrMapper attrMapper;
    @Resource
    private GlobalAttrMapper globalAttrMapper;
    @Resource
    private RelMapper relMapper;

    @Override
    public String getName() {
        return "同步CMDB配置项数据";
    }

    @Input({
            @Param(name = "syncCiList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "同步模型列表"),
            @Param(name = "integrationName", type = ApiParamType.STRING, isRequired = true, desc = "集成名称"),
            @Param(name = "integrationParam", type = ApiParamType.STRING, desc = "集成输入参数"),
            @Param(name = "listeningNameList", type = ApiParamType.JSONARRAY, desc = "监听列表", help = "配置项名称"),
            @Param(name = "integrationRequestMaxCount", type = ApiParamType.INTEGER, defaultValue = "10000", desc = "集成请求最大次数，默认是10000"),

    })
    @Output({

    })
    @Description(desc = "同步CMDB配置项数据，获取数据接口是balantflow/restservices/cmdb/glasnostViewQueryRestComponentApi?currentPage=1&tPageSize=20&id=65")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        List<String> listeningNameList = new ArrayList<>();
        JSONArray listeningNameArray = paramObj.getJSONArray("listeningNameList");
        if (CollectionUtils.isNotEmpty(listeningNameArray)) {
            listeningNameList = listeningNameArray.toJavaList(String.class);
        }
        JSONArray syncCiList = paramObj.getJSONArray("syncCiList");
        List<CiMappingVo> ciMappingList = checkSyncCiList(syncCiList);
        resultObj.put("ciMappingList", ciMappingList);
        String integrationName = paramObj.getString("integrationName");
        if (integrationName != null) {
            IntegrationVo supplierIntegrationVo = integrationMapper.getIntegrationByName(integrationName);
            if (supplierIntegrationVo != null) {
                Map<String, Long> label2CiIdMap = new HashMap<>();
                IIntegrationHandler handler = IntegrationHandlerFactory.getHandler(supplierIntegrationVo.getHandler());
                if (handler == null) {
                    throw new IntegrationHandlerNotFoundException(supplierIntegrationVo.getHandler());
                }
                JSONArray insertList = new JSONArray();
                JSONArray updateList = new JSONArray();
                JSONArray listeningList = new JSONArray();
                JSONObject integrationParam = paramObj.getJSONObject("integrationParam");
                if (MapUtils.isNotEmpty(integrationParam)) {
                    supplierIntegrationVo.getParamObj().putAll(integrationParam);
                }
                int integrationRequestMaxCount = paramObj.getInteger("integrationRequestMaxCount");
                for (int currentPage = 1; currentPage <= integrationRequestMaxCount; currentPage++) {
                    supplierIntegrationVo.getParamObj().put("currentPage", currentPage);
                    IntegrationResultVo resultVo = handler.sendRequest(supplierIntegrationVo, FrameworkRequestFrom.API);
                    if (StringUtils.isNotBlank(resultVo.getError())) {
                        logger.error(resultVo.getError());
                        throw new MatrixExternalAccessException(supplierIntegrationVo.getName());
                    }
                    handler.validate(resultVo);
                    JSONObject dataObj = JSONObject.parseObject(resultVo.getTransformedResult());
                    if (MapUtils.isNotEmpty(dataObj)) {
                        JSONObject returnObj = dataObj.getJSONObject("Return");
                        if (MapUtils.isNotEmpty(returnObj)) {
                            Long id = returnObj.getLong("id");
                            String name = returnObj.getString("name");
                            JSONArray columnList = returnObj.getJSONArray("columnList");
                            if (MapUtils.isEmpty(label2CiIdMap)) {
                                if (CollectionUtils.isNotEmpty(columnList)) {
                                    for (int i = 0; i < columnList.size(); i++) {
                                        JSONObject columnObj = columnList.getJSONObject(i);
                                        if (MapUtils.isNotEmpty(columnObj)) {
                                            Long ciId = columnObj.getLong("ciId");
                                            Long attrId = columnObj.getLong("attrId");
                                            String label = columnObj.getString("label");
                                            label2CiIdMap.put(label, ciId);
                                        }
                                    }
                                }
                            }
                            JSONArray resultList = returnObj.getJSONArray("resultList");
                            if (CollectionUtils.isNotEmpty(resultList)) {
                                List<Long> transactionGroupIdList = savePageCiEntityList(label2CiIdMap, resultList, ciMappingList, listeningNameList, insertList, updateList, listeningList);
                                resultObj.put("transactionGroupIdList", transactionGroupIdList);
                            }
                        } else {
                            break;
                        }
                    }
                }
                resultObj.put("insertList", insertList);
                resultObj.put("updateList", updateList);
                if (CollectionUtils.isNotEmpty(listeningList)) {
                    resultObj.put("listeningList", listeningList);
                }
            }
        }
        return resultObj;
    }
    private List<Long> savePageCiEntityList(
            Map<String, Long> label2CiIdMap,
            JSONArray resultList,
            List<CiMappingVo> ciMappingList,
            List<String> listeningNameList,
            JSONArray insertList,
            JSONArray updateList,
            JSONArray listeningList
    ) {
        List<Long> transactionGroupIdList = new ArrayList<>();
        for (int i = 0; i < resultList.size(); i++) {
            JSONObject row = resultList.getJSONObject(i);
            Long ciEntityId = row.getLong("ciEntityId");
            JSONArray attrList = row.getJSONArray("attrList");
            if (CollectionUtils.isNotEmpty(attrList)) {
                for (int j = 0; j < attrList.size(); j++) {
                    JSONObject attrObj = attrList.getJSONObject(i);
                    if (MapUtils.isNotEmpty(attrObj)) {
                        Long attrId = attrObj.getLong("attrId");
                        String label = attrObj.getString("label");
                        Object value = attrObj.get("value");
                        Long ciId = label2CiIdMap.get(label);
                        attrObj.put("ciId", ciId);
                    }
                }
                for (CiMappingVo ciMappingVo : ciMappingList) {
                    JSONObject positioningError = new JSONObject();
                    List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
                    CiEntityTransactionVo ciEntityTransactionVo = generateCiEntityTransactionVo(attrList, ciMappingVo, ciEntityTransactionList, positioningError);
                    Long transactionGroupId = saveRowCiEntityList(ciEntityTransactionList, listeningNameList, insertList, updateList, listeningList);
                    transactionGroupIdList.add(transactionGroupId);
                }
            }
        }
        return transactionGroupIdList;
    }

    private CiEntityTransactionVo generateCiEntityTransactionVo(JSONArray attrList, CiMappingVo ciMappingVo, List<CiEntityTransactionVo> ciEntityTransactionList, JSONObject positioningError) {
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        ciEntityTransactionVo.setCiId(ciMappingVo.getCiId());
        ciEntityTransactionVo.setAllowCommit(true);
        ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
        ciEntityTransactionVo.setDescription(ciMappingVo.getDescription());
        /* 属性 */
        JSONObject attrEntityData = buildAttrEntityData(attrList, ciMappingVo.getAttrList(), ciMappingVo, ciEntityTransactionList, positioningError);
        ciEntityTransactionVo.setAttrEntityData(attrEntityData);
        Long ciEntityId = getCiEntityIdByUniqueAttrIdList(ciMappingVo, ciEntityTransactionVo, positioningError);
        if (ciEntityId != null) {
            ciEntityTransactionVo.setCiEntityId(ciEntityId);
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        } else {
            ciEntityTransactionVo.getCiEntityId();
            ciEntityTransactionVo.setCiEntityUuid(UuidUtil.randomUuid());
            JSONObject nameAttrEntityData = ciEntityTransactionVo.getAttrEntityDataByAttrId(ciMappingVo.getNameAttrId());
            if (MapUtils.isNotEmpty(nameAttrEntityData)) {
                JSONArray valueArray = nameAttrEntityData.getJSONArray("valueList");
                if (CollectionUtils.isNotEmpty(valueArray)) {
                    List<String> valueList = valueArray.toJavaList(String.class);
                    ciEntityTransactionVo.setName(String.join(",", valueList));
                }
            }
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        }
        /* 关系 */
        if (CollectionUtils.isNotEmpty(ciMappingVo.getRelList())) {
            JSONObject relEntityData = buildRelEntityData(attrList, ciMappingVo.getRelList(), ciEntityTransactionVo, ciEntityTransactionList, positioningError);
            ciEntityTransactionVo.setRelEntityData(relEntityData);
        }
        /* 全局属性 */
        if (CollectionUtils.isNotEmpty(ciMappingVo.getGlobalAttrList())) {
            JSONObject globalAttrEntityData = buildGlobalAttrEntityData(attrList, ciMappingVo.getGlobalAttrList(), ciMappingVo, positioningError);
            ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrEntityData);
        }
        ciEntityTransactionList.add(ciEntityTransactionVo);
        return ciEntityTransactionVo;
    }

    /**
     * 组装关系
     * @param attrList
     * @param relMappingList
     * @param upperCiEntityTransactionVo
     * @param ciEntityTransactionList
     * @param positioningError
     * @return
     */
    private JSONObject buildRelEntityData(
            JSONArray attrList,
            List<RelMappingVo> relMappingList,
            CiEntityTransactionVo upperCiEntityTransactionVo,
            List<CiEntityTransactionVo> ciEntityTransactionList,
            JSONObject positioningError
    ) {
        JSONObject relEntityData = new JSONObject();
        for (RelMappingVo relMappingVo : relMappingList) {
            CiMappingVo valueMapping = relMappingVo.getValueMapping();
            CiEntityTransactionVo ciEntityTransactionVo = generateCiEntityTransactionVo(attrList, valueMapping, ciEntityTransactionList, positioningError);
            {
                String key = "rel" + relMappingVo.getDirection() + "_" + relMappingVo.getId();
                JSONArray valueList = new JSONArray();
                JSONObject valueObj = new JSONObject();
//                if (Objects.equals(relMappingVo.getDirection(), RelDirectionType.FROM.getValue())) {
//                    if (Objects.equals(relMappingVo.getFromRule(), RelRuleType.N.getValue())) {
//                        valueObj.put("action", RelActionType.INSERT.getValue());
//                    } else {
//                        valueObj.put("action", RelActionType.REPLACE.getValue());
//                    }
//                } else if (Objects.equals(relMappingVo.getDirection(), RelDirectionType.TO.getValue())) {
//                    if (Objects.equals(relMappingVo.getToRule(), RelRuleType.N.getValue())) {
//                        valueObj.put("action", RelActionType.INSERT.getValue());
//                    } else {
//                        valueObj.put("action", RelActionType.REPLACE.getValue());
//                    }
//                }
                if (Objects.equals(ciEntityTransactionVo.getAction(), TransactionActionType.INSERT.getValue())) {
                    valueObj.put("_relId", relMappingVo.getId());
                    valueObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
                    valueObj.put("ciEntityUuid", ciEntityTransactionVo.getCiEntityUuid());
                    valueObj.put("ciEntityName", "新的配置项");
                    valueObj.put("ciId", valueMapping.getCiId());
                    valueObj.put("type", "new");
                } else {
                    valueObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
                    valueObj.put("ciEntityName", ciEntityTransactionVo.getName());
                    valueObj.put("ciId", valueMapping.getCiId());
                }
                valueList.add(valueObj);
                if (CollectionUtils.isNotEmpty(valueList)) {
                    JSONObject relEntity = new JSONObject();
                    relEntity.put("valueList", valueList);
                    relEntityData.put(key, relEntity);
                }
            }
            {
                JSONArray valueList = new JSONArray();
                JSONObject valueObj = new JSONObject();
                String key = "rel";
                if (Objects.equals(relMappingVo.getDirection(), RelDirectionType.FROM.getValue())) {
                    key += RelDirectionType.TO.getValue() + "_" + relMappingVo.getId();
//                    if (Objects.equals(relMappingVo.getToRule(), RelRuleType.N.getValue())) {
//                        valueObj.put("action", RelActionType.INSERT.getValue());
//                    } else {
//                        valueObj.put("action", RelActionType.REPLACE.getValue());
//                    }
                } else if (Objects.equals(relMappingVo.getDirection(), RelDirectionType.TO.getValue())) {
                    key += RelDirectionType.FROM.getValue() + "_" + relMappingVo.getId();
//                    if (Objects.equals(relMappingVo.getFromRule(), RelRuleType.N.getValue())) {
//                        valueObj.put("action", RelActionType.INSERT.getValue());
//                    } else {
//                        valueObj.put("action", RelActionType.REPLACE.getValue());
//                    }
                }
                if (Objects.equals(ciEntityTransactionVo.getAction(), TransactionActionType.INSERT.getValue())) {
                    valueObj.put("_relId", relMappingVo.getId());
                    valueObj.put("ciEntityId", upperCiEntityTransactionVo.getCiEntityId());
                    valueObj.put("ciEntityUuid", upperCiEntityTransactionVo.getCiEntityUuid());
                    valueObj.put("ciEntityName", "来源配置项");
                    valueObj.put("ciId", upperCiEntityTransactionVo.getCiId());
                    valueObj.put("type", "from");
                } else {
                    valueObj.put("ciEntityId", upperCiEntityTransactionVo.getCiEntityId());
                    valueObj.put("ciEntityName", upperCiEntityTransactionVo.getName());
                    valueObj.put("ciId", upperCiEntityTransactionVo.getCiId());
                }
                valueList.add(valueObj);
                JSONObject relEntity = new JSONObject();
                relEntity.put("valueList", valueList);
                JSONObject relEntityData1 = ciEntityTransactionVo.getRelEntityData();
                if (relEntityData1 == null) {
                    relEntityData1 = new JSONObject();
                    ciEntityTransactionVo.setRelEntityData(relEntityData1);
                }
                relEntityData1.put(key, relEntity);
            }
        }
        return relEntityData;
    }

    /**
     * 组装属性
     * @param attrList
     * @param attrMappingList
     * @param ciMappingVo
     * @param ciEntityTransactionList
     * @param positioningError
     * @return
     */
    private JSONObject buildAttrEntityData(
            JSONArray attrList,
            List<AttrMappingVo> attrMappingList,
            CiMappingVo ciMappingVo,
            List<CiEntityTransactionVo> ciEntityTransactionList,
            JSONObject positioningError
    ) {
        JSONObject attrEntityData = new JSONObject();
        for (AttrMappingVo attrMappingVo : attrMappingList) {
            JSONArray valueList = new JSONArray();
            AttrValueMappingVo valueMapping = attrMappingVo.getValueMapping();
            if (valueMapping != null) {
                for (int i = 0; i < attrList.size(); i++) {
                    JSONObject attrObj = attrList.getJSONObject(i);
                    if (MapUtils.isNotEmpty(attrObj)) {
                        Long ciId = attrObj.getLong("ciId");
                        Long attrId = attrObj.getLong("attrId");
                        String label = attrObj.getString("label");
                        if (Objects.equals(valueMapping.getLabel(), label) || Objects.equals(valueMapping.getAttrId(), attrId)) {
                            Object value = attrObj.get("value");
                            if (value instanceof List) {
                                valueList.addAll((List) value);
                            } else {
                                valueList.add(value);
                            }
                        }
                    }
                }
            }
            if (CollectionUtils.isEmpty(valueList)) {
                if (CollectionUtils.isNotEmpty(attrMappingVo.getValueList())) {
                    valueList.addAll(attrMappingVo.getValueList());
                }
            }
            if (CollectionUtils.isNotEmpty(valueList)) {
                String key = "attr_" + attrMappingVo.getId();
                JSONObject attrEntity = new JSONObject();
                attrEntity.put("type", attrMappingVo.getType());
                attrEntity.put("config", attrMappingVo.getConfig());
                if (Objects.equals(attrMappingVo.getType(), PropHandlerType.SELECT.getValue()) || Objects.equals(attrMappingVo.getType(), PropHandlerType.TABLE.getValue())) {
                    IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
                    AttrVo attrVo = attrCrossoverMapper.getAttrById(attrMappingVo.getId());
                    JSONArray newValueList = validSelectAndTableAttrValueList(ciMappingVo.getCiId(), ciMappingVo.getCiName(), ciMappingVo.getCiLabel(), ciEntityTransactionList, attrVo, valueList, positioningError);
                    attrEntity.put("valueList", newValueList);
                } else {
                    attrEntity.put("valueList", valueList);
                }
                attrEntityData.put(key, attrEntity);
            }
        }
        return attrEntityData;
    }

    /**
     * 处理和校验下拉框类型和表格类型属性的值
     *
     * @param attrVo
     * @param valueList
     */
    private JSONArray validSelectAndTableAttrValueList(
            Long ciId,
            String ciName,
            String ciLabel,
            List<CiEntityTransactionVo> ciEntityTransactionList,
            AttrVo attrVo,
            JSONArray valueList,
            JSONObject positioningError
    ) {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        IAttrValueHandler attrHandler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        if (attrHandler == null) {
            positioningError.put("ciId", ciId);
            positioningError.put("ciName", ciName);
            positioningError.put("ciLabel", ciLabel);
            throw new AttrTypeNotFoundException(attrVo.getType());
        }
        if (CollectionUtils.isEmpty(valueList)) {
            return valueList;
        }
        List<Long> longValueList = new ArrayList<>();
        List<String> stringValueList = new ArrayList<>();
        JSONArray tempList = new JSONArray();
        tempList.addAll(valueList);
        for (int i = 0; i < tempList.size(); i++) {
            Object valueObj = tempList.get(i);
            if (valueObj instanceof Long) {
                longValueList.add((Long) valueObj);
            } else if (valueObj instanceof String) {
                String valueStr = valueObj.toString();
                try {
                    Integer.valueOf(valueStr);
                    stringValueList.add(valueStr);
                } catch (NumberFormatException e1) {
                    try {
                        longValueList.add(Long.valueOf(valueStr));
                    } catch (NumberFormatException e2) {
                        stringValueList.add(valueStr);
                    }
                }
            }
        }
        JSONArray newValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(longValueList)) {
            JSONArray array = new JSONArray();
            array.addAll(longValueList);
            attrHandler.valid(attrVo, array);
            newValueList.addAll(longValueList);
        }
        if (CollectionUtils.isNotEmpty(stringValueList)) {
            CiVo ciVo = ciCrossoverMapper.getCiById(attrVo.getTargetCiId());
            if (ciVo == null) {
                positioningError.put("ciId", ciId);
                positioningError.put("ciName", ciName);
                positioningError.put("ciLabel", ciLabel);
                throw new CiNotFoundException(attrVo.getTargetCiId().toString());
            }
            for (String valueStr : stringValueList) {
                CiEntityVo search = new CiEntityVo();
                search.setName(valueStr);
                if (ciVo.getIsVirtual().equals(0)) {
                    // 非虚拟模型
                    List<CiVo> downwardCiList = ciCrossoverMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    Map<Long, CiVo> downwardCiMap = downwardCiList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
                    boolean isFind = false;
                    for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
                        // 如果action为空，说明该ciEntityTransactionVo对象没有在createCiEntityTransactionVo方法中完成初始化，不能调用它的getCiEntityId()方法
                        if (ciEntityTransactionVo.getAction() == null) {
                            continue;
                        }
                        CiVo downwardCi = downwardCiMap.get(ciEntityTransactionVo.getCiId());
                        if (downwardCi == null) {
                            continue;
                        }
                        if (downwardCi.getNameAttrId() == null) {
                            continue;
                        }
                        AttrEntityTransactionVo attrEntityTransaction = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(downwardCi.getNameAttrId());
                        if (attrEntityTransaction == null) {
                            continue;
                        }
                        if (CollectionUtils.isEmpty(attrEntityTransaction.getValueList())) {
                            continue;
                        }
                        for (Object value : attrEntityTransaction.getValueList()) {
                            if (Objects.equals(value, valueStr)) {
                                newValueList.add(ciEntityTransactionVo.getCiEntityId());
                                isFind = true;
                                break;
                            }
                        }
                    }
                    if (!isFind) {
                        search.setIdList(new ArrayList<>(downwardCiMap.keySet()));
                        List<CiEntityVo> ciEntityList = ciEntityCrossoverMapper.getCiEntityListByCiIdListAndName(search);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            attrVo.setCiName(ciName);
                            attrVo.setCiLabel(ciLabel);
                            positioningError.put("ciId", ciId);
                            positioningError.put("ciName", ciName);
                            positioningError.put("ciLabel", ciLabel);
                            throw new AttrValueIrregularException(attrVo, valueStr);
                        }
                        for (CiEntityVo ciEntity : ciEntityList) {
                            newValueList.add(ciEntity.getId());
                        }
                    }
                } else {
                    // 虚拟模型
                    search.setCiId(ciVo.getId());
                    List<CiEntityVo> ciEntityList = ciEntityCrossoverMapper.getVirtualCiEntityBaseInfoByName(search);
                    if (CollectionUtils.isEmpty(ciEntityList)) {
                        attrVo.setCiName(ciName);
                        attrVo.setCiLabel(ciLabel);
                        positioningError.put("ciId", ciId);
                        positioningError.put("ciName", ciName);
                        positioningError.put("ciLabel", ciLabel);
                        throw new AttrValueIrregularException(attrVo, valueStr);
                    }
                    for (CiEntityVo ciEntity : ciEntityList) {
                        newValueList.add(ciEntity.getId());
                    }
                }
            }
        }
        return newValueList;
    }

    /**
     * 根据唯一属性列表查询配置项id
     * @param ciEntityTransactionVo
     * @return
     */
    private Long getCiEntityIdByUniqueAttrIdList(
            CiMappingVo ciMappingVo,
            CiEntityTransactionVo ciEntityTransactionVo,
            JSONObject positioningError
    ) {
//        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
//        CiVo ciVo = ciCrossoverMapper.getCiById(ciMappingVo.getCiId());
        List<Long> uniqueAttrIdList = ciMappingVo.getUniqueAttrIdList();
        if (CollectionUtils.isEmpty(uniqueAttrIdList)) {
            return null;
        }
        //校验唯一规则
        CiEntityVo ciEntityConditionVo = new CiEntityVo();
        ciEntityConditionVo.setCiId(ciMappingVo.getCiId());
        ciEntityConditionVo.setAttrIdList(new ArrayList<Long>() {{
            this.add(0L);
        }});
        ciEntityConditionVo.setRelIdList(new ArrayList<Long>() {{
            this.add(0L);
        }});
        for (Long attrId : uniqueAttrIdList) {
            List<String> valueList = new ArrayList<>();
            AttrEntityTransactionVo attrEntityTransaction = ciEntityTransactionVo.getAttrEntityTransactionByAttrId(attrId);
            if (attrEntityTransaction != null) {
                JSONArray valueArray = attrEntityTransaction.getValueList();
                if (CollectionUtils.isNotEmpty(valueArray)) {
                    valueList = valueArray.toJavaList(String.class);
                }
            }
            if (CollectionUtils.isEmpty(valueList)) {
                positioningError.put("ciId", ciMappingVo.getCiId());
                positioningError.put("ciName", ciMappingVo.getCiName());
                positioningError.put("ciLabel", ciMappingVo.getCiLabel());
                AttrVo attr = attrCrossoverMapper.getAttrById(attrId);
                if (attr == null) {
                    throw new AttrNotFoundException(ciMappingVo.getCiName(), attrId.toString());
                }
                attr.setCiName(ciMappingVo.getCiName());
                attr.setCiLabel(ciMappingVo.getCiLabel());
                throw new CiUniqueAttrNotFoundException(attr);
            }
            AttrFilterVo filterVo = new AttrFilterVo();
            filterVo.setAttrId(attrId);
            filterVo.setExpression(SearchExpression.EQ.getExpression());
            filterVo.setValueList(valueList);
            ciEntityConditionVo.addAttrFilter(filterVo);
        }
        if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
            List<CiEntityVo> checkList = ciEntityCrossoverService.searchCiEntity(ciEntityConditionVo);
            if (CollectionUtils.isNotEmpty(checkList)) {
                CiEntityVo ciEntityVo = checkList.get(0);
                ciEntityTransactionVo.setCiEntityUuid(ciEntityVo.getUuid());
                ciEntityTransactionVo.setName(ciEntityVo.getName());
                return ciEntityVo.getId();
            }
        }
        return null;
    }

    /**
     * 组装全局属性
     * @param attrList
     * @param globalAttrMappingList
     * @param ciMappingVo
     * @param positioningError
     * @return
     */
    private JSONObject buildGlobalAttrEntityData(JSONArray attrList, List<GlobalAttrMappingVo> globalAttrMappingList, CiMappingVo ciMappingVo, JSONObject positioningError) {
        IGlobalAttrCrossoverMapper globalAttrCrossoverMapper = CrossoverServiceFactory.getApi(IGlobalAttrCrossoverMapper.class);
        JSONObject globalAttrEntityData = new JSONObject();
        for (GlobalAttrMappingVo globalAttrMappingVo : globalAttrMappingList) {
            JSONArray valueArray = new JSONArray();
            AttrValueMappingVo valueMapping = globalAttrMappingVo.getValueMapping();
            if (valueMapping != null) {
                for (int i = 0; i < attrList.size(); i++) {
                    JSONObject attrObj = attrList.getJSONObject(i);
                    if (MapUtils.isNotEmpty(attrObj)) {
                        Long ciId = attrObj.getLong("ciId");
                        Long attrId = attrObj.getLong("attrId");
                        String label = attrObj.getString("label");
                        if (Objects.equals(valueMapping.getCiId(), ciId)
                                && (Objects.equals(valueMapping.getLabel(), label) || Objects.equals(valueMapping.getAttrId(), attrId))
                        ) {
                            Object value = attrObj.get("value");
                            if (value instanceof List) {
                                valueArray.addAll((List) value);
                            } else {
                                valueArray.add(value);
                            }
                        }
                    }
                }
            }
            if (CollectionUtils.isEmpty(valueArray)) {
                if (CollectionUtils.isNotEmpty(globalAttrMappingVo.getValueList())) {
                    valueArray.addAll(globalAttrMappingVo.getValueList());
                }
            }
            if (CollectionUtils.isNotEmpty(valueArray)) {
                String key = "global_" + globalAttrMappingVo.getId();
                List<String> valueArrayStrList = new ArrayList<>();
                JSONArray valueList = new JSONArray();
                for (int i = 0; i < valueArray.size(); i++) {
                    Object valueObj = valueArray.get(i);
                    if (valueObj instanceof JSONObject) {
                        // 映射模式为常量
                        valueList.add(JSONObject.parse(JSONObject.toJSONString(valueObj)));
                    } else {
                        valueArrayStrList.add(valueObj.toString());
                        List<GlobalAttrItemVo> itemList = globalAttrCrossoverMapper.getAllGlobalAttrItemByAttrId(globalAttrMappingVo.getId());
                        for (GlobalAttrItemVo item : itemList) {
                            if (Objects.equals(item.getValue(), valueObj)
                                    || Objects.equals(item.getId().toString(), valueObj.toString())) {
                                JSONObject jsonObj = new JSONObject();
                                jsonObj.put("id", item.getId());
                                jsonObj.put("value", item.getValue());
                                jsonObj.put("sort", item.getSort());
                                jsonObj.put("attrId", globalAttrMappingVo.getId());
                                valueList.add(jsonObj);
                            }
                        }
                    }
                }
                if (CollectionUtils.isEmpty(valueList)) {
                    positioningError.put("ciId", ciMappingVo.getCiId());
                    positioningError.put("ciName", ciMappingVo.getCiName());
                    positioningError.put("ciLabel", ciMappingVo.getCiLabel());
                    GlobalAttrVo globalAttrVo = new GlobalAttrVo();
                    globalAttrVo.setId(globalAttrMappingVo.getId());
                    globalAttrVo.setName(globalAttrMappingVo.getName());
                    globalAttrVo.setLabel(globalAttrMappingVo.getLabel());
                    throw new GlobalAttrValueIrregularException(globalAttrVo, String.join(",", valueArrayStrList));
                }
                JSONObject globalAttrEntity = new JSONObject();
                globalAttrEntity.put("valueList", valueList);
                globalAttrEntityData.put(key, globalAttrEntity);
            }
        }
        return globalAttrEntityData;
    }

    private Long saveRowCiEntityList(
            List<CiEntityTransactionVo> ciEntityTransactionList,
            List<String> listeningNameList,
            JSONArray insertList,
            JSONArray updateList,
            JSONArray listeningList
    ) {
        for (CiEntityTransactionVo ciEntityTransaction : ciEntityTransactionList) {
            CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
            ciEntityTransactionVo.setCiEntityUuid(ciEntityTransaction.getCiEntityUuid());
            ciEntityTransactionVo.setName(ciEntityTransaction.getName());
            ciEntityTransactionVo.setCiId(ciEntityTransaction.getCiId());
            ciEntityTransactionVo.setAllowCommit(ciEntityTransaction.isAllowCommit());
            ciEntityTransactionVo.setEditMode(ciEntityTransaction.getEditMode());
            ciEntityTransactionVo.setDescription(ciEntityTransaction.getDescription());
            ciEntityTransactionVo.setCiEntityId(ciEntityTransaction.getCiEntityId());
            ciEntityTransactionVo.setAction(ciEntityTransaction.getAction());
            ciEntityTransactionVo.setAttrEntityData(JSON.parseObject(JSON.toJSONString(ciEntityTransaction.getAttrEntityData())));
            ciEntityTransactionVo.setRelEntityData(JSON.parseObject(JSON.toJSONString(ciEntityTransaction.getRelEntityData())));
            ciEntityTransactionVo.setGlobalAttrEntityData(JSON.parseObject(JSON.toJSONString(ciEntityTransaction.getGlobalAttrEntityData())));
            boolean flag = ciEntityService.validateCiEntityTransaction(ciEntityTransactionVo);
            if (flag) {
                if (Objects.equals(ciEntityTransaction.getAction(), TransactionActionType.INSERT.getValue())) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("ciId", ciEntityTransaction.getCiId());
                    jsonObj.put("ciEntityId", ciEntityTransaction.getCiEntityId());
                    jsonObj.put("ciEntityUuid", ciEntityTransaction.getCiEntityUuid());
                    jsonObj.put("ciEntityName", ciEntityTransaction.getName());
                    insertList.add(jsonObj);
                } else if (Objects.equals(ciEntityTransaction.getAction(), TransactionActionType.UPDATE.getValue())) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("ciId", ciEntityTransaction.getCiId());
                    jsonObj.put("ciEntityId", ciEntityTransaction.getCiEntityId());
                    jsonObj.put("ciEntityUuid", ciEntityTransaction.getCiEntityUuid());
                    jsonObj.put("ciEntityName", ciEntityTransaction.getName());
                    updateList.add(jsonObj);
                }
            } else {
                if (listeningNameList.contains(ciEntityTransaction.getName())) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("ciId", ciEntityTransaction.getCiId());
                    jsonObj.put("ciEntityId", ciEntityTransaction.getCiEntityId());
                    jsonObj.put("ciEntityUuid", ciEntityTransaction.getCiEntityUuid());
                    jsonObj.put("ciEntityName", ciEntityTransaction.getName());
                    JSONObject newData = new JSONObject();
                    newData.put("attrEntityData", JSON.parseObject(JSON.toJSONString(ciEntityTransaction.getAttrEntityData())));
                    newData.put("relEntityData", JSON.parseObject(JSON.toJSONString(ciEntityTransaction.getRelEntityData())));
                    newData.put("globalAttrEntityData", JSON.parseObject(JSON.toJSONString(ciEntityTransaction.getGlobalAttrEntityData())));
                    jsonObj.put("newData", newData);
                    CiEntityVo pCiEntityVo = new CiEntityVo();
                    pCiEntityVo.setId(ciEntityTransaction.getCiEntityId());
                    pCiEntityVo.setCiId(ciEntityTransaction.getCiId());
                    pCiEntityVo.setLimitRelEntity(true);
                    pCiEntityVo.setLimitAttrEntity(true);
                    CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(pCiEntityVo);
                    if (ciEntityVo != null) {
                        JSONObject oldData = new JSONObject();
                        oldData.put("attrEntityData", ciEntityVo.getAttrEntityData());
                        oldData.put("relEntityData", ciEntityVo.getRelEntityData());
                        oldData.put("globalAttrEntityData", ciEntityVo.getGlobalAttrEntityData());
                        jsonObj.put("oldData", oldData);
                    }
                    listeningList.add(jsonObj);
                }
            }
        }
        InputFromContext.init(InputFrom.RESTFUL);
        TransactionGroupVo transactionGroupVo = new TransactionGroupVo();
//        transactionGroupVo.setNeedLock(false);
        ciEntityTransactionList.sort((o1, o2) -> {
            if (o1.getCiId() < o2.getCiId()) {
                return -1;
            } else if (o1.getCiId() > o2.getCiId()) {
                return 1;
            } else {
                return o1.getCiEntityId().compareTo(o2.getCiEntityId());
            }
        });
//        System.out.println("ciEntityTransactionList = " + JSONObject.toJSONString(ciEntityTransactionList));
        Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList, transactionGroupVo);
        return transactionGroupId;
    }

    @Override
    public String getToken() {
        return "cmdb/data/sync";
    }

    private List<CiMappingVo> checkSyncCiList(JSONArray syncCiList) {
        List<CiMappingVo> ciMappingList = new ArrayList<>();
        for (int i = 0; i < syncCiList.size(); i++) {
            JSONObject syncCiObj = syncCiList.getJSONObject(i);
            if (MapUtils.isNotEmpty(syncCiObj)) {
                CiMappingVo ciMappingVo = checkSyncCi(syncCiObj, "syncCiList[" + i + "].");
                ciMappingList.add(ciMappingVo);
            }
        }
        return ciMappingList;
    }

    private CiMappingVo checkSyncCi(JSONObject syncCiObj, String pathPrefix) {
        String ciName = syncCiObj.getString("ciName");
        if (StringUtils.isBlank(ciName)) {
            throw new ParamNotExistsException(pathPrefix + "ciName");
        }
        CiVo ciVo = ciMapper.getCiByName(ciName);
        if (ciVo == null) {
            throw new CiNotFoundException(ciName);
        }
        CiMappingVo ciMappingVo = new CiMappingVo();
        ciMappingVo.setCiId(ciVo.getId());
        ciMappingVo.setCiName(ciVo.getName());
        ciMappingVo.setCiLabel(ciVo.getLabel());
        ciMappingVo.setUniqueAttrIdList(ciVo.getUniqueAttrIdList());
        ciMappingVo.setNameAttrId(ciVo.getNameAttrId());
        String description = syncCiObj.getString("description");
        ciMappingVo.setDescription(description);
        JSONArray attrArray = syncCiObj.getJSONArray("attrList");
        if (CollectionUtils.isNotEmpty(attrArray)) {
            List<AttrMappingVo> attrList = new ArrayList<>();
            Map<Long, AttrVo> id2AttrMap = new HashMap<>();
            Map<String, List<AttrVo>> name2AttrListMap = new HashMap<>();
            List<AttrVo> attrVoList = attrMapper.getAttrByCiId(ciVo.getId());
            for (AttrVo attrVo : attrVoList) {
                id2AttrMap.put(attrVo.getId(), attrVo);
                name2AttrListMap.computeIfAbsent(attrVo.getName(), key -> new ArrayList<>()).add(attrVo);
            }
            for (int j = 0; j < attrArray.size(); j++) {
                JSONObject attrObj = attrArray.getJSONObject(j);
                if (MapUtils.isNotEmpty(attrObj)) {
                    String path = "attrList[" + j + "].";
                    AttrVo attrVo = null;
                    Long id = attrObj.getLong("id");
                    String name = attrObj.getString("name");
                    if (id == null && StringUtils.isBlank(name)) {
                        throw new ParamNotExistsException(path + "id", path + "name");
                    } else if (id != null) {
                        attrVo = id2AttrMap.get(id);
                        if (attrVo == null) {
                            throw new AttrNotFoundException(ciVo.getName(), id.toString());
                        }
                    } else if (StringUtils.isNotBlank(name)) {
                        List<AttrVo> list = name2AttrListMap.get(name);
                        if (CollectionUtils.isEmpty(list)) {
                            throw new AttrNotFoundException(ciVo.getName(), name);
                        }
                        if (list.size() > 1) {
                            throw new ApiRuntimeException(ciVo.getName() + "模型中存在两个名称为" + name + "的属性");
                        }
                        attrVo = list.get(0);
                    }
                    if (attrVo != null) {
                        AttrMappingVo attrMappingVo = new AttrMappingVo();
                        attrMappingVo.setId(attrVo.getId());
                        attrMappingVo.setName(attrVo.getName());
                        attrMappingVo.setLabel(attrVo.getLabel());
                        attrMappingVo.setType(attrVo.getType());
                        attrMappingVo.setConfig(attrVo.getConfig());
                        JSONArray valueList = attrObj.getJSONArray("valueList");
                        JSONObject valueMapping = attrObj.getJSONObject("valueMapping");
                        if (CollectionUtils.isEmpty(valueList) && MapUtils.isEmpty(valueMapping)) {
                            throw new ParamNotExistsException(path + "valueList", path + "valueMapping");
                        }
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            attrMappingVo.setValueList(valueList);
                        }
                        if (MapUtils.isNotEmpty(valueMapping)) {
                            String label = valueMapping.getString("label");
                            if (StringUtils.isBlank(label)) {
                                throw new ParamNotExistsException(path + "valueMapping.label");
                            }
                            Long attrId = valueMapping.getLong("attrId");
                            Long ciId = valueMapping.getLong("ciId");
                            AttrValueMappingVo attrValueMappingVo = new AttrValueMappingVo();
                            attrValueMappingVo.setCiId(ciId);
                            attrValueMappingVo.setAttrId(attrId);
                            attrValueMappingVo.setLabel(label);
                            attrMappingVo.setValueMapping(attrValueMappingVo);
                        }
                        attrList.add(attrMappingVo);
                    }
                }
            }
            ciMappingVo.setAttrList(attrList);
        }
        JSONArray globalAttrArray = syncCiObj.getJSONArray("globalAttrList");
        if (CollectionUtils.isNotEmpty(globalAttrArray)) {
            GlobalAttrVo search = new GlobalAttrVo();
            search.setIsActive(1);
            List<GlobalAttrVo> globalAttrVoList = globalAttrMapper.searchGlobalAttr(search);
            Map<String, GlobalAttrVo> globalAttrVoMap = globalAttrVoList.stream().filter(Objects::nonNull).filter(e -> e.getName() != null).collect(Collectors.toMap(GlobalAttrVo::getName, e -> e));
            List<GlobalAttrMappingVo> globalAttrMappingList = new ArrayList<>();
            for (int i = 0; i < globalAttrArray.size(); i++) {
                JSONObject globalAttrObj = globalAttrArray.getJSONObject(i);
                if (MapUtils.isNotEmpty(globalAttrObj)) {
                    String path = pathPrefix + "globalAttrList[" + i + "].";
                    String name = globalAttrObj.getString("name");
                    if (StringUtils.isBlank(name)) {
                        throw new ParamNotExistsException(path + "name");
                    }
                    GlobalAttrVo globalAttrVo = globalAttrVoMap.get(name);
                    if (globalAttrVo == null) {
                        throw new GlobalAttrNotFoundException(name);
                    }
                    JSONArray valueList = globalAttrObj.getJSONArray("valueList");
                    JSONObject valueMapping = globalAttrObj.getJSONObject("valueMapping");
                    if (CollectionUtils.isEmpty(valueList) && MapUtils.isEmpty(valueMapping)) {
                        throw new ParamNotExistsException(path + "valueList", path + "valueMapping");
                    }
                    GlobalAttrMappingVo globalAttrMappingVo = new GlobalAttrMappingVo();
                    globalAttrMappingVo.setId(globalAttrVo.getId());
                    globalAttrMappingVo.setName(globalAttrVo.getName());
                    globalAttrMappingVo.setLabel(globalAttrVo.getLabel());
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        JSONArray newValueList = new JSONArray();
                        List<GlobalAttrItemVo> allGlobalAttrItemList = globalAttrMapper.getAllGlobalAttrItemByAttrId(globalAttrVo.getId());
                        if (CollectionUtils.isNotEmpty(allGlobalAttrItemList)) {
                            for (int j = 0; j < valueList.size(); j++) {
                                String value = valueList.getString(j);
                                if (StringUtils.isNotBlank(value)) {
                                    for (GlobalAttrItemVo globalAttrItemVo : allGlobalAttrItemList) {
                                        if (value.equalsIgnoreCase(globalAttrItemVo.getValue())) {
                                            JSONObject newValueObj = new JSONObject();
                                            newValueObj.put("attrId", globalAttrItemVo.getAttrId());
                                            newValueObj.put("id", globalAttrItemVo.getId());
                                            newValueObj.put("value", globalAttrItemVo.getValue());
                                            newValueObj.put("sort", globalAttrItemVo.getSort());
                                            newValueList.add(newValueObj);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        globalAttrMappingVo.setValueList(newValueList);
                    }
                    if (MapUtils.isNotEmpty(valueMapping)) {
                        String label = valueMapping.getString("label");
                        if (StringUtils.isBlank(label)) {
                            throw new ParamNotExistsException(path + "valueMapping.label");
                        }
                        Long attrId = valueMapping.getLong("attrId");
                        Long ciId = valueMapping.getLong("ciId");
                        AttrValueMappingVo attrValueMappingVo = new AttrValueMappingVo();
                        attrValueMappingVo.setCiId(ciId);
                        attrValueMappingVo.setAttrId(attrId);
                        attrValueMappingVo.setLabel(label);
                        globalAttrMappingVo.setValueMapping(attrValueMappingVo);
                    }
                    globalAttrMappingList.add(globalAttrMappingVo);
                }
            }
            ciMappingVo.setGlobalAttrList(globalAttrMappingList);
        }
        JSONArray relArray = syncCiObj.getJSONArray("relList");
        if (CollectionUtils.isNotEmpty(relArray)) {
            List<RelMappingVo> relMappingList = new ArrayList<>();
            Map<Long, RelVo> id2RelMap = new HashMap<>();
            Map<String, RelVo> name2RelMap = new HashMap<>();
            List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
            for (RelVo relVo : relList) {
                id2RelMap.put(relVo.getId(), relVo);
                name2RelMap.put(relVo.getFromName() + "#" + relVo.getToName(), relVo);
            }
            for (int j = 0; j < relArray.size(); j++) {
                JSONObject relObj = relArray.getJSONObject(j);
                if (MapUtils.isNotEmpty(relObj)) {
                    String path = pathPrefix + "relList[" + j + "].";
                    RelVo relVo = null;
                    Long id = relObj.getLong("id");
                    String fromName = relObj.getString("fromName");
                    String toName = relObj.getString("toName");
                    if (id == null && (StringUtils.isBlank(fromName) || StringUtils.isBlank(toName)) ) {
                        if (StringUtils.isBlank(fromName)) {
                            throw new ParamNotExistsException(path + "id", path + "fromName");
                        } else {
                            throw new ParamNotExistsException(path + "id", path + "toName");
                        }
                    } else if (id != null) {
                        relVo = id2RelMap.get(id);
                        if (relVo == null) {
                            throw new RelNotFoundException(id);
                        }
                    } else {
                        relVo = name2RelMap.get(fromName + "#" + toName);
                        if (relVo == null) {
                            throw new ApiRuntimeException(ciVo.getName() + "模型中不存在fromName为" + fromName + ",toName为" + toName + "的关系");
                        }
                    }
                    RelMappingVo relMappingVo = new RelMappingVo();
                    relMappingVo.setId(relVo.getId());
                    relMappingVo.setFromName(relVo.getFromName());
                    relMappingVo.setFromLabel(relVo.getFromLabel());
                    relMappingVo.setToName(relVo.getToName());
                    relMappingVo.setToLabel(relVo.getToLabel());
                    relMappingVo.setDirection(relVo.getDirection());
                    relMappingVo.setFromRule(relVo.getFromRule());
                    relMappingVo.setToRule(relVo.getToRule());
                    JSONObject valueMapping = relObj.getJSONObject("valueMapping");
                    if (valueMapping == null) {
                        throw new ParamNotExistsException("valueMapping");
                    }
                    CiMappingVo relCiMappingVo = checkSyncCi(valueMapping, path + "valueMapping.");
                    relMappingVo.setValueMapping(relCiMappingVo);
                    relMappingList.add(relMappingVo);
                }
            }
            ciMappingVo.setRelList(relMappingList);
        }
        return ciMappingVo;
    }

    private static class CiMappingVo {
        private Long ciId;
        private String ciName;
        private String ciLabel;
        private List<AttrMappingVo> attrList;
        private List<RelMappingVo> relList;
        private List<GlobalAttrMappingVo> globalAttrList;
        private String description;
        private List<Long> uniqueAttrIdList;
        private Long nameAttrId;

        public Long getCiId() {
            return ciId;
        }

        public void setCiId(Long ciId) {
            this.ciId = ciId;
        }

        public String getCiName() {
            return ciName;
        }

        public void setCiName(String ciName) {
            this.ciName = ciName;
        }

        public String getCiLabel() {
            return ciLabel;
        }

        public void setCiLabel(String ciLabel) {
            this.ciLabel = ciLabel;
        }

        public List<AttrMappingVo> getAttrList() {
            return attrList;
        }

        public void setAttrList(List<AttrMappingVo> attrList) {
            this.attrList = attrList;
        }

        public List<RelMappingVo> getRelList() {
            return relList;
        }

        public void setRelList(List<RelMappingVo> relList) {
            this.relList = relList;
        }

        public List<GlobalAttrMappingVo> getGlobalAttrList() {
            return globalAttrList;
        }

        public void setGlobalAttrList(List<GlobalAttrMappingVo> globalAttrList) {
            this.globalAttrList = globalAttrList;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Long> getUniqueAttrIdList() {
            return uniqueAttrIdList;
        }

        public void setUniqueAttrIdList(List<Long> uniqueAttrIdList) {
            this.uniqueAttrIdList = uniqueAttrIdList;
        }

        public Long getNameAttrId() {
            return nameAttrId;
        }

        public void setNameAttrId(Long nameAttrId) {
            this.nameAttrId = nameAttrId;
        }
    }

    private static class AttrMappingVo {
        private Long id;
        private String name;
        private String label;
        private JSONArray valueList;
        private AttrValueMappingVo valueMapping;

        private String type;
        private JSONObject config;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public JSONArray getValueList() {
            return valueList;
        }

        public void setValueList(JSONArray valueList) {
            this.valueList = valueList;
        }

        public AttrValueMappingVo getValueMapping() {
            return valueMapping;
        }

        public void setValueMapping(AttrValueMappingVo valueMapping) {
            this.valueMapping = valueMapping;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public JSONObject getConfig() {
            return config;
        }

        public void setConfig(JSONObject config) {
            this.config = config;
        }
    }

    private static class RelMappingVo {
        private Long id;
        private String fromName;
        private String fromLabel;
        private String fromRule;
        private String toName;
        private String toLabel;
        private String toRule;
        private CiMappingVo valueMapping;

        private String direction;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getFromLabel() {
            return fromLabel;
        }

        public void setFromLabel(String fromLabel) {
            this.fromLabel = fromLabel;
        }

        public String getToName() {
            return toName;
        }

        public void setToName(String toName) {
            this.toName = toName;
        }

        public String getToLabel() {
            return toLabel;
        }

        public void setToLabel(String toLabel) {
            this.toLabel = toLabel;
        }

        public CiMappingVo getValueMapping() {
            return valueMapping;
        }

        public void setValueMapping(CiMappingVo valueMapping) {
            this.valueMapping = valueMapping;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getFromRule() {
            return fromRule;
        }

        public void setFromRule(String fromRule) {
            this.fromRule = fromRule;
        }

        public String getToRule() {
            return toRule;
        }

        public void setToRule(String toRule) {
            this.toRule = toRule;
        }
    }

    private static class GlobalAttrMappingVo {
        private Long id;
        private String name;
        private String label;
        private JSONArray valueList;
        private AttrValueMappingVo valueMapping;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public JSONArray getValueList() {
            return valueList;
        }

        public void setValueList(JSONArray valueList) {
            this.valueList = valueList;
        }

        public AttrValueMappingVo getValueMapping() {
            return valueMapping;
        }

        public void setValueMapping(AttrValueMappingVo valueMapping) {
            this.valueMapping = valueMapping;
        }
    }

    private static class AttrValueMappingVo {
        private Long ciId;
        private Long attrId;
        private String label;

        public Long getCiId() {
            return ciId;
        }

        public void setCiId(Long ciId) {
            this.ciId = ciId;
        }

        public Long getAttrId() {
            return attrId;
        }

        public void setAttrId(Long attrId) {
            this.attrId = attrId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
