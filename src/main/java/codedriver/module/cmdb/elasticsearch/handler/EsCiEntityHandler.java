package codedriver.module.cmdb.elasticsearch.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
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

@Service
public class EsCiEntityHandler extends ElasticSearchHandlerBase<CiEntityVo, List<CiEntityVo>> {
    Logger logger = LoggerFactory.getLogger(EsCiEntityHandler.class);

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

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
            while (itKey.hasNext()) {
                String key = itKey.next();
                JSONObject dataObj = attrEntityData.getJSONObject(key);
                JSONArray valueList = dataObj.getJSONArray("valueList");
                if (CollectionUtils.isNotEmpty(valueList)) {
                    // 这个值用于匹配like搜索表达式
                    dataObj.put("value", valueList.stream().map(v -> v.toString()).collect(Collectors.joining(",")));
                }
            }
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
        if (CollectionUtils.isNotEmpty(attrFilterList)) {
            for (AttrFilterVo attrFilterVo : attrFilterList) {
                if (CollectionUtils.isNotEmpty(attrFilterVo.getValueList())) {
                    // between需要替换三个参数，暂不支持
                    if (!attrFilterVo.getExpression().equals(Expression.BETWEEN.getExpression())) {
                        if (!attrFilterVo.getExpression().equals(Expression.LIKE.getExpression())
                            && !attrFilterVo.getExpression().equals(Expression.NOTLIKE.getExpression())
                            && !attrFilterVo.getExpression().equals(Expression.ISNULL.getExpression())
                            && !attrFilterVo.getExpression().equals(Expression.ISNOTNULL.getExpression())) {
                            whereList.add("(" + String.format(Expression.getExpressionEs(attrFilterVo.getExpression()),
                                "attrEntityData.attr_" + attrFilterVo.getAttrId() + ".valueList", attrFilterVo
                                    .getValueList().stream().map(v -> "'" + v + "'").collect(Collectors.joining(",")))
                                + ")");
                        } else {
                            whereList.add("(" + String.format(Expression.getExpressionEs(attrFilterVo.getExpression()),
                                "attrEntityData.attr_" + attrFilterVo.getAttrId() + ".value", attrFilterVo
                                    .getValueList().stream().map(v -> "'" + v + "'").collect(Collectors.joining(",")))
                                + ")");
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
        System.out.println(sql);
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
            ciEntityList = ciEntityMapper.searchCiEntityByIdList(ciEntityIdList);

            if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
                List<AttrEntityVo> attrEntityList =
                    attrEntityMapper.searchAttrEntityByCiEntityIdList(ciEntityIdList, ciEntityVo.getAttrIdList());
                List<RelEntityVo> relEntityList =
                    relEntityMapper.searchRelEntityByCiEntityIdList(ciEntityIdList, ciEntityVo.getRelIdList());
                for (CiEntityVo entity : ciEntityList) {
                    Iterator<AttrEntityVo> itAttrEntity = attrEntityList.iterator();
                    while (itAttrEntity.hasNext()) {
                        AttrEntityVo attrEntity = itAttrEntity.next();
                        if (attrEntity.getCiEntityId().equals(entity.getId())) {
                            entity.addAttrEntity(attrEntity);
                            itAttrEntity.remove();
                        }
                    }
                    // 一个关系可能被多个配置项引用，所以不能使用属性的处理方式来处理
                    for (RelEntityVo relEntity : relEntityList) {
                        if (relEntity.getFromCiEntityId().equals(entity.getId())
                            && relEntity.getDirection().equals(RelDirectionType.FROM.getValue())
                            || relEntity.getToCiEntityId().equals(entity.getId())
                                && relEntity.getDirection().equals(RelDirectionType.TO.getValue())) {
                            entity.addRelEntity(relEntity);
                        }
                    }
                }
            }
        }
        return ciEntityList;
    }

    public static void main(String[] argv) {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        System.out.println(list.stream().map(v -> "'" + v + "'").collect(Collectors.joining(",")));
    }

}
