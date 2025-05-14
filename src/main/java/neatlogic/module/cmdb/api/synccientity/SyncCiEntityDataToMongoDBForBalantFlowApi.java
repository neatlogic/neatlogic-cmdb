/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.api.synccientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.ADMIN;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.core.ApiRuntimeException;
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
import neatlogic.framework.util.Md5Util;
import neatlogic.framework.util.UuidUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AuthAction(action = ADMIN.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SyncCiEntityDataToMongoDBForBalantFlowApi extends PrivateApiComponentBase {

    private final static Logger logger = LoggerFactory.getLogger(SyncCiEntityDataToMongoDBForBalantFlowApi.class);

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private IntegrationMapper integrationMapper;

    @Override
    public String getName() {
        return "同步CMDB配置项数据到mongodb";
    }

    @Override
    public JSONObject example() {
        JSONObject defaultJson = new JSONObject(true);
        JSONArray dictionaryList = new JSONArray();
        {
            JSONObject dictionaryObj = new JSONObject(true);
            dictionaryObj.put("name", "APP");
            dictionaryObj.put("label", "应用系统");
            dictionaryObj.put("collection", "COLLECT_balantflowCiEntity");
            dictionaryObj.put("collection_label", "COLLECT_balantflowCiEntity");
            dictionaryObj.put("docroot", null);
            dictionaryObj.put("filter", new JSONObject());
            JSONArray fields = new JSONArray();
            {
                JSONObject fieldObj = new JSONObject(true);
                fieldObj.put("name", "name");
                fieldObj.put("desc", "名称");
                fieldObj.put("type", "String");
                fields.add(fieldObj);
            }
            {
                JSONObject fieldObj = new JSONObject(true);
                fieldObj.put("name", "abbrName");
                fieldObj.put("desc", "简称");
                fieldObj.put("type", "String");
                fields.add(fieldObj);
            }
            {
                JSONObject fieldObj = new JSONObject(true);
                fieldObj.put("name", "applicationCode");
                fieldObj.put("desc", "编号");
                fieldObj.put("type", "String");
                fields.add(fieldObj);
            }
            {
                JSONObject fieldObj = new JSONObject(true);
                fieldObj.put("name", "APPComponent");
                fieldObj.put("desc", "应用模块");
                fieldObj.put("type", "JsonArray");
                JSONArray subset = new JSONArray();
                {
                    JSONObject subFieldObj = new JSONObject(true);
                    subFieldObj.put("name", "name");
                    subFieldObj.put("desc", "名称");
                    subFieldObj.put("type", "String");
                    subset.add(subFieldObj);
                }
                {
                    JSONObject subFieldObj = new JSONObject(true);
                    subFieldObj.put("name", "abbrName");
                    subFieldObj.put("desc", "简称");
                    subFieldObj.put("type", "String");
                    subset.add(subFieldObj);
                }
                fieldObj.put("subset", subset);
                fields.add(fieldObj);
            }
            dictionaryObj.put("fields", fields);
            dictionaryList.add(dictionaryObj);
        }
        {
            JSONObject dictionaryObj = new JSONObject(true);
            dictionaryObj.put("name", "APPComponent");
            dictionaryObj.put("label", "应用模块");
            dictionaryObj.put("collection", "COLLECT_balantflowCiEntity");
            dictionaryObj.put("collection_label", "COLLECT_balantflowCiEntity");
            dictionaryObj.put("docroot", null);
            dictionaryObj.put("filter", new JSONObject());
            JSONArray fields = new JSONArray();
            {
                JSONObject fieldObj = new JSONObject(true);
                fieldObj.put("name", "name");
                fieldObj.put("desc", "名称");
                fieldObj.put("type", "String");
                fields.add(fieldObj);
            }
            {
                JSONObject fieldObj = new JSONObject(true);
                fieldObj.put("name", "abbrName");
                fieldObj.put("desc", "简称");
                fieldObj.put("type", "String");
                fields.add(fieldObj);
            }
            dictionaryObj.put("fields", fields);
            dictionaryList.add(dictionaryObj);

        }
        defaultJson.put("dictionaryList", dictionaryList);
        JSONArray dictionaryFieldMappingList = new JSONArray();
        {
            JSONObject dictionaryFieldMappingObj = new JSONObject();
            dictionaryFieldMappingObj.put("name", "application");
            dictionaryFieldMappingObj.put("collection", "COLLECT_balantflowCiEntity");
            dictionaryFieldMappingObj.put("_OBJ_CATEGORY", "APPLICATION");
            dictionaryFieldMappingObj.put("_OBJ_TYPE", "APPLICATION");
            JSONArray fieldMappingList = new JSONArray();
            {
                JSONObject fieldMappingObj = new JSONObject();
                fieldMappingObj.put("field", "name");
                JSONObject valueMapping = new JSONObject();
                valueMapping.put("ciId", 110);
                valueMapping.put("attrId", 903);
                valueMapping.put("label", "动态文本框");
                fieldMappingObj.put("valueMapping", valueMapping);
                fieldMappingList.add(fieldMappingObj);
            }
            {
                JSONObject fieldMappingObj = new JSONObject();
                fieldMappingObj.put("field", "abbrName");
                JSONObject valueMapping = new JSONObject();
                valueMapping.put("ciId", 110);
                valueMapping.put("attrId", 903);
                valueMapping.put("label", "动态文本框");
                fieldMappingObj.put("valueMapping", valueMapping);
                fieldMappingList.add(fieldMappingObj);
            }
            {
                JSONObject fieldMappingObj = new JSONObject();
                fieldMappingObj.put("field", "applicationCode");
                JSONObject valueMapping = new JSONObject();
                valueMapping.put("ciId", 110);
                valueMapping.put("attrId", 903);
                valueMapping.put("label", "动态文本框");
                fieldMappingObj.put("valueMapping", valueMapping);
                fieldMappingList.add(fieldMappingObj);
            }
            {
                JSONObject fieldMappingObj = new JSONObject();
                fieldMappingObj.put("field", "APPComponent");
                JSONObject valueMapping = new JSONObject();
                valueMapping.put("refName", "applicationModule");
                fieldMappingObj.put("valueMapping", valueMapping);
                fieldMappingList.add(fieldMappingObj);
            }
            dictionaryFieldMappingObj.put("fieldMappingList", fieldMappingList);
            dictionaryFieldMappingList.add(dictionaryFieldMappingObj);
        }
        {
            JSONObject dictionaryFieldMappingObj = new JSONObject();
            dictionaryFieldMappingObj.put("name", "applicationModule");
//            dictionaryFieldMappingObj.put("collection", "COLLECT_balantflowCiEntity");
            dictionaryFieldMappingObj.put("_OBJ_CATEGORY", "APPLICATION");
            dictionaryFieldMappingObj.put("_OBJ_TYPE", "APPLICATION_MODULE");
            JSONArray fieldMappingList = new JSONArray();
            {
                JSONObject fieldMappingObj = new JSONObject();
                fieldMappingObj.put("field", "name");
                JSONObject valueMapping = new JSONObject();
                valueMapping.put("ciId", 110);
                valueMapping.put("attrId", 903);
                valueMapping.put("label", "动态文本框");
                fieldMappingObj.put("valueMapping", valueMapping);
                fieldMappingList.add(fieldMappingObj);
            }
            {
                JSONObject fieldMappingObj = new JSONObject();
                fieldMappingObj.put("field", "abbrName");
                JSONObject valueMapping = new JSONObject();
                valueMapping.put("ciId", 110);
                valueMapping.put("attrId", 903);
                valueMapping.put("label", "动态文本框");
                fieldMappingObj.put("valueMapping", valueMapping);
                fieldMappingList.add(fieldMappingObj);
            }
            dictionaryFieldMappingObj.put("fieldMappingList", fieldMappingList);
            dictionaryFieldMappingList.add(dictionaryFieldMappingObj);
        }
        defaultJson.put("dictionaryFieldMappingList", dictionaryFieldMappingList);
        String integrationName = "获取数据的集成名称";
        defaultJson.put("integrationName", integrationName);
        JSONObject integrationParam = new JSONObject();
        defaultJson.put("integrationParam", integrationParam);
        Integer integrationRequestMaxCount = 10000;
        defaultJson.put("integrationRequestMaxCount", integrationRequestMaxCount);
        return defaultJson;
    }

    @Input({
            @Param(name = "dictionaryList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "字典列表"),
            @Param(name = "dictionaryFieldMappingList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "字典字段映射列表"),
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
        JSONArray dictionaryList = paramObj.getJSONArray("dictionaryList");
        if (CollectionUtils.isNotEmpty(dictionaryList)) {
            for (int i = 0; i < dictionaryList.size(); i++) {
                JSONObject dictionaryObj = dictionaryList.getJSONObject(i);
                if (MapUtils.isNotEmpty(dictionaryObj)) {
                    String name = dictionaryObj.getString("name");
                    if (StringUtils.isNotBlank(name)) {
                        String id = UuidUtil.getCustomUUID(name);
                        dictionaryObj.put("_id", id);
                        Query query = new Query();
                        query.addCriteria(Criteria.where("name").is(name));
                        List<JSONObject> collectionVoList = mongoTemplate.find(query, JSONObject.class, "_dictionary");
                        if (CollectionUtils.isNotEmpty(collectionVoList)) {
                            if (collectionVoList.size() > 1) {
                                throw new ApiRuntimeException(name + "已存在多个");
                            }
                            JSONObject collectionObj = collectionVoList.get(0);
                            String id1 = collectionObj.getString("_id");
                            if (!Objects.equals(id1, id)) {
                                throw new ApiRuntimeException(name + "已存在");
                            }
                            collectionObj = JSONObject.parseObject(collectionObj.toJSONString());
                            if (!Objects.equals(
                                    Md5Util.encryptMD5(JSONObject.toJSONString(collectionObj, SerializerFeature.MapSortField)),
                                    Md5Util.encryptMD5(JSONObject.toJSONString(dictionaryObj, SerializerFeature.MapSortField))
                            )
                            ) {
                                mongoTemplate.findAndReplace(query, dictionaryObj, "_dictionary");
                            }
                        } else {
                            mongoTemplate.insert(dictionaryObj, "_dictionary");
                        }
                    }
                }
            }
        }
        Map<String, JSONObject> dictionaryFieldMappingMap = new HashMap<>();
        JSONArray dictionaryFieldMappingList = paramObj.getJSONArray("dictionaryFieldMappingList");
        for (int i = 0; i < dictionaryFieldMappingList.size(); i++) {
            JSONObject dictionaryFieldMappingObj = dictionaryFieldMappingList.getJSONObject(i);
            if (MapUtils.isNotEmpty(dictionaryFieldMappingObj)) {
                String name = dictionaryFieldMappingObj.getString("name");
                if (StringUtils.isNotBlank(name)) {
                    dictionaryFieldMappingMap.put(name, dictionaryFieldMappingObj);
                }
            }
        }
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
                        JSONArray resultList = returnObj.getJSONArray("resultList");
                        if (CollectionUtils.isNotEmpty(resultList)) {
                            savePageData(resultList, dictionaryFieldMappingMap);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return resultObj;
    }

    private void savePageData(
            JSONArray resultList,
            Map<String, JSONObject> dictionaryFieldMappingMap
    ) {
        for (int i = 0; i < resultList.size(); i++) {
            JSONObject row = resultList.getJSONObject(i);
            JSONArray attrList = row.getJSONArray("attrList");
            if (CollectionUtils.isNotEmpty(attrList)) {
                saveRowData(attrList, dictionaryFieldMappingMap);
            }
        }
    }

    private void saveRowData(
            JSONArray attrList,
            Map<String, JSONObject> dictionaryFieldMappingMap
    ) {
        for (Map.Entry<String, JSONObject> entry : dictionaryFieldMappingMap.entrySet()) {
            JSONObject dictionaryFieldMappingObj = entry.getValue();
            String collection = dictionaryFieldMappingObj.getString("collection");
            if (StringUtils.isNotBlank(collection)) {
                JSONObject dataObj = generateData(attrList, dictionaryFieldMappingMap, dictionaryFieldMappingObj);
                mongoTemplate.insert(dataObj, collection);
            }
        }
    }

    private JSONObject generateData(
            JSONArray attrList,
            Map<String, JSONObject> dictionaryFieldMappingMap,
            JSONObject dictionaryFieldMappingObj
    ) {
        JSONObject dataObj = new JSONObject();
        String _OBJ_CATEGORY = dictionaryFieldMappingObj.getString("_OBJ_CATEGORY");
        dataObj.put("_OBJ_CATEGORY", _OBJ_CATEGORY);
        String _OBJ_TYPE = dictionaryFieldMappingObj.getString("_OBJ_TYPE");
        dataObj.put("_OBJ_TYPE", _OBJ_TYPE);
        JSONArray fieldMappingList = dictionaryFieldMappingObj.getJSONArray("fieldMappingList");
        if (CollectionUtils.isNotEmpty(fieldMappingList)) {
            for (int j = 0; j < fieldMappingList.size(); j++) {
                JSONObject fieldMappingObj = fieldMappingList.getJSONObject(j);
                if (MapUtils.isNotEmpty(fieldMappingObj)) {
                    String field = fieldMappingObj.getString("field");
                    if (StringUtils.isNotBlank(field)) {
                        JSONObject valueMapping = fieldMappingObj.getJSONObject("valueMapping");
                        if (MapUtils.isNotEmpty(valueMapping)) {
                            String refName = valueMapping.getString("refName");
                            if (StringUtils.isNotBlank(refName)) {
                                JSONObject refDictionaryFieldMappingObj = dictionaryFieldMappingMap.get(refName);
                                if (MapUtils.isNotEmpty(refDictionaryFieldMappingObj)) {
                                    JSONObject jsonObject = generateData(attrList, dictionaryFieldMappingMap, refDictionaryFieldMappingObj);
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.add(jsonObject);
                                    dataObj.put(field, jsonArray);
                                }
                            } else {
                                for (int k = 0; k < attrList.size(); k++) {
                                    JSONObject attrObj = attrList.getJSONObject(k);
                                    if (MapUtils.isNotEmpty(attrObj)) {
                                        Long attrId = attrObj.getLong("attrId");
                                        String label = attrObj.getString("label");
                                        if (Objects.equals(valueMapping.getString("label"), label) || Objects.equals(valueMapping.getLong("attrId"), attrId)) {
                                            Object value = attrObj.get("value");
                                            dataObj.put(field, value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return dataObj;
    }

    @Override
    public String getToken() {
        return "cmdb/ciEntity/data/mongodb/sync";
    }
}
