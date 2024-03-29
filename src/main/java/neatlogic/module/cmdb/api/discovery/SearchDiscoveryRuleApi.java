/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.discovery;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
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
public class SearchDiscoveryRuleApi extends PrivateApiComponentBase {
    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public String getName() {
        return "搜索自动发现规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "oidList", type = ApiParamType.JSONARRAY, desc = "oid列表"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数量"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字")})
    @Description(desc = "搜索自动发现规则接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        BasePageVo pageVo = JSONObject.toJavaObject(paramObj, BasePageVo.class);
        JSONObject resultObj = new JSONObject();
        List<Criteria> criteriaList = new ArrayList<>();
        String[] fieldList = new String[]{"sysObjectId", "sysDescrPattern", "_OBJ_CATEGORY", "_OBJ_TYPE", "VENDOR", "MODEL"};
        Pattern pattern = null;
        Query query = new Query();
        Query countQuery = new Query();
        if (StringUtils.isNotBlank(pageVo.getKeyword())) {
            pattern = Pattern.compile("^.*" + pageVo.getKeyword() + ".*$", Pattern.CASE_INSENSITIVE);
            Criteria c = new Criteria();
        }
        if (pattern != null) {
            for (String field : fieldList) {
                criteriaList.add(Criteria.where(field).is(pattern));
            }
        }
        JSONArray oidList = paramObj.getJSONArray("oidList");
        if (CollectionUtils.isNotEmpty(oidList)) {
            criteriaList.add(Criteria.where("sysObjectId").in(oidList));
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            query.addCriteria(new Criteria().orOperator(criteriaList));
            countQuery.addCriteria(new Criteria().orOperator(criteriaList));
        }

        query.limit(pageVo.getPageSize());
        if (pageVo.getCurrentPage() > 1) {
            query.skip((long) pageVo.getPageSize() * (pageVo.getCurrentPage() - 1));
        }
        List<JSONObject> resultList = mongoTemplate.find(query, JSONObject.class, "_discovery_rule");
        long rowNum = mongoTemplate.count(countQuery, "_discovery_rule");
        pageVo.setRowNum((int) rowNum);
        resultObj.put("tbodyList", resultList);
        resultObj.put("currentPage", pageVo.getCurrentPage());
        resultObj.put("pageSize", pageVo.getPageSize());
        resultObj.put("pageCount", pageVo.getPageCount());
        resultObj.put("rowNum", pageVo.getRowNum());
        return resultObj;
    }

    @Override
    public String getToken() {
        return "/cmdb/discovery/rule/search";
    }
}
