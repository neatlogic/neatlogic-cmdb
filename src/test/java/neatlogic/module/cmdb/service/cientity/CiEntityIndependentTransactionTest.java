package neatlogic.module.cmdb.service.cientity;

import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 使用真实 Spring 事务管理器和内存连接替身验证逐个提交、失败回滚及外层事务隔离。 */
public class CiEntityIndependentTransactionTest {
    private RecordingDataSource dataSource;
    private RecordingService target;
    private CiEntityService service;
    private Field managerField;
    private Object previousManager;
    private Long failedGroupId;

    /** 测试自行注入 Mapper 替身，并保留事务工具的原始装配以便恢复。 */
    @Before
    public void setUp() throws Exception {
        dataSource = new RecordingDataSource();
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        managerField = TransactionUtil.class.getDeclaredField("dataSourceTransactionManager");
        managerField.setAccessible(true);
        previousManager = managerField.get(null);
        new TransactionUtil(manager);
        target = new RecordingService();
        TransactionMapper mapper = (TransactionMapper) Proxy.newProxyInstance(
                TransactionMapper.class.getClassLoader(), new Class<?>[]{TransactionMapper.class},
                (proxy, method, args) -> {
                    if (!"insertTransactionGroup".equals(method.getName())) {
                        throw new AssertionError("不应调用其他 Mapper 方法");
                    }
                    dataSource.write("group:" + args[1]);
                    if (args[1].equals(failedGroupId)) {
                        throw new IllegalStateException("分组写入失败");
                    }
                    return 1;
                });
        Field mapperField = CiEntityServiceImpl.class.getDeclaredField("transactionMapper");
        mapperField.setAccessible(true);
        mapperField.set(target, mapper);
        ProxyFactory factory = new ProxyFactory(target);
        factory.setInterfaces(CiEntityService.class);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        service = (CiEntityService) factory.getProxy();
    }

    /** 避免测试替身污染后续测试的事务工具。 */
    @After
    public void tearDown() throws Exception {
        managerField.set(null, previousManager);
        Assert.assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    /** 第二个配置项失败时，第一个配置项和分组关联已提交，后续配置项不再处理。 */
    @Test
    public void laterFailureKeepsEarlierCommit() {
        target.failedCiId = 2L;
        expectFailure(() -> service.saveCiEntityWithoutTransaction(records(), group()));
        Assert.assertEquals(Arrays.asList("ci:1", "group:1"), dataSource.committed);
        Assert.assertEquals(1, dataSource.commitCount);
        Assert.assertEquals(1, dataSource.rollbackCount);
        Assert.assertEquals(2, target.connections.size());
        Assert.assertNotSame(target.connections.get(0), target.connections.get(1));
    }

    /** 分组关联失败时，同一配置项已经执行的写入也必须回滚。 */
    @Test
    public void groupFailureRollsBackItsCi() {
        failedGroupId = 2L;
        expectFailure(() -> service.saveCiEntityWithoutTransaction(records(), group()));
        Assert.assertEquals(Arrays.asList("ci:1", "group:1"), dataSource.committed);
        Assert.assertEquals(1, dataSource.rollbackCount);
    }

    /** 逐个保存挂起并恢复外层事务，外层回滚不撤销已提交的配置项。 */
    @Test
    public void independentCommitsSurviveOuterRollback() {
        TransactionStatus outer = TransactionUtil.openTx();
        Connection outerConnection = DataSourceUtils.getConnection(dataSource);
        try {
            Long groupId = service.saveCiEntityWithoutTransaction(records(), group());
            Assert.assertEquals(Long.valueOf(100L), groupId);
            Assert.assertSame(outerConnection, DataSourceUtils.getConnection(dataSource));
            for (Connection connection : target.connections) {
                Assert.assertNotSame(outerConnection, connection);
            }
        } finally {
            TransactionUtil.rollbackTx(outer);
        }
        Assert.assertEquals(Arrays.asList("ci:1", "group:1", "ci:2", "group:2", "ci:3", "group:3"), dataSource.committed);
        Assert.assertEquals(3, dataSource.commitCount);
    }

    /** 普通批量事务入口仍然统一回滚，不能被逐个提交逻辑改变。 */
    @Test
    public void ordinaryBatchKeepsAtomicRollback() {
        target.failedCiId = 2L;
        expectFailure(() -> service.saveCiEntity(records(), group()));
        Assert.assertTrue(dataSource.committed.isEmpty());
        Assert.assertEquals(0, dataSource.commitCount);
        Assert.assertEquals(1, dataSource.rollbackCount);
        Assert.assertSame(target.connections.get(0), target.connections.get(1));
    }

    /** 构造固定编号的新增配置项，避免依赖租户和编号生成器。 */
    private List<CiEntityTransactionVo> records() {
        List<CiEntityTransactionVo> records = new ArrayList<>();
        for (long id = 1; id <= 3; id++) {
            CiEntityTransactionVo record = new CiEntityTransactionVo();
            record.setCiEntityId(id);
            record.setAction(TransactionActionType.INSERT.getValue());
            records.add(record);
        }
        return records;
    }

    /** 使用固定分组编号验证返回值。 */
    private TransactionGroupVo group() {
        TransactionGroupVo group = new TransactionGroupVo();
        group.setId(100L);
        return group;
    }

    /** 只接受模拟的保存异常，避免把其他错误当作回滚验证通过。 */
    private void expectFailure(Runnable action) {
        try {
            action.run();
            Assert.fail("应传播当前配置项的保存异常");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().endsWith("失败"));
        }
    }

