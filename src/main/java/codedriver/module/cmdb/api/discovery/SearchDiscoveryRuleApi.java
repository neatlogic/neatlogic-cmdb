/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.discovery;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
