package neatlogic.module.cmdb.service.sync;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClientSettings;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.sync.*;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.dao.mapper.ConfigMapper;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.module.cmdb.api.sync.LaunchSyncCollectionApi;
import neatlogic.module.cmdb.dao.mapper.sync.ObjectMapper;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import neatlogic.module.cmdb.service.rel.RelFilterI18nTestBase;
import org.bson.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/** 验证预设选择、条件快照和实际 Mongo 查询，不访问外部服务。 */
public class SyncPresetConditionTest extends RelFilterI18nTestBase {
    private final Map<Field, Object> dependencies = new LinkedHashMap<>();
    private TenantContext previousTenant;
    private CollectionVo collection;

    /** 安装仅返回字典数据的替身，并保留全局依赖以便清理。 */
    @Before
    public void initializeSync() throws Exception {
        previousTenant = TenantContext.get();
        TenantContext.init("preset_test");
        replace(ConfigManager.class, "configMapper", Proxy.newProxyInstance(ConfigMapper.class.getClassLoader(),
                new Class<?>[]{ConfigMapper.class}, (proxy, method, args) -> null));
        collection = new CollectionVo();
        collection.setName("server");
        collection.setCollection("server_data");
        collection.setFilter(new JSONObject().fluentPut("source", "discovery"));
        MongoDatabaseFactory factory = (MongoDatabaseFactory) Proxy.newProxyInstance(MongoDatabaseFactory.class.getClassLoader(),
                new Class<?>[]{MongoDatabaseFactory.class}, (proxy, method, args) -> {
                    if ("getExceptionTranslator".equals(method.getName())) {
                        return new MongoExceptionTranslator();
                    }
                    if ("getCodecRegistry".equals(method.getName())) {
                        return MongoClientSettings.getDefaultCodecRegistry();
                    }
                    throw new AssertionError("不应连接 Mongo：" + method.getName());
                });
        replace(CiSyncManager.class, "mongoTemplate", new MongoTemplate(factory) {
            @Override
            public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
                Assert.assertEquals("_dictionary", collectionName);
                return Collections.singletonList(entityClass.cast(collection));
            }
        });
    }

    /** 恢复静态依赖及租户上下文，避免污染其他测试。 */
    @After
    public void restoreSync() throws Exception {
        for (Map.Entry<Field, Object> entry : dependencies.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
        TenantContext.get().release();
        TenantContext.init(previousTenant);
    }

    /** 数据库序列化往返后，预设与执行条件仍然分离，清空不能读回旧条件。 */
    @Test
    public void presetRoundTripAndClearRemainSeparateFromRuntime() {
        SyncCiCollectionVo mapping = mapping("production");
        String serialized = mapping.getDefaultConditionStr();
        SyncCiCollectionVo loaded = new SyncCiCollectionVo();
        loaded.setDefaultConditionStr(serialized);
        Assert.assertEquals("production", loaded.getDefaultConditionList().get(0).getValue());
        Assert.assertNull(loaded.getConditionList());
        loaded.setDefaultConditionList(Collections.emptyList());
        SyncCiCollectionVo cleared = new SyncCiCollectionVo();
        cleared.setDefaultConditionStr(loaded.getDefaultConditionStr());
        Assert.assertTrue(cleared.getDefaultConditionList() == null || cleared.getDefaultConditionList().isEmpty());
    }

    /** 损坏预设不能被当作空条件执行，以免扩大同步范围。 */
    @Test
    public void malformedStoredPresetFailsClosed() {
        SyncCiCollectionVo mapping = new SyncCiCollectionVo();
        mapping.setDefaultConditionStr("not-json");
        Assert.assertThrows(ParamIrregularException.class, mapping::getDefaultConditionList);
    }

    /** 缺省及显式 null 使用预设，非空数组只替换本次，空数组取消本次过滤。 */
    @Test
    public void singleMappingUsesPresetOrExplicitOverride() throws Exception {
        for (int i = 0; i < 2; i++) {
            JSONObject request = new JSONObject();
            if (i == 1) {
                request.put("conditionList", null);
            }
            SyncCiCollectionVo mapping = mapping("production");
            apply(Collections.singletonList(mapping), request.getJSONArray("conditionList"));
            Assert.assertEquals("production", mapping.getConditionList().get(0).getValue());
        }
        SyncCiCollectionVo mapping = mapping("production");
        apply(Collections.singletonList(mapping), JSON.parseArray(JSON.toJSONString(conditions("testing"))));
        Assert.assertEquals("testing", mapping.getConditionList().get(0).getValue());
        Assert.assertEquals("production", mapping.getDefaultConditionList().get(0).getValue());
        apply(Collections.singletonList(mapping), new JSONArray());
        Assert.assertTrue(mapping.getConditionList().isEmpty());
        Assert.assertEquals("production", mapping.getDefaultConditionList().get(0).getValue());
    }

    /** 多条映射分别使用自己的预设，不能把单条临时条件扩散到整批。 */
    @Test
    public void multipleMappingsUseTheirOwnPresets() throws Exception {
        SyncCiCollectionVo first = mapping("production");
        SyncCiCollectionVo second = mapping("testing");
        apply(Arrays.asList(first, second), JSON.parseArray(JSON.toJSONString(conditions("ignored"))));
        Assert.assertEquals("production", first.getConditionList().get(0).getValue());
        Assert.assertEquals("testing", second.getConditionList().get(0).getValue());
    }

    /** 五种选择入口均在选中映射后加载预设；被动映射阻止测试触发异步同步。 */
    @Test
    public void allLaunchSelectorsApplyPresetToSelectedMappings() throws Exception {
        List<JSONObject> requests = Arrays.asList(
                new JSONObject().fluentPut("id", 1L),
                new JSONObject().fluentPut("idList", new JSONArray().fluentAdd(1L)),
                new JSONObject().fluentPut("collectionList", new JSONArray().fluentAdd("server")),
                new JSONObject().fluentPut("collectionObjList", new JSONArray().fluentAdd(
                        new JSONObject().fluentPut("category", "hardware").fluentPut("type", "server"))),
                new JSONObject().fluentPut("isAll", 1));
        for (JSONObject request : requests) {
            SyncCiCollectionVo selected = mapping("production");
            selected.setCollectMode("passive");
            LaunchSyncCollectionApi api = new LaunchSyncCollectionApi();
            SyncMapper mapper = (SyncMapper) Proxy.newProxyInstance(SyncMapper.class.getClassLoader(), new Class<?>[]{SyncMapper.class},
                    (proxy, method, args) -> {
                        if ("searchSyncCiCollection".equals(method.getName())) {
                            if (((SyncCiCollectionVo) args[0]).getCurrentPage() == 1) {
                                return Collections.singletonList(selected);
                            }
                            return Collections.emptyList();
                        }
                        if ("getSyncCiCollectionByIdList".equals(method.getName())
                                || "getInitiativeSyncCiCollectionByCollectNameList".equals(method.getName())
                                || "getInitiativeSyncCiCollectionByCollectNameAndCiId".equals(method.getName())) {
                            return Collections.singletonList(selected);
                        }
                        throw new AssertionError("不应读取其他映射：" + method.getName());
                    });
            Field syncField = LaunchSyncCollectionApi.class.getDeclaredField("syncMapper");
            syncField.setAccessible(true);
            syncField.set(api, mapper);
            ObjectMapper objectMapper = (ObjectMapper) Proxy.newProxyInstance(ObjectMapper.class.getClassLoader(),
                    new Class<?>[]{ObjectMapper.class}, (proxy, method, args) -> {
                        Assert.assertEquals("getObjectByCategoryAndType", method.getName());
                        ObjectVo object = new ObjectVo();
                        object.setCiId(11L);
                        return object;
                    });
            Field objectField = LaunchSyncCollectionApi.class.getDeclaredField("objectMapper");
            objectField.setAccessible(true);
            objectField.set(api, objectMapper);
            api.myDoService(request);
            Assert.assertEquals(request.toJSONString(), "production", selected.getConditionList().get(0).getValue());
        }
    }

    /** 快照恢复使用启动时条件，包含显式空条件；旧审计不会套用新预设。 */
    @Test
    public void resumeRestoresLaunchSnapshotInsteadOfCurrentPreset() throws Exception {
        SyncCiCollectionVo launched = mapping("production");
        apply(Collections.singletonList(launched), null);
        JSONObject snapshot = snapshot(launched, "batch-a", 1000L);
        Assert.assertEquals("batch-a", snapshot.getString("batchTag"));
        Assert.assertEquals(Long.valueOf(1000L), snapshot.getLong("startTime"));
        SyncCiCollectionVo resumed = mapping("changed-after-pause");
        restore(resumed, JSON.parseObject(snapshot.toJSONString()));
        Assert.assertEquals("production", resumed.getConditionList().get(0).getValue());
        launched.setConditionList(Collections.emptyList());
        restore(resumed, snapshot(launched, null, null));
        Assert.assertTrue(resumed.getConditionList().isEmpty());
        SyncCiCollectionVo historical = mapping("current-preset");
        restore(historical, new JSONObject().fluentPut("batchTag", "old"));
        Assert.assertNull(historical.getConditionList());
    }

    /** 实际查询同时保留字典条件、最终执行条件、批次、时间和恢复游标。 */
    @Test
    public void actualMongoQueryCombinesAllExecutionConstraints() throws Exception {
        SyncCiCollectionVo mapping = mapping("production");
        apply(Collections.singletonList(mapping), null);
        SyncAuditVo audit = new SyncAuditVo();
        audit.setCurrentDataId("507f1f77bcf86cd799439011");
        mapping.setSyncAudit(audit);
        CiSyncManager.SyncHandler handler = new CiSyncManager.SyncHandler(Collections.singletonList(mapping), "batch-a", 1000L);
        Method method = CiSyncManager.SyncHandler.class.getDeclaredMethod("buildQuery", SyncCiCollectionVo.class);
        method.setAccessible(true);
        Query query = (Query) method.invoke(handler, mapping);
        Document actual = query.getQueryObject();
        String json = actual.toJson();
        Assert.assertTrue(json, json.contains("source") && json.contains("discovery"));
        Assert.assertTrue(json, json.contains("environment") && json.contains("production"));
        Assert.assertTrue(json, json.contains("_batch_tag") && json.contains("batch-a"));
        Assert.assertTrue(json, json.contains("_renewtime") && json.contains("$gte"));
        Assert.assertTrue(json, json.contains("_id") && json.contains("$gt"));
        Assert.assertTrue(actual.containsKey("$and"));
    }

    /** 定时等直接创建运行对象的入口不会仅因读取预设而自动增加过滤。 */
    @Test
    public void presetAloneDoesNotFilterScheduledExecution() throws Exception {
        SyncCiCollectionVo mapping = mapping("production");
        mapping.setSyncAudit(new SyncAuditVo());
        CiSyncManager.SyncHandler handler = new CiSyncManager.SyncHandler(Collections.singletonList(mapping), null, null);
        Method method = CiSyncManager.SyncHandler.class.getDeclaredMethod("buildQuery", SyncCiCollectionVo.class);
        method.setAccessible(true);
        String query = ((Query) method.invoke(handler, mapping)).getQueryObject().toJson();
        Assert.assertFalse(query, query.contains("environment"));
        Assert.assertTrue(query, query.contains("source"));
    }

    /** 通过真实入口内部选择器验证条件优先级。 */
    private void apply(List<SyncCiCollectionVo> mappings, JSONArray conditions) throws Exception {
        Method method = LaunchSyncCollectionApi.class.getDeclaredMethod("applyConditionList", List.class, JSONArray.class);
        method.setAccessible(true);
        method.invoke(new LaunchSyncCollectionApi(), mappings, conditions);
    }

    /** 调用生产快照构造方法，避免在测试内重复序列化逻辑。 */
    private JSONObject snapshot(SyncCiCollectionVo mapping, String tag, Long time) throws Exception {
        Method method = CiSyncManager.class.getDeclaredMethod("buildSyncConfig", SyncCiCollectionVo.class, String.class, Long.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(null, mapping, tag, time);
    }

    /** 调用恢复入口共用的快照还原方法。 */
    private void restore(SyncCiCollectionVo mapping, JSONObject config) throws Exception {
        Method method = CiSyncManager.class.getDeclaredMethod("restoreSyncCondition", SyncCiCollectionVo.class, JSONObject.class);
        method.setAccessible(true);
        method.invoke(null, mapping, config);
    }

    /** 构造包含独立预设、尚未解析执行条件的映射。 */
    private SyncCiCollectionVo mapping(String value) {
        SyncCiCollectionVo mapping = new SyncCiCollectionVo();
        mapping.setId(1L);
        mapping.setCollectionName("server");
        mapping.setDefaultConditionList(conditions(value));
        return mapping;
    }

    /** 构造可直接生成 Mongo 查询的字符串条件。 */
    private List<SyncConditionVo> conditions(String value) {
        SyncConditionVo condition = new SyncConditionVo();
        condition.setField("environment");
        condition.setType("string");
        condition.setExpression("is");
        condition.setValue(value);
        return Collections.singletonList(condition);
    }

    /** 记录并替换静态依赖，测试结束后统一还原。 */
    private void replace(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        dependencies.put(field, field.get(null));
        field.set(null, value);
    }
}
