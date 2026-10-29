package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.rel.RelFilterI18nTestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/** 使用 Mapper 替身验证独立保存预设的校验、持久化及权限声明。 */
public class SaveSyncCiCollectionConditionApiTest extends RelFilterI18nTestBase {
    private SaveSyncCiCollectionConditionApi api;
    private SyncCiCollectionVo stored;
    private int writes;
    private UserContext previousUser;

    /** 注入内存映射替身，不启动容器也不修改数据库。 */
    @Before
    public void prepareApi() throws Exception {
        previousUser = UserContext.get();
        UserContext.init((UserContext) null);
        UserContext.get().setUserUuid("preset-user");
        stored = new SyncCiCollectionVo();
        stored.setId(1L);
        api = new SaveSyncCiCollectionConditionApi();
        SyncMapper mapper = (SyncMapper) Proxy.newProxyInstance(SyncMapper.class.getClassLoader(), new Class<?>[]{SyncMapper.class},
                (proxy, method, args) -> {
                    if ("getSyncCiCollectionById".equals(method.getName())) {
                        if (Long.valueOf(1L).equals(args[0])) {
                            return stored;
                        }
                        return null;
                    }
                    if ("updateSyncCiCollectionCondition".equals(method.getName())) {
                        writes++;
                        stored = (SyncCiCollectionVo) args[0];
                        return null;
                    }
                    throw new AssertionError("不应调用其他保存操作：" + method.getName());
                });
        Field field = SaveSyncCiCollectionConditionApi.class.getDeclaredField("syncMapper");
        field.setAccessible(true);
        field.set(api, mapper);
    }

    /** 恢复调用前用户上下文。 */
    @After
    public void restoreUser() {
        UserContext.get().release();
        UserContext.init(previousUser);
    }

    /** 保存、覆盖和主动清空都只更新预设，不产生运行时条件。 */
    @Test
    public void saveOverwriteAndClearPreset() {
        api.myDoService(request(condition("production")));
        Assert.assertEquals("production", stored.getDefaultConditionList().get(0).getValue());
        Assert.assertEquals("preset-user", stored.getLcu());
        Assert.assertNull(stored.getConditionList());
        api.myDoService(request(condition("testing")));
        Assert.assertEquals("testing", stored.getDefaultConditionList().get(0).getValue());
        api.myDoService(request(new JSONArray()));
        Assert.assertTrue(stored.getDefaultConditionList().isEmpty());
        Assert.assertEquals(3, writes);
    }

    /** 映射不存在时，不可写入任何预设。 */
    @Test
    public void missingMappingFailsBeforeWrite() {
        Assert.assertThrows(SyncCiCollectionNotFoundException.class,
                () -> api.myDoService(request(condition("production")).fluentPut("id", 99L)));
        Assert.assertEquals(0, writes);
    }

    /** 嵌套不完整、未知枚举、不兼容表达式以及无效值都必须在写入前失败。 */
    @Test
    public void invalidNestedConditionsCannotBecomeSharedPreset() {
        for (String key : new String[]{"field", "type", "expression", "value"}) {
            JSONArray conditions = condition("production");
            conditions.getJSONObject(0).remove(key);
            Assert.assertThrows(ParamIrregularException.class, () -> api.myDoService(request(conditions)));
        }
        Assert.assertThrows(ParamIrregularException.class, () -> api.myDoService(request(
                new JSONArray().fluentAdd(new JSONObject().fluentPut("field", "count").fluentPut("type", "int")
                        .fluentPut("expression", "in").fluentPut("value", "1")))));
        Assert.assertThrows(ParamIrregularException.class, () -> api.myDoService(request(
                new JSONArray().fluentAdd(new JSONObject().fluentPut("field", "count").fluentPut("type", "int")
                        .fluentPut("expression", "is").fluentPut("value", "not-a-number")))));
        for (String key : new String[]{"type", "expression"}) {
            JSONArray conditions = condition("production");
            conditions.getJSONObject(0).put(key, "unsupported");
            Assert.assertThrows(ParamIrregularException.class, () -> api.myDoService(request(conditions)));
        }
        Assert.assertThrows(ParamIrregularException.class, () -> api.myDoService(request(new JSONArray().fluentAdd(null))));
        Assert.assertThrows(ParamNotExistsException.class, () -> api.myDoService(request(null)));
        Assert.assertEquals(0, writes);
    }

    /** 保留修改采集配置所需的权限声明，真实拦截由框架负责。 */
    @Test
    public void saveRequiresSyncModifyPermission() {
        AuthAction authorization = SaveSyncCiCollectionConditionApi.class.getAnnotation(AuthAction.class);
        Assert.assertNotNull(authorization);
        Assert.assertEquals(SYNC_MODIFY.class, authorization.action());
    }

    /** 统一构造含显式数组的保存请求。 */
    private JSONObject request(JSONArray conditions) {
        return new JSONObject().fluentPut("id", 1L).fluentPut("conditionList", conditions);
    }

    /** 构造前端使用的字符串条件格式。 */
    private JSONArray condition(String value) {
        return new JSONArray().fluentAdd(new JSONObject().fluentPut("field", "environment").fluentPut("type", "string")
                .fluentPut("expression", "is").fluentPut("value", value));
    }
}
