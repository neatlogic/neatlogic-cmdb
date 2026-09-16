package neatlogic.module.cmdb.service.rel;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException.Reason;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 过滤条件结构校验，保留高级搜索原始值格式，错误通过框架异常国际化。 */
final class RelFilterValidator {
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList("attrFilterList", "globalAttrFilterList",
            "relFilterList", "groupId", "filterCiEntityId", "filterCiId"));

    private RelFilterValidator() {
    }

    /** 仅保存搜索条件白名单，拒绝不完整条件，去除空列表和空标量。 */
    static JSONObject normalize(JSONObject filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        JSONObject result = new JSONObject();
        for (String key : filter.keySet()) {
            if (!KEYS.contains(key)) {
                throw new RelFilterInvalidException(Reason.UNSUPPORTED_FIELD, key);
            }
            Object value = filter.get(key);
            if (value == null) {
                continue;
            }
            if (key.endsWith("FilterList")) {
                if (!(value instanceof JSONArray)) {
                    throw new RelFilterInvalidException(Reason.ARRAY_REQUIRED, key);
                }
                JSONArray normalized = new JSONArray();
                for (Object item : (JSONArray) value) {
                    if (!(item instanceof JSONObject)) {
                        throw new RelFilterInvalidException(Reason.INVALID_CONDITION, key);
                    }
                    normalized.add(normalizeCondition(key, (JSONObject) item));
                }
                if (!normalized.isEmpty()) {
                    result.put(key, normalized);
                }
            } else if (!(value instanceof String) || StringUtils.isNotBlank((String) value)) {
                result.put(key, positiveId(value, key));
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    /** 空值操作符不需要值，其他操作符必须携带值；范围字符串由原属性搜索器解释。 */
    private static JSONObject normalizeCondition(String type, JSONObject condition) {
        boolean relation = "relFilterList".equals(type);
        String idKey = "attrId";
        if (relation) {
            idKey = "relId";
        }
        Set<String> keys = new HashSet<>(Arrays.asList(idKey, "expression", "valueList"));
        if (relation) {
            keys.add("direction");
        }
        if (!keys.containsAll(condition.keySet())) {
            throw new RelFilterInvalidException(Reason.UNSUPPORTED_CONDITION_FIELD);
        }
        JSONObject result = new JSONObject();
        result.put(idKey, positiveId(condition.get(idKey), idKey));
        String expression = condition.getString("expression");
        if (!SearchExpression.checkExpressionIsExists(expression)) {
            throw new RelFilterInvalidException(Reason.OPERATOR_INVALID);
        }
        result.put("expression", expression);
        if (relation) {
            String direction = condition.getString("direction");
            if (!"from".equals(direction) && !"to".equals(direction)) {
                throw new RelFilterInvalidException(Reason.DIRECTION_INVALID);
            }
            result.put("direction", direction);
        }
        if (SearchExpression.NULL.getExpression().equals(expression) || SearchExpression.NOTNULL.getExpression().equals(expression)) {
            return result;
        }
        Object value = condition.get("valueList");
        if (!(value instanceof JSONArray) || ((JSONArray) value).isEmpty()) {
            throw new RelFilterInvalidException(Reason.VALUE_REQUIRED);
        }
        JSONArray values = new JSONArray();
        for (Object item : (JSONArray) value) {
            // 日期范围搜索器原样返回二元数组，具体属性类型在元数据校验阶段确认。
            if ("attrFilterList".equals(type) && item instanceof JSONArray) {
                JSONArray range = (JSONArray) item;
                if (range.size() != 2 || range.stream().anyMatch(bound -> !(bound instanceof String) || StringUtils.isBlank((String) bound))) {
                    throw new RelFilterInvalidException(Reason.RANGE_BOUNDS_REQUIRED);
                }
                values.add(new JSONArray(new java.util.ArrayList<>(range)));
                continue;
            }
            if (!(item instanceof String) && !(item instanceof Number) && !(item instanceof Boolean)) {
                throw new RelFilterInvalidException(Reason.VALUE_INVALID);
            }
            if (StringUtils.isBlank(String.valueOf(item))) {
                throw new RelFilterInvalidException(Reason.VALUE_EMPTY);
            }
            if (relation || "globalAttrFilterList".equals(type)) {
                values.add(positiveId(item, "valueList"));
            } else {
                values.add(item);
            }
        }
        result.put("valueList", values);
        return result;
    }

    /** 根据属性类型验证范围值，文本波浪号保留为合法字面值。 */
    static void validateAttributeValues(String type, JSONArray values) {
        if (values == null) {
            return;
        }
        for (Object value : values) {
            if (value instanceof JSONArray && !"datetimerange".equals(type)) {
                throw new RelFilterInvalidException(Reason.RANGE_ARRAY_UNSUPPORTED);
            }
            if (Arrays.asList("number", "date", "datetime", "time", "datetimerange").contains(type) && "~".equals(value)) {
                throw new RelFilterInvalidException(Reason.RANGE_BOUND_REQUIRED);
            }
        }
    }

    /** 按十进制整数验证编号，防止浮点数被转换时截断。 */
    private static Long positiveId(Object value, String field) {
        if (value == null || !String.valueOf(value).matches("[0-9]+")) {
            throw new RelFilterInvalidException(Reason.ID_INVALID, field);
        }
        try {
            long id = Long.parseLong(String.valueOf(value));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // 超出 Long 范围时统一返回字段校验错误。
        }
        throw new RelFilterInvalidException(Reason.ID_INVALID, field);
    }
}
