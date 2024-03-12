/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCollectionDataApi extends PrivateApiComponentBase {
    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public String getName() {
        return "nmcas.searchcollectiondataapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "collection", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.collectionname"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")
    })
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "subTheadData", type = ApiParamType.JSONOBJECT, desc = "子属性表头定义"),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头数据"),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "表格数据")})
    @Description(desc = "nmcas.searchcollectiondataapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String collection = paramObj.getString("collection");
        BasePageVo pageVo = JSON.toJavaObject(paramObj, BasePageVo.class);
        JSONObject resultObj = new JSONObject();
        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(collection)), CollectionVo.class, "_dictionary");
        JSONArray theadList = new JSONArray();
        Query query = new Query();
        Query countQuery = new Query();
        JSONObject subsetData = new JSONObject();
        if (collectionVo != null) {
            List<Criteria> finalCriteria = new ArrayList<>();

            if (MapUtils.isNotEmpty(collectionVo.getFilter())) {
                finalCriteria.add(collectionVo.getFilterCriteria());
            }
            if (CollectionUtils.isNotEmpty(collectionVo.getFields())) {
                List<Criteria> criteriaList = new ArrayList<>();
                Pattern pattern = null;
                if (StringUtils.isNotBlank(pageVo.getKeyword())) {
                    pattern = Pattern.compile("^.*" + pageVo.getKeyword() + ".*$", Pattern.CASE_INSENSITIVE);
                    Criteria c = new Criteria();
                }
                for (int i = 0; i < collectionVo.getFields().size(); i++) {
                    JSONObject fieldObj = collectionVo.getFields().getJSONObject(i);
                    JSONObject headObj = new JSONObject();
                    headObj.put("key", fieldObj.getString("name"));
                    headObj.put("title", fieldObj.getString("desc") + "(" + fieldObj.getString("name") + ")");
                    headObj.put("type", fieldObj.getString("type"));
                    headObj.put("className", "top");
                    theadList.add(headObj);
                    //字符串字段才启用模糊匹配
                    if (pattern != null && fieldObj.getString("type").equalsIgnoreCase("string")) {
                        criteriaList.add(Criteria.where(fieldObj.getString("name")).is(pattern));
                    }
                    if (fieldObj.getString("type").equalsIgnoreCase("array") || (fieldObj.containsKey("subset") && fieldObj.get("subset") instanceof List)) {
                        JSONArray subTheadList = new JSONArray();
                        if (fieldObj.containsKey("subset") && fieldObj.get("subset") instanceof List) {
                            for (int s = 0; s < fieldObj.getJSONArray("subset").size(); s++) {
                                JSONObject subHeadObj = new JSONObject();
                                JSONObject subObj = fieldObj.getJSONArray("subset").getJSONObject(s);
                                subHeadObj.put("key", subObj.getString("name"));
                                subHeadObj.put("title", subObj.getString("desc") + "(" + subObj.getString("name") + ")");
                                subTheadList.add(subHeadObj);
                            }
                        }
                        subsetData.put(fieldObj.getString("name"), subTheadList);
                    }
                }

                if (CollectionUtils.isNotEmpty(criteriaList)) {
                    Criteria criteria = new Criteria();
                    criteria.orOperator(criteriaList);
                    finalCriteria.add(criteria);
                }
                if (CollectionUtils.isNotEmpty(finalCriteria)) {
                    query.addCriteria(new Criteria().andOperator(finalCriteria));
                    countQuery.addCriteria(new Criteria().andOperator(finalCriteria));
                }

                query.limit(pageVo.getPageSize());
                if (pageVo.getCurrentPage() > 1) {
                    query.skip((long) pageVo.getPageSize() * (pageVo.getCurrentPage() - 1));
                }
                List<JSONObject> resultList = mongoTemplate.find(query, JSONObject.class, collectionVo.getCollection());
                if (StringUtils.isNotEmpty(collectionVo.getDocroot())) {
                    List<JSONObject> finalResultList = new ArrayList<>();
                    for (JSONObject obj : resultList) {
                        JSONArray objList = (JSONArray) JSONPath.read(obj.toJSONString(), "$." + collectionVo.getDocroot());
                        for (int i = 0; i < objList.size(); i++) {
                            finalResultList.add(objList.getJSONObject(i));
                        }
                    }
                    resultList = finalResultList;
                }
                long rowNum = mongoTemplate.count(countQuery, collectionVo.getCollection());
                pageVo.setRowNum((int) rowNum);
                resultObj.put("theadList", theadList);
                resultObj.put("tbodyList", resultList);
                resultObj.put("subTheadData", subsetData);
                resultObj.put("currentPage", pageVo.getCurrentPage());
                resultObj.put("pageSize", pageVo.getPageSize());
                resultObj.put("pageCount", pageVo.getPageCount());
                resultObj.put("rowNum", pageVo.getRowNum());
            }
        }
        return resultObj;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/collection/data/search";
    }
}
