package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/** 验证真实 API 注解校验链不会吞掉显式清空条件。 */
public class SaveRelFilterInputTest {
    /** 框架移除可选 null 后，关系 API 只恢复过滤字段并保留缺省差异。 */
    @Test
    public void frameworkValidationPreservesExplicitClear() throws Exception {
        SaveRelApi api = new SaveRelApi();
        Method method = SaveRelApi.class.getMethod("myDoService", JSONObject.class);
        JSONObject input = JSONObject.parseObject("{\"typeId\":1,\"fromCiId\":10,\"toCiId\":20,\"fromLabel\":\"a\",\"toLabel\":\"b\",\"fromRule\":\"N\",\"toRule\":\"N\",\"fromIsUnique\":0,\"toIsUnique\":0,\"fromIsRequired\":0,\"toIsRequired\":0,\"fromIsCascadeDelete\":0,\"toIsCascadeDelete\":0,\"fromFilter\":null,\"fromGroupId\":null}");
        api.validInput(method, input);
        Assert.assertTrue(input.containsKey("fromFilter"));
        Assert.assertNull(input.get("fromFilter"));
        Assert.assertFalse(input.containsKey("toFilter"));
        Assert.assertFalse(input.containsKey("fromGroupId"));
        RelVo rel = JSON.toJavaObject(input, RelVo.class);
        // 保存入口显式调用 setter，保证 FastJSON 不同版本下的 null 都是清空。
        if (input.containsKey("fromFilter")) {
            rel.setFromFilter(input.getJSONObject("fromFilter"));
        }
        Assert.assertTrue(rel.isFromFilterSpecified());
        Assert.assertFalse(rel.isToFilterSpecified());
    }
}
