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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.ADMIN;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.sync.ObjectVo;
import neatlogic.framework.cmdb.exception.sync.CollectionNameFieldValueRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.exception.integration.IntegrationHandlerNotFoundException;
import neatlogic.framework.integration.core.IIntegrationHandler;
import neatlogic.framework.integration.core.IntegrationHandlerFactory;
import neatlogic.framework.integration.dao.mapper.IntegrationMapper;
import neatlogic.framework.integration.dto.IntegrationResultVo;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.framework.matrix.exception.MatrixExternalAccessException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HanyuPinyinUtil;
import neatlogic.framework.util.Md5Util;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.sync.ObjectMapper;
import neatlogic.module.framework.integration.handler.FrameworkRequestFrom;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@AuthAction(action = ADMIN.class)
@AuthUser(SystemUser.SYSTEM)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SyncCiEntityDataToMongoDBForBalantFlowApi extends PrivateApiComponentBase {

    private final static Logger logger = LoggerFactory.getLogger(SyncCiEntityDataToMongoDBForBalantFlowApi.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private IntegrationMapper integrationMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "同步CMDB配置项数据到mongodb";
    }

    @Override
    public JSONObject example() {
        JSONObject defaultJson = new JSONObject(true);
        JSONArray dictionaryConfigList = new JSONArray();
        {
            JSONObject dictionaryConfigObj = new JSONObject(true);
            dictionaryConfigObj.put("dictionaryName", "APP");
            dictionaryConfigObj.put("dictionaryLabel", "应用系统");
            dictionaryConfigObj.put("dictionaryCollection", "COLLECT_BALANTFLOWCIENTITY");
            dictionaryConfigObj.put("_OBJ_CATEGORY", "APPLICATION");
            dictionaryConfigObj.put("_OBJ_TYPE", "APPLICATION");
            dictionaryConfigObj.put("neatLogicCiName", "APP");
            dictionaryConfigObj.put("balantflowCiId", "99");
            dictionaryConfigObj.put("balantflowCiName", "应用系统");
            dictionaryConfigList.add(dictionaryConfigObj);
        }
        {
            JSONObject dictionaryConfigObj = new JSONObject(true);
            dictionaryConfigObj.put("dictionaryName", "APPComponent");
            dictionaryConfigObj.put("dictionaryLabel", "应用模块");
            dictionaryConfigObj.put("dictionaryCollection", "COLLECT_BALANTFLOWCIENTITY");
            dictionaryConfigObj.put("_OBJ_CATEGORY", "APPLICATION");
            dictionaryConfigObj.put("_OBJ_TYPE", "APPLICATION_MODULE");
            dictionaryConfigObj.put("neatLogicCiName", "APPComponent");
            dictionaryConfigObj.put("balantflowCiId", "99");
            dictionaryConfigObj.put("balantflowCiName", "应用系统");
            dictionaryConfigList.add(dictionaryConfigObj);
        }
        defaultJson.put("dictionaryConfigList", dictionaryConfigList);
        String integrationName = "获取数据的集成名称";
        defaultJson.put("integrationName", integrationName);
        JSONObject integrationParam = new JSONObject();
        defaultJson.put("integrationParam", integrationParam);
        Integer integrationRequestMaxCount = 10000;
        defaultJson.put("integrationRequestMaxCount", integrationRequestMaxCount);
        return defaultJson;
    }

    @Input({
            @Param(name = "dictionaryConfigList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "字典配置列表"),
            @Param(name = "integrationName", type = ApiParamType.STRING, isRequired = true, desc = "集成名称"),
            @Param(name = "integrationParam", type = ApiParamType.JSONOBJECT, desc = "集成输入参数"),
            @Param(name = "integrationRequestMaxCount", type = ApiParamType.INTEGER, defaultValue = "10000", desc = "集成请求最大次数，默认是10000"),
    })
    @Output({

    })
    @Description(desc = "同步CMDB配置项数据，获取数据接口是balantflow/restservices/cmdb/glasnostViewQueryRestComponentApi?currentPage=1&tPageSize=20&id=65")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        JSONArray dictionaryConfigList = paramObj.getJSONArray("dictionaryConfigList");
        String integrationName = paramObj.getString("integrationName");
        IntegrationVo supplierIntegrationVo = integrationMapper.getIntegrationByName(integrationName);
        if (supplierIntegrationVo != null) {
            IIntegrationHandler handler = IntegrationHandlerFactory.getHandler(supplierIntegrationVo.getHandler());
            if (handler == null) {
                throw new IntegrationHandlerNotFoundException(supplierIntegrationVo.getHandler());
            }
            JSONObject integrationParam = paramObj.getJSONObject("integrationParam");
            if (MapUtils.isNotEmpty(integrationParam)) {
                supplierIntegrationVo.getParamObj().putAll(integrationParam);
            }
            boolean flag = false;
            Map<Long, Long> attrId2CiIdMap = new HashMap<>();
            JSONArray dictionaryList = new JSONArray();
            LocalDateTime now = LocalDateTime.now();
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
                        if (!flag) {
                            Long id = returnObj.getLong("id");
                            String name = returnObj.getString("name");
                            JSONArray columnList = returnObj.getJSONArray("columnList");
                            if (CollectionUtils.isNotEmpty(columnList)) {
                                for (int i = 0; i < columnList.size(); i++) {
                                    JSONObject columnObj = columnList.getJSONObject(i);
                                    if (MapUtils.isNotEmpty(columnObj)) {
                                        Long ciId = columnObj.getLong("ciId");
                                        Long attrId = columnObj.getLong("attrId");
                                        String label = columnObj.getString("label");
                                        attrId2CiIdMap.put(attrId, ciId);
                                    }
                                }
                            }
                            dictionaryList = generateDictionaryList(id, name, columnList, dictionaryConfigList);
                            saveDictionaryList(dictionaryList, now);
                            flag = true;
                        }
                        JSONArray resultList = returnObj.getJSONArray("resultList");
                        if (CollectionUtils.isNotEmpty(resultList)) {
                            savePageData(resultList, attrId2CiIdMap, dictionaryList, now);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return resultObj;
    }

    private JSONArray generateDictionaryList(
            Long customViewId,
            String customViewName,
            JSONArray columnList,
            JSONArray dictionaryConfigList
    ) {
        JSONArray dictionaryList = new JSONArray();
        Set<Long> balantflowCiIdSet = new HashSet<>();
        Map<Long, String> attrId2AttrLabelMap = new HashMap<>();
        Map<Long, JSONArray> ciId2AttrListMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(columnList)) {
            for (int i = 0; i < columnList.size(); i++) {
                JSONObject columnObj = columnList.getJSONObject(i);
                if (MapUtils.isNotEmpty(columnObj)) {
                    Long ciId = columnObj.getLong("ciId");
                    Long attrId = columnObj.getLong("attrId");
                    String label = columnObj.getString("label");
                    balantflowCiIdSet.add(ciId);
                    JSONObject attrObj = new JSONObject(true);
                    String name = HanyuPinyinUtil.format(label + "_" + attrId);
                    attrObj.put("name", name);
                    attrObj.put("desc", label + "_" + attrId);
                    attrObj.put("type", "String");
                    attrObj.put("attrId", attrId);
                    ciId2AttrListMap.computeIfAbsent(ciId, key -> new JSONArray()).add(attrObj);
                    attrId2AttrLabelMap.put(attrId, label);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(dictionaryConfigList)) {
            Map<Long, String> ciId2CiName = new HashMap<>();
            for (int i = 0; i < dictionaryConfigList.size(); i++) {
                JSONObject dictionaryConfigObj = dictionaryConfigList.getJSONObject(i);
                if (MapUtils.isNotEmpty(dictionaryConfigObj)) {
                    Long balantflowCiId = dictionaryConfigObj.getLong("balantflowCiId");
                    String balantflowCiName = dictionaryConfigObj.getString("balantflowCiName");
                    if (balantflowCiId != null) {
                        if (StringUtils.isNotBlank(balantflowCiName)) {
                            ciId2CiName.put(balantflowCiId, balantflowCiName);
                        }
                    }
                }
            }
            for (int i = 0; i < dictionaryConfigList.size(); i++) {
                JSONObject dictionaryConfigObj = dictionaryConfigList.getJSONObject(i);
                if (MapUtils.isNotEmpty(dictionaryConfigObj)) {
                    String dictionaryName = dictionaryConfigObj.getString("dictionaryName");
                    String dictionaryLabel = dictionaryConfigObj.getString("dictionaryLabel");
                    String dictionaryCollection = dictionaryConfigObj.getString("dictionaryCollection");
                    String dictionaryCollectionLabel = dictionaryConfigObj.getString("dictionaryCollectionLabel");
                    String _OBJ_CATEGORY = dictionaryConfigObj.getString("_OBJ_CATEGORY");
                    String _OBJ_TYPE = dictionaryConfigObj.getString("_OBJ_TYPE");
                    String neatLogicCiName = dictionaryConfigObj.getString("neatLogicCiName");
                    Long balantflowCiId = dictionaryConfigObj.getLong("balantflowCiId");
                    String balantflowCiName = dictionaryConfigObj.getString("balantflowCiName");
                    List<Long> multipleSelectTypeAttrIdList = new ArrayList<>();
                    JSONArray multipleSelectTypeAttrIdArray = dictionaryConfigObj.getJSONArray("multipleSelectTypeAttrIdList");
                    if (CollectionUtils.isNotEmpty(multipleSelectTypeAttrIdArray)) {
                        multipleSelectTypeAttrIdList = multipleSelectTypeAttrIdArray.toJavaList(Long.class);
                    }
                    if (StringUtils.isNotBlank(dictionaryName)) {
                        JSONObject dictionaryObj = new JSONObject();
                        String id = UuidUtil.getCustomUUID(dictionaryName);
                        dictionaryObj.put("_id", id);
                        dictionaryObj.put("name", dictionaryName);
                        dictionaryObj.put("label", dictionaryLabel);
                        dictionaryObj.put("collection", dictionaryCollection);
                        dictionaryObj.put("collectionLabel", dictionaryCollectionLabel);
                        dictionaryObj.put("_OBJ_CATEGORY", _OBJ_CATEGORY);
                        dictionaryObj.put("_OBJ_TYPE", _OBJ_TYPE);
                        dictionaryObj.put("neatLogicCiName", neatLogicCiName);
                        dictionaryObj.put("balantflowCiId", balantflowCiId);
                        dictionaryObj.put("balantflowCiName", balantflowCiName);
                        JSONArray fields = new JSONArray();
                        for (Long ciId : balantflowCiIdSet) {
                            String ciName = ciId2CiName.get(ciId);
                            JSONObject fieldObj = generateField(ciId, ciName, multipleSelectTypeAttrIdList, columnList);
                            if (MapUtils.isNotEmpty(fieldObj)) {
                                fields.add(fieldObj);
                            }
                            dictionaryObj.put("fields", fields);
                        }
                        JSONObject balantflowObj = new JSONObject();
                        balantflowObj.put("customViewId", customViewId);
                        balantflowObj.put("customViewName", customViewName);
                        dictionaryObj.put("balantflow", balantflowObj);
                        JSONObject filterObj = new JSONObject();
                        filterObj.put("dictionaryName", dictionaryName);
                        dictionaryObj.put("filter", filterObj);
                        dictionaryList.add(dictionaryObj);
                    }
                }
            }
        }
        return dictionaryList;
    }

    private JSONObject generateField(Long balantflowCiId, String balantflowCiName, List<Long> multipleSelectTypeAttrIdList, JSONArray columnList) {
        Map<Long, JSONArray> ciId2AttrListMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(columnList)) {
            for (int i = 0; i < columnList.size(); i++) {
                JSONObject columnObj = columnList.getJSONObject(i);
                if (MapUtils.isNotEmpty(columnObj)) {
                    Long ciId = columnObj.getLong("ciId");
                    Long attrId = columnObj.getLong("attrId");
                    String label = columnObj.getString("label");
                    JSONObject attrObj = new JSONObject(true);
                    String name = HanyuPinyinUtil.format(label + "_" + attrId);
                    attrObj.put("name", name);
                    attrObj.put("desc", label + "_" + attrId);
                    attrObj.put("attrId", attrId);
                    if (multipleSelectTypeAttrIdList.contains(attrId)) {
                        attrObj.put("type", "JsonArray");
                        JSONArray subset = new JSONArray();
                        JSONObject subObj = new JSONObject();
                        subObj.put("name", name);
                        subObj.put("desc", label + "_" + attrId);
                        subObj.put("type", "String");
                        subset.add(subObj);
                        attrObj.put("subset", subset);
                    } else {
                        attrObj.put("type", "String");
                    }
                    ciId2AttrListMap.computeIfAbsent(ciId, key -> new JSONArray()).add(attrObj);
                }
            }
        }
        JSONArray subset = ciId2AttrListMap.get(balantflowCiId);
        if (CollectionUtils.isNotEmpty(subset)) {
            String desc = "模型_" + balantflowCiId;
            if (StringUtils.isNotBlank(balantflowCiName)) {
                desc = balantflowCiName;
            }
            String name = HanyuPinyinUtil.format(desc);
            JSONObject fieldObj = new JSONObject(true);
            fieldObj.put("ciId", balantflowCiId);
            fieldObj.put("name", name);
            fieldObj.put("desc", desc);
            fieldObj.put("type", "JsonArray");
            fieldObj.put("subset", subset);
            return fieldObj;
        }
        return null;
    }

    private void saveDictionaryList(JSONArray dictionaryList, LocalDateTime localDateTime) {
        if (CollectionUtils.isNotEmpty(dictionaryList)) {
            JSONObject dictionaryObj = dictionaryList.getJSONObject(0);
            String id = dictionaryObj.getString("_id");
            String name = dictionaryObj.getString("name");
            Query query = new Query();
            query.addCriteria(Criteria.where("name").is(name));
            List<JSONObject> collectionVoList = mongoTemplate.find(query, JSONObject.class, "_dictionary");
            if (CollectionUtils.isNotEmpty(collectionVoList)) {
                if (collectionVoList.size() > 1) {
                    throw new CollectionNameFieldValueRepeatException("_dictionary", name);
                }
                JSONObject collectionObj = collectionVoList.get(0);
                String _id = collectionObj.getString("_id");
                if (!Objects.equals(_id, id)) {
                    throw new CollectionNameFieldValueRepeatException("_dictionary", name);
                }
                collectionObj = JSONObject.parseObject(collectionObj.toJSONString());
                if (!Objects.equals(
                        Md5Util.encryptMD5(JSONObject.toJSONString(collectionObj, SerializerFeature.MapSortField)),
                        Md5Util.encryptMD5(JSONObject.toJSONString(dictionaryObj, SerializerFeature.MapSortField))
                )
                ) {
                    dictionaryObj.put("lcd", localDateTime.format(dateTimeFormatter));
                    mongoTemplate.findAndReplace(query, dictionaryObj, "_dictionary");
                }
            } else {
                dictionaryObj.put("fcd", localDateTime.format(dateTimeFormatter));
                mongoTemplate.insert(dictionaryObj, "_dictionary");
            }
        }
        for (int i = 0; i < dictionaryList.size(); i++) {
            JSONObject dictionaryObj = dictionaryList.getJSONObject(i);
            String _OBJ_CATEGORY = dictionaryObj.getString("_OBJ_CATEGORY");
            String _OBJ_TYPE = dictionaryObj.getString("_OBJ_TYPE");
            String neatLogicCiName = dictionaryObj.getString("neatLogicCiName");
            if (StringUtils.isNotBlank(neatLogicCiName)) {
                CiVo ciVo = ciMapper.getCiByName(neatLogicCiName);
                if (ciVo != null) {
                    ObjectVo oldObjectVo = objectMapper.getObjectByCategoryAndType(_OBJ_CATEGORY, _OBJ_TYPE);
                    if (oldObjectVo == null) {
                        ObjectVo objectVo = new ObjectVo();
                        objectVo.setObjCategory(_OBJ_CATEGORY);
                        objectVo.setObjType(_OBJ_TYPE);
                        objectVo.setCiId(ciVo.getId());
                        objectMapper.insertObject(objectVo);
                    } else {
                        if (!Objects.equals(oldObjectVo.getCiId(), ciVo.getId())) {
                            oldObjectVo.setCiId(ciVo.getId());
                            objectMapper.updateObject(oldObjectVo);
                        }
                    }
                }
            }
        }
    }


    private void savePageData(
            JSONArray resultList,
            Map<Long, Long> attrId2CiIdMap,
            JSONArray dictionaryList,
            LocalDateTime localDateTime
    ) {
        for (int i = 0; i < resultList.size(); i++) {
            JSONObject row = resultList.getJSONObject(i);
            Long balantflowCiEntityId = row.getLong("ciEntityId");
            JSONArray attrList = row.getJSONArray("attrList");
            if (CollectionUtils.isNotEmpty(attrList)) {
                saveRowData(balantflowCiEntityId, attrList, attrId2CiIdMap, dictionaryList, localDateTime);
            }
        }
    }

    private void saveRowData(
            Long balantflowCiEntityId,
            JSONArray attrList,
            Map<Long, Long> attrId2CiIdMap,
            JSONArray dictionaryList,
            LocalDateTime localDateTime
    ) {
        for (int i = 0; i < attrList.size(); i++) {
            JSONObject attrObj = attrList.getJSONObject(i);
            if (MapUtils.isNotEmpty(attrObj)) {
                Long attrId = attrObj.getLong("attrId");
                Long ciId = attrId2CiIdMap.get(attrId);
                attrObj.put("ciId", ciId);
            }
        }
        Map<Long, JSONObject> ciId2DictionaryMap = new HashMap<>();
        for (int i = 0; i < dictionaryList.size(); i++) {
            JSONObject dictionaryObj = dictionaryList.getJSONObject(i);
            Long ciId = dictionaryObj.getLong("balantflowCiId");
            if (ciId != null) {
                ciId2DictionaryMap.put(ciId, dictionaryObj);
            }
        }
//        for (int i = 0; i < dictionaryList.size(); i++) {
            JSONObject dictionaryObj = dictionaryList.getJSONObject(0);
            String collection = dictionaryObj.getString("collection");
            if (StringUtils.isNotBlank(collection)) {
                JSONObject dataObj = generateData(balantflowCiEntityId, attrList, dictionaryObj, ciId2DictionaryMap, localDateTime);
                mongoTemplate.insert(dataObj, collection);
            }
//        }
    }

    private JSONObject generateData(
            Long balantflowCiEntityId,
            JSONArray attrList,
            JSONObject dictionaryObj,
            Map<Long, JSONObject> ciId2DictionaryMap,
            LocalDateTime localDateTime
    ) {
        JSONObject dataObj = new JSONObject();
        JSONArray fields = dictionaryObj.getJSONArray("fields");
        if (CollectionUtils.isNotEmpty(fields)) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject fieldObj = fields.getJSONObject(i);
                if (MapUtils.isNotEmpty(fieldObj)) {
                    Long ciId = fieldObj.getLong("ciId");
                    String name = fieldObj.getString("name");
                    JSONArray subset = fieldObj.getJSONArray("subset");
                    if (CollectionUtils.isNotEmpty(subset)) {
                        JSONObject jsonObj = new JSONObject();
                        JSONObject subDictionaryObj = ciId2DictionaryMap.get(ciId);
                        if (MapUtils.isNotEmpty(subDictionaryObj)) {
                            String _OBJ_CATEGORY = subDictionaryObj.getString("_OBJ_CATEGORY");
                            jsonObj.put("_OBJ_CATEGORY", _OBJ_CATEGORY);
                            String _OBJ_TYPE = subDictionaryObj.getString("_OBJ_TYPE");
                            jsonObj.put("_OBJ_TYPE", _OBJ_TYPE);
                        }
                        JSONArray jsonArray = new JSONArray();
                        for (int j = 0; j < subset.size(); j++) {
                            JSONObject subObj = subset.getJSONObject(j);
                            if (MapUtils.isNotEmpty(subObj)) {
                                String subName = subObj.getString("name");
                                Long attrId = subObj.getLong("attrId");
                                String type = subObj.getString("type");
                                if (Objects.equals(type, "String")) {
                                    for (int k = 0; k < attrList.size(); k++) {
                                        JSONObject attrObj = attrList.getJSONObject(k);
                                        if (MapUtils.isNotEmpty(attrObj)) {
                                            if (Objects.equals(attrId, attrObj.getLong("attrId"))) {
                                                String value = attrObj.getString("value");
                                                if (StringUtils.isNotBlank(value)) {
                                                    jsonObj.put(subName, value.trim());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } else if (Objects.equals(type, "JsonArray")) {
                                    String subName1 = null;
                                    JSONArray subset1 = subObj.getJSONArray("subset");
                                    if (CollectionUtils.isNotEmpty(subset1)) {
                                        JSONObject subObj1 = subset1.getJSONObject(0);
                                        if (MapUtils.isNotEmpty(subObj1)) {
                                            subName1 = subObj1.getString("name");
                                        }
                                    }
                                    List<String> valueList = new ArrayList<>();
                                    for (int k = 0; k < attrList.size(); k++) {
                                        JSONObject attrObj = attrList.getJSONObject(k);
                                        if (MapUtils.isNotEmpty(attrObj)) {
                                            if (Objects.equals(attrId, attrObj.getLong("attrId"))) {
                                                Object value = attrObj.get("value");
                                                if (value != null) {
                                                    if (value instanceof String) {
                                                        String valueStr = (String) value;
                                                        if (valueStr.contains("<br>")) {
                                                            String[] split = valueStr.split("<br>");
                                                            for (String str : split) {
                                                                if (StringUtils.isNotBlank(str)) {
                                                                    valueList.add(str.trim());
                                                                }
                                                            }
                                                        } else {
                                                            if (StringUtils.isNotBlank(valueStr)) {
                                                                valueList.add(valueStr.trim());
                                                            }
                                                        }
                                                    } else {
                                                        valueList.add(value.toString());
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    if (StringUtils.isNotBlank(subName1) && CollectionUtils.isNotEmpty(valueList)) {
                                        JSONArray array = new JSONArray();
                                        for (String value : valueList) {
                                            JSONObject json = new JSONObject();
                                            json.put(subName1, value);
                                            array.add(json);
                                        }
                                        jsonObj.put(subName, array);
                                    }
                                }
                            }
                        }
                        jsonArray.add(jsonObj);
                        dataObj.put(name, jsonArray);
                    } else {
                        Long attrId = fieldObj.getLong("attrId");
                        for (int k = 0; k < attrList.size(); k++) {
                            JSONObject attrObj = attrList.getJSONObject(k);
                            if (MapUtils.isNotEmpty(attrObj)) {
                                if (Objects.equals(attrId, attrObj.getLong("attrId"))) {
                                    Object value = attrObj.get("value");
                                    dataObj.put(name, value);
                                }
                            }
                        }
                    }
                }
            }
        }
        String _OBJ_CATEGORY = dictionaryObj.getString("_OBJ_CATEGORY");
        dataObj.put("_OBJ_CATEGORY", _OBJ_CATEGORY);
        String _OBJ_TYPE = dictionaryObj.getString("_OBJ_TYPE");
        dataObj.put("_OBJ_TYPE", _OBJ_TYPE);
        dataObj.put("dictionaryName", dictionaryObj.getString("name"));
        dataObj.put("insertTime", localDateTime.format(dateTimeFormatter));
        dataObj.put("balantflowCiEntityId", balantflowCiEntityId);
        return dataObj;
    }

    @Override
    public String getToken() {
        return "cmdb/ciEntity/data/mongodb/sync";
    }
}
