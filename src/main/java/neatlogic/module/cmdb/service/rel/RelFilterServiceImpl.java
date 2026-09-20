package neatlogic.module.cmdb.service.rel;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException.Reason;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** 复用模型元数据校验过滤条件，不调用搜索接口，避免失效条件被搜索逻辑忽略。 */
@Service
public class RelFilterServiceImpl implements RelFilterService {
    @Resource
    private AttrMapper attrMapper;
    @Resource
    private GlobalAttrMapper globalAttrMapper;
    @Resource
    private RelMapper relMapper;
    @Resource
    private CiMapper ciMapper;
    @Resource
    private GroupMapper groupMapper;

    /** 先校验结构，再校验字段和模型归属；不执行配置项搜索。 */
    @Override
    public JSONObject validate(Long ciId, JSONObject filter, String parameter) {
        try {
            JSONObject normalized = RelFilterValidator.normalize(filter);
            if (normalized == null) {
                return null;
            }
            CiVo ci = ciMapper.getCiById(ciId);
            if (ci == null) {
                throw new RelFilterInvalidException(Reason.MODEL_MISSING);
            }
            validateAttributes(ciId, normalized);
            validateGlobals(ciId, normalized);
            validateRelations(ciId, normalized);
            Long filterCiId = normalized.getLong("filterCiId");
            if (filterCiId != null && !ciMapper.getDownwardCiIdListByLR(ci.getLft(), ci.getRht()).contains(filterCiId)) {
                throw new RelFilterInvalidException(Reason.CHILD_MODEL_INVALID);
            }
            Long groupId = normalized.getLong("groupId");
            if (groupId != null && groupMapper.getActiveGroupByCiId(ciId).stream().noneMatch(group -> Objects.equals(group.getId(), groupId))) {
                throw new RelFilterInvalidException(Reason.GROUP_INVALID);
            }
            return normalized;
        } catch (RelFilterInvalidException e) {
            throw new RelFilterInvalidException(parameter, e);
        }
    }

    /** 继承属性以当前模型的实际可用属性列表为准，操作符以属性处理器契约为准。 */
    private void validateAttributes(Long ciId, JSONObject filter) {
        JSONArray conditions = filter.getJSONArray("attrFilterList");
        if (conditions == null) {
            return;
        }
        Map<Long, AttrVo> attributes = new HashMap<>();
        for (AttrVo attr : attrMapper.getAttrByCiId(ciId)) {
            attributes.put(attr.getId(), attr);
        }
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            AttrVo attr = attributes.get(condition.getLong("attrId"));
            if (attr == null) {
                throw new RelFilterInvalidException(Reason.ATTRIBUTE_INVALID);
            }
            IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attr.getType());
            if (!Integer.valueOf(1).equals(attr.getIsSearchAble()) || handler == null || !handler.isCanSearch() || Arrays.stream(handler.getSupportExpression())
                    .noneMatch(expression -> expression.getExpression().equals(condition.getString("expression")))) {
                throw new RelFilterInvalidException(Reason.ATTRIBUTE_OPERATOR_INVALID);
            }
            RelFilterValidator.validateAttributeValues(attr.getType(), condition.getJSONArray("valueList"));
        }
    }

    /** 全局属性仅允许当前模型的有效属性及其搜索操作符。 */
    private void validateGlobals(Long ciId, JSONObject filter) {
        JSONArray conditions = filter.getJSONArray("globalAttrFilterList");
        if (conditions == null) {
            return;
        }
        Map<Long, GlobalAttrVo> attributes = new HashMap<>();
        for (GlobalAttrVo attr : globalAttrMapper.getGlobalAttrByCiId(ciId)) {
            attributes.put(attr.getId(), attr);
        }
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            GlobalAttrVo attr = attributes.get(condition.getLong("attrId"));
            if (attr == null || !Integer.valueOf(1).equals(attr.getIsActive())) {
                throw new RelFilterInvalidException(Reason.GLOBAL_ATTRIBUTE_INVALID);
            }
            validateCollectionExpression(condition);
        }
    }

    /** 关系需要同时匹配编号和方向，从而正确处理继承和自关联。 */
    private void validateRelations(Long ciId, JSONObject filter) {
        JSONArray conditions = filter.getJSONArray("relFilterList");
        if (conditions == null) {
            return;
        }
        Map<String, RelVo> relations = new HashMap<>();
        for (RelVo rel : relMapper.getRelByCiId(ciId)) {
            relations.put(rel.getId() + ":" + rel.getDirection(), rel);
        }
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            if (!relations.containsKey(condition.getLong("relId") + ":" + condition.getString("direction"))) {
                throw new RelFilterInvalidException(Reason.RELATION_INVALID);
            }
            validateCollectionExpression(condition);
        }
    }

    /** 关系和全局属性均复用既有集合搜索操作符。 */
    private void validateCollectionExpression(JSONObject condition) {
        String expression = condition.getString("expression");
        if (!Arrays.asList(SearchExpression.LI.getExpression(), SearchExpression.NL.getExpression(),
                SearchExpression.NULL.getExpression(), SearchExpression.NOTNULL.getExpression()).contains(expression)) {
            throw new RelFilterInvalidException(Reason.COLLECTION_OPERATOR_INVALID);
        }
    }
}
