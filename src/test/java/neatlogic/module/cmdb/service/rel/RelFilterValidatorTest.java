package neatlogic.module.cmdb.service.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException;
import org.junit.Assert;
import org.junit.Test;

/** 使用最小翻译上下文验证保存条件结构的边界，不依赖租户或数据库。 */
public class RelFilterValidatorTest extends RelFilterI18nTestBase {
    /** 空配置归空，非搜索字段不允许混入持久化配置。 */
    @Test
    public void emptyAndUnsupportedConditions() {
        Assert.assertNull(RelFilterValidator.normalize(null));
        Assert.assertNull(RelFilterValidator.normalize(JSONObject.parseObject("{\"attrFilterList\":[],\"groupId\":null}")));
        invalid("{\"dsl\":\"x\"}");
        invalid("{\"filterCiId\":1.5}");
        invalid("{\"filterCiEntityId\":\"9223372036854775808\"}");
    }

    /** 空值判断无需取值，日期和数字范围必须保留高级搜索的范围字符串。 */
    @Test
    public void nullAndRangeValuesPreserveSearchContract() {
        JSONObject filter = JSONObject.parseObject("{\"attrFilterList\":[{\"attrId\":1,\"expression\":\"between\",\"valueList\":[\"1~10\"]},{\"attrId\":2,\"expression\":\"is-null\"}]}");
        JSONObject result = RelFilterValidator.normalize(filter);
        Assert.assertEquals("1~10", result.getJSONArray("attrFilterList").getJSONObject(0).getJSONArray("valueList").getString(0));
        Assert.assertFalse(result.getJSONArray("attrFilterList").getJSONObject(1).containsKey("valueList"));
        Assert.assertNotSame(filter, result);
    }

    /** 日期范围的嵌套数组可保存，其他属性禁止使用此结构。 */
    @Test
    public void dateRangeArraysAreValidatedByAttributeType() {
        JSONObject result = RelFilterValidator.normalize(JSONObject.parseObject("{\"attrFilterList\":[{\"attrId\":1,\"expression\":\"equal\",\"valueList\":[[\"2026-09-01\",\"2026-09-16\"]]}]}"));
        com.alibaba.fastjson.JSONArray values = result.getJSONArray("attrFilterList").getJSONObject(0).getJSONArray("valueList");
        RelFilterValidator.validateAttributeValues("datetimerange", values);
        Assert.assertEquals("2026-09-16", values.getJSONArray(0).getString(1));
        try {
            RelFilterValidator.validateAttributeValues("text", values);
            Assert.fail("普通文本不能携带范围数组");
        } catch (RelFilterInvalidException expected) {
            Assert.assertNotNull(expected.getMessage());
        }
        invalid("{\"attrFilterList\":[{\"attrId\":1,\"expression\":\"equal\",\"valueList\":[[\"\",\"\"]]}]}");
    }

    /** 不完整条件、未知操作符、错误方向及空范围不得被静默丢弃。 */
    @Test
    public void invalidConditionsAreRejected() {
        invalid("{\"attrFilterList\":[{\"attrId\":1,\"expression\":\"equal\",\"valueList\":[]}]}");
        invalid("{\"attrFilterList\":[{\"attrId\":1,\"expression\":\"bad\"}]}");
        invalid("{\"relFilterList\":[{\"relId\":1,\"direction\":\"invalid\",\"expression\":\"is-null\"}]}");
        invalid("{\"globalAttrFilterList\":[{\"attrId\":1,\"expression\":\"like\",\"valueList\":[\"bad\"]}]}");
    }

    /** 自关联两端使用同一编号但方向独立，长整数编号不发生精度损失。 */
    @Test
    public void directionalAndLongIdsRemainIndependent() {
        JSONObject result = RelFilterValidator.normalize(JSONObject.parseObject("{\"filterCiEntityId\":\"9007199254740993\",\"relFilterList\":[{\"relId\":1,\"direction\":\"from\",\"expression\":\"is-null\"},{\"relId\":1,\"direction\":\"to\",\"expression\":\"like\",\"valueList\":[2,3]}]}"));
        Assert.assertEquals(Long.valueOf(9007199254740993L), result.getLong("filterCiEntityId"));
        Assert.assertEquals(2, result.getJSONArray("relFilterList").size());
    }

    /** 只捕获预期结构错误，避免把其他异常误判为验证通过。 */
    private void invalid(String json) {
        try {
            RelFilterValidator.normalize(JSONObject.parseObject(json));
            Assert.fail("应拒绝无效条件");
        } catch (RelFilterInvalidException expected) {
            Assert.assertNotNull(expected.getMessage());
        }
    }
}
