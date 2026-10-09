package neatlogic.module.cmdb.service.sync;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClientSettings;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityExpiredTimeVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncMappingVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.dao.mapper.ConfigMapper;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.service.cientity.CiEntityServiceImpl;
import neatlogic.module.cmdb.service.rel.RelFilterI18nTestBase;
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

/** 执行真实同步事务生成逻辑，验证过期配置项复用及唯一属性的实际查询值。 */
public class CiSyncExpiredReuseTest extends RelFilterI18nTestBase {
    private final Map<Field, Object> previousDependencies = new LinkedHashMap<>();
    private TenantContext previousTenant;
    private CiSyncManager.SyncHandler handler;
    private SyncCiCollectionVo mapping;
    private CiEntityVo expiredCi;
    private int queryCount;

    /** 测试自行注入模型、查询和 Mongo 字典替身，不访问外部服务。 */
    @Before
    public void setUpSync() throws Exception {
        previousTenant = TenantContext.get();
        TenantContext.init("expired_test");
        replace(ConfigManager.class, "configMapper", proxy(ConfigMapper.class, (method, args) -> null));
        MongoDatabaseFactory factory = proxy(MongoDatabaseFactory.class, (method, args) -> {
            if ("getExceptionTranslator".equals(method)) {
                return new MongoExceptionTranslator();
            }
            if ("getCodecRegistry".equals(method)) {
                return MongoClientSettings.getDefaultCodecRegistry();
            }
            throw new AssertionError("不应访问 Mongo 数据库：" + method);
        });
        MongoTemplate mongo = new MongoTemplate(factory) {
            @Override
            public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
                Assert.assertEquals(CollectionVo.class, entityClass);
                return Collections.emptyList();
            }
        };
        replace(CiSyncManager.class, "mongoTemplate", mongo);
        CiVo ci = new CiVo();
        ci.setId(1L);
        ci.setUniqueAttrIdList(Collections.singletonList(101L));
        replace(CiSyncManager.class, "ciMapper", proxy(CiMapper.class, (method, args) -> ci));
        AttrVo attr = new AttrVo() {
            @Override
            public Boolean isNeedTargetCi() {
                return false;
            }
        };
        attr.setId(101L);
        attr.setCiId(1L);
        attr.setType("text");
        replace(CiSyncManager.class, "attrMapper", proxy(AttrMapper.class, (method, args) -> Collections.singletonList(attr)));
        replace(CiSyncManager.class, "globalAttrMapper", proxy(GlobalAttrMapper.class, (method, args) -> Collections.emptyList()));
        replace(CiSyncManager.class, "relMapper", proxy(RelMapper.class, (method, args) -> Collections.emptyList()));
        expiredCi = new CiEntityVo(1L, 300L);
        expiredCi.setExpiredTime(new Date(0));
        replace(CiSyncManager.class, "ciEntityService", proxy(CiEntityService.class, (method, args) -> {
            Assert.assertEquals("searchCiEntity", method);
            queryCount++;
            CiEntityVo condition = (CiEntityVo) args[0];
            Assert.assertTrue(condition.isIncludeExpired());
            Assert.assertEquals(1, condition.getAttrFilterList().get(0).getValueList().size());
            String value = condition.getAttrFilterList().get(0).getValue();
            if ("host01".equals(value)) {
                return Collections.singletonList(expiredCi);
            }
            Assert.assertEquals("host02", value);
            return Collections.emptyList();
        }));
        SyncMappingVo attrMapping = new SyncMappingVo();
        attrMapping.setAttrId(101L);
        attrMapping.setField("name");
        mapping = new SyncCiCollectionVo();
        mapping.setCiId(1L);
        mapping.setIsAutoCommit(1);
        mapping.setMappingList(Collections.singletonList(attrMapping));
        handler = new CiSyncManager.SyncHandler(new JSONObject(), Collections.singletonList(mapping));
    }

    /** 恢复全局依赖及租户，避免测试污染其他同步用例。 */
    @After
    public void restoreSync() throws Exception {
        for (Map.Entry<Field, Object> entry : previousDependencies.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
        TenantContext.get().release();
        TenantContext.init(previousTenant);
    }

    /** 过期记录仍存在时，生成 UPDATE 并保留旧配置项编号。 */
    @Test
    public void expiredCiIsUpdatedUsingItsOriginalId() throws Exception {
        CiEntityTransactionVo transaction = generate("host01");
        Assert.assertEquals(TransactionActionType.UPDATE.getValue(), transaction.getAction());
        Assert.assertEquals(expiredCi.getId(), transaction.getCiEntityId());
    }

    /** 缓存命中后仍复用同一旧配置项，不再次生成新增配置项。 */
    @Test
    public void cachedExpiredCiKeepsOriginalId() throws Exception {
        generate("host01");
        CiEntityTransactionVo transaction = generate("host01");
        Assert.assertEquals(TransactionActionType.UPDATE.getValue(), transaction.getAction());
        Assert.assertEquals(expiredCi.getId(), transaction.getCiEntityId());
        Assert.assertEquals(1, queryCount);
    }

    /** 数据库确实找不到匹配配置项时，继续沿用原有新增行为。 */
    @Test
    public void missingCiIsStillInserted() throws Exception {
        CiEntityTransactionVo transaction = generate("host02");
        Assert.assertEquals(TransactionActionType.INSERT.getValue(), transaction.getAction());
        Assert.assertNotEquals(expiredCi.getId(), transaction.getCiEntityId());
    }

    /** 旧配置项属性没有变化时也续期，并释放本次保存的编辑锁。 */
    @Test
    public void unchangedExpiredCiIsRenewedAndUnlocked() throws Exception {
        CiEntityTransactionVo transaction = generate("host01");
        transaction.setOldCiEntityVo(expiredCi);
        List<String> writes = new ArrayList<>();
        CiEntityExpiredTimeVo expiry = new CiEntityExpiredTimeVo();
        expiry.setCiEntityId(expiredCi.getId());
        expiry.setExpiredDay(1);
        expiry.setExpiredTime(new Date(0));
        CiEntityMapper mapper = proxy(CiEntityMapper.class, (method, args) -> {
            if ("getCiEntityExpiredTimeById".equals(method)) {
                Assert.assertEquals(expiredCi.getId(), args[0]);
                return expiry;
            }
            if ("updateCiEntityLockById".equals(method)) {
                CiEntityVo ci = (CiEntityVo) args[0];
                writes.add("lock:" + ci.getIsLocked());
            } else if ("updateCiEntityRenewTime".equals(method)) {
                Assert.assertEquals(expiredCi.getId(), args[0]);
                writes.add("renew");
            } else if ("updateCiEntityExpiredTime".equals(method)) {
                Assert.assertSame(expiry, args[0]);
                writes.add("expiry");
            } else {
                throw new AssertionError("不应执行其他配置项写入：" + method);
            }
            return 1;
        });
        CiEntityServiceImpl saveService = new CiEntityServiceImpl() {
            @Override
            public boolean validateCiEntityTransaction(CiEntityTransactionVo record) {
                return false;
            }
        };
        Field field = CiEntityServiceImpl.class.getDeclaredField("ciEntityMapper");
        field.setAccessible(true);
        field.set(saveService, mapper);
        InputFromContext previousInput = InputFromContext.get();
        UserContext previousUser = UserContext.get();
        InputFromContext.init(InputFrom.AUTOEXEC);
        UserContext.init(SystemUser.SYSTEM);
        try {
            Assert.assertEquals(Long.valueOf(0), saveService.saveCiEntity(transaction,
                    new neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo()));
            Assert.assertEquals(Arrays.asList("lock:1", "renew", "expiry", "lock:0"), writes);
            Assert.assertEquals(expiredCi.getId(), transaction.getCiEntityId());
        } finally {
            InputFromContext.get().release();
            if (previousInput != null) {
                InputFromContext.init(previousInput);
            }
            UserContext.get().release();
            if (previousUser != null) {
                UserContext.init(previousUser);
            }
        }
    }

    /** 通过实际私有入口生成同步事务，不在生产代码添加测试装配入口。 */
    private CiEntityTransactionVo generate(String name) throws Exception {
        Method method = CiSyncManager.SyncHandler.class.getDeclaredMethod("generateCiEntityTransaction",
                JSONObject.class, SyncCiCollectionVo.class, Map.class, String.class);
        method.setAccessible(true);
        return (CiEntityTransactionVo) method.invoke(handler, new JSONObject().fluentPut("name", name),
                mapping, new LinkedHashMap<Integer, CiEntityTransactionVo>(), null);
    }

    /** 保存原装配后替换静态依赖，测试结束时逐一恢复。 */
    private void replace(Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        previousDependencies.put(field, field.get(null));
        field.set(null, value);
    }

    /** 创建仅处理预期方法的接口替身。 */
    private <T> T proxy(Class<T> type, Stub stub) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> stub.invoke(method.getName(), args)));
    }

    /** 描述替身的最小调用契约。 */
    private interface Stub {
        Object invoke(String method, Object[] args);
    }
}
