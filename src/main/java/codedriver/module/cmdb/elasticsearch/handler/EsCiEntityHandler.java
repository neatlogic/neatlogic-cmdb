package codedriver.module.cmdb.elasticsearch.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.QueryResultSet;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.elasticsearch.core.ElasticSearchHandlerBase;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.AttrFilterVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
public class EsCiEntityHandler extends ElasticSearchHandlerBase<CiEntityVo, List<CiEntityVo>> {
    Logger logger = LoggerFactory.getLogger(EsCiEntityHandler.class);

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getDocument() {
        return "cientity";
    }

    @Override
    public JSONObject mySave(Long ciEntityId) {
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityById(ciEntityId);
        if (ciEntityVo != null) {
            List<AttrEntityVo> attrEntityList = attrEntityMapper.getAttrEntityByCiEntityId(ciEntityId);
            ciEntityVo.setAttrEntityList(attrEntityList);
            List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(ciEntityId);
            ciEntityVo.setRelEntityList(relEntityList);
            JSONObject jsonObj = new JSONObject();
            JSONObject attrEntityData = ciEntityVo.getAttrEntityData();
            JSONObject relEntityData = ciEntityVo.getRelEntityData();
            jsonObj.put("id", ciEntityVo.getId());
            jsonObj.put("ciId", ciEntityVo.getCiId());
            Iterator<String> itKey = attrEntityData.keySet().iterator();
            // 集中所有属性内容，以便模糊检索
            String content = "";
            while (itKey.hasNext()) {
                String key = itKey.next();
                JSONObject dataObj = attrEntityData.getJSONObject(key);
                JSONArray valueList = dataObj.getJSONArray("valueList");
                if (CollectionUtils.isNotEmpty(valueList)) {
                    // 这个值用于匹配like搜索表达式
                    String value = valueList.stream().map(v -> v.toString()).collect(Collectors.joining(","));
                    dataObj.put("value", value);
                    if (StringUtils.isNotBlank(content)) {
                        content += ",";
                    }
                    content += value;
                }

            }
            jsonObj.put("content", content);
            jsonObj.put("attrEntityData", attrEntityData);
            jsonObj.put("relEntityData", relEntityData);
            return jsonObj;
        }
        return null;
    }

    @Override
    protected String buildSql(CiEntityVo ciEntityVo) {
        List<AttrFilterVo> attrFilterList = ciEntityVo.getAttrFilterList();

        String column = "id";
        List<String> whereList = new ArrayList<>();
        if (ciEntityVo.getCiId() != null) {
            whereList.add("(" + String.format(Expression.EQUAL.getExpressionEs(), "ciId", ciEntityVo.getCiId()) + ")");
        }
        // 简易搜索
        if (StringUtils.isNotBlank(ciEntityVo.getKeyword())) {
            if (ciEntityVo.getKeyword().contains(" ")) {
                String[] keywords = ciEntityVo.getKeyword().split("\\s+");
                List<String> conditions = new ArrayList<>();
                for (String k : keywords) {
                    conditions.add("content contains '" + k + "'");
                }
                if (CollectionUtils.isNotEmpty(conditions)) {
                    whereList.add("( " + conditions.stream().collect(Collectors.joining(" OR ")) + " )");
                }
            } else {
                whereList.add("( content contains '" + ciEntityVo.getKeyword() + "')");
            }
            // 高级搜索
        } else {
            if (CollectionUtils.isNotEmpty(attrFilterList)) {
                for (AttrFilterVo attrFilterVo : attrFilterList) {
                    if (CollectionUtils.isNotEmpty(attrFilterVo.getValueList())) {
                        // between需要替换三个参数，暂不支持
                        if (!attrFilterVo.getExpression().equals(Expression.BETWEEN.getExpression())) {
                            if (!attrFilterVo.getExpression().equals(Expression.LIKE.getExpression())
                                && !attrFilterVo.getExpression().equals(Expression.NOTLIKE.getExpression())
                                && !attrFilterVo.getExpression().equals(Expression.ISNULL.getExpression())
                                && !attrFilterVo.getExpression().equals(Expression.ISNOTNULL.getExpression())) {
                                whereList
                                    .add("(" + String.format(Expression.getExpressionEs(attrFilterVo.getExpression()),
                                        "attrEntityData.attr_" + attrFilterVo.getAttrId() + ".valueList",
                                        attrFilterVo.getValueList().stream().map(v -> "'" + v + "'")
                                            .collect(Collectors.joining(",")))
                                        + ")");
                            } else {
                                whereList
                                    .add("(" + String.format(Expression.getExpressionEs(attrFilterVo.getExpression()),
                                        "attrEntityData.attr_" + attrFilterVo.getAttrId() + ".value",
                                        attrFilterVo.getValueList().stream().map(v -> "'" + v + "'")
                                            .collect(Collectors.joining(",")))
                                        + ")");
                            }
                        }
                    }
                }
            }
        }
        String where = "";
        if (CollectionUtils.isNotEmpty(whereList)) {
            where = "where " + whereList.stream().collect(Collectors.joining(" and "));
        }
        String orderBy = "order by id desc";
        String sql = String.format("select %s from %s %s %s limit %d,%d", column, TenantContext.get().getTenantUuid(),
            where, orderBy, ciEntityVo.getStartNum(), ciEntityVo.getPageSize());
        if (logger.isDebugEnabled()) {
            logger.debug("ElasticSearchSql:" + sql);
        }
        return sql;
    }

    @Override
    protected List<CiEntityVo> makeupQueryResult(CiEntityVo ciEntityVo, QueryResult result) {
        List<MultiAttrsObject> dataList = result.getData();
        List<Long> ciEntityIdList = new ArrayList<>();
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataList)) {
            for (MultiAttrsObject attrObj : dataList) {
                ciEntityIdList.add(Long.parseLong(attrObj.getId()));
            }
        }
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            if (ciEntityVo.getNeedPage()) {
                ciEntityVo.setRowNum(result.getTotal());
                ciEntityVo.setPageCount(PageUtil.getPageCount(result.getTotal(), ciEntityVo.getPageSize()));
            }
            ciEntityList = ciEntityService.searchCiEntityByIds(ciEntityIdList, ciEntityVo);
        }
        return ciEntityList;
    }

    @Override
    protected List<CiEntityVo> makeupQueryIterateResult(CiEntityVo t, QueryResultSet result) {
        // TODO Auto-generated method stub
        return null;
    }

}