    /** 仅替换单个配置项的数据库操作，保留生产批量编排及事务边界。 */
    private class RecordingService extends CiEntityServiceImpl {
        private Long failedCiId;
        private final List<Connection> connections = new ArrayList<>();

        @Override
        public Long saveCiEntity(CiEntityTransactionVo record, TransactionGroupVo group) {
            Assert.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            Connection connection = DataSourceUtils.getConnection(dataSource);
            connections.add(connection);
            dataSource.write("ci:" + record.getCiEntityId());
            if (record.getCiEntityId().equals(failedCiId)) {
                throw new IllegalStateException("配置项保存失败");
            }
            return record.getCiEntityId();
        }
    }

    /** 连接仅在 Spring 真正提交时发布暂存写入，回滚时丢弃暂存数据。 */
    private static class RecordingDataSource extends AbstractDataSource {
        private final List<String> committed = new ArrayList<>();
        private int commitCount;
        private int rollbackCount;

        private void write(String value) {
            Connection connection = DataSourceUtils.getConnection(this);
            RecordingConnection state = (RecordingConnection) Proxy.getInvocationHandler(connection);
            Assert.assertFalse(state.autoCommit);
            state.pending.add(value);
        }

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, new RecordingConnection());
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        /** 实现事务管理器需要的连接状态及提交、回滚操作，不访问真实数据库。 */
        private class RecordingConnection implements java.lang.reflect.InvocationHandler {
            private boolean autoCommit = true;
            private boolean closed;
            private int isolation = Connection.TRANSACTION_REPEATABLE_READ;
            private final List<String> pending = new ArrayList<>();

            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                switch (method.getName()) {
                    case "getAutoCommit": return autoCommit;
                    case "setAutoCommit": autoCommit = (boolean) args[0]; return null;
                    case "getTransactionIsolation": return isolation;
                    case "setTransactionIsolation": isolation = (int) args[0]; return null;
                    case "isReadOnly": return false;
                    case "setReadOnly": return null;
                    case "commit": committed.addAll(pending); pending.clear(); commitCount++; return null;
                    case "rollback": pending.clear(); rollbackCount++; return null;
                    case "close": closed = true; return null;
                    case "isClosed": return closed;
                    case "toString": return "测试事务连接";
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    default: throw new AssertionError("未预期的连接方法：" + method.getName());
                }
            }
        }
    }
}
