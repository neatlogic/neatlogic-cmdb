package neatlogic.module.cmdb.mq.subscribe;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityStatusVo;
import neatlogic.framework.mq.dto.SubscribeVo;
import neatlogic.module.cmdb.dao.mapper.cientity.AlertLevelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import neatlogic.module.cmdb.service.cientity.InformantAlertService;
import neatlogic.module.cmdb.service.cientity.InformantAlertServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 使用真实消息解析、Spring 事务、MyBatis XML 和隔离 H2 验证消费闭环。 */
public class InformantAlertSubscribeTest {
    public JdbcTemplate jdbc;
    public CiEntityAlertMapper mapper;
    public InformantAlertSubscribe handler;
    private final SubscribeVo subscription = new SubscribeVo();

    /** 初始化隔离数据库，并在测试侧装配真实服务事务代理。 */
    @Before
    public void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table cmdb_cientity_alert (id bigint primary key, cientity_id bigint, cientity_uuid char(32), ip varchar(50), level int, alert_time timestamp(3), metric_name varchar(255), metric_value varchar(255), alert_message varchar(500), alert_link varchar(255))");
        // 只在隔离 H2 执行实际迁移脚本，同时验证旧表升级而非测试自造新表。
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("neatlogic/resources/cmdb/changelog/2026-11-03/neatlogic_tenant.sql")) {
            String migration = org.apache.commons.io.IOUtils.toString(in, java.nio.charset.StandardCharsets.UTF_8);
            for (String statement : migration.split(";")) {
                if (!statement.trim().isEmpty()) {
                    jdbc.execute(statement);
                }
            }
        }
        jdbc.execute("create table cmdb_cientity_alertlevel (id bigint primary key, level int, name varchar(50), label varchar(50), type varchar(50), color varchar(50))");
        jdbc.execute("create table cmdb_cientity (id bigint primary key, ci_id bigint, name varchar(50))");
        jdbc.execute("insert into cmdb_cientity values (11,1,'应用实例'), (12,1,'数据库实例')");
        jdbc.execute("insert into cmdb_cientity_alertlevel values (1,7,'critical','严重','informant','#ff0000'), (2,8,'critical','巡检严重','inspect','#ff00ff')");
        Configuration config = new Configuration(new Environment("test", new SpringManagedTransactionFactory(), ds));
        for (String name : new String[]{"CiEntityAlertMapper", "AlertLevelMapper"}) {
            String resource = "neatlogic/module/cmdb/dao/mapper/cientity/" + name + ".xml";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
            }
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
        SqlSessionTemplate session = new SqlSessionTemplate(factory);
        mapper = session.getMapper(CiEntityAlertMapper.class);
        InformantAlertServiceImpl target = new InformantAlertServiceImpl();
        inject(target, "ciEntityAlertMapper", mapper);
        inject(target, "alertLevelMapper", session.getMapper(AlertLevelMapper.class));
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setInterfaces(InformantAlertService.class);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds), new AnnotationTransactionAttributeSource()));
        handler = new InformantAlertSubscribe();
        inject(handler, "informantAlertService", proxy.getProxy());
        subscription.setName("CMDB测试订阅");
        TenantContext.init("test-tenant");
    }

    /** 清理测试线程上下文。 */
    @After
    public void tearDown() {
        TenantContext.get().release();
    }

    /** 直接调用真实处理逻辑，供隔离 Kafka 联调复用。 */
    public void consume(Object message) {
        handler.myOnMessage(subscription, message);
    }

    /** 重复异常仅更新、改绑沿用 UUID、另一告警独立、恢复不依赖配置项。 */
    @Test
    public void lifecycleIsIdempotentAndRecoveryIsPrecise() {
        JSONObject first = message("a", "Critical", 11L);
        consume(first.toJSONString());
        consume(first.toJSONString());
        Assert.assertEquals(1, count());
        Assert.assertEquals(Integer.valueOf(7), jdbc.queryForObject("select level from cmdb_cientity_alert", Integer.class));
        first.put("cientity_id", 12L);
        consume(first.toJSONString());
        Assert.assertEquals(Long.valueOf(12), jdbc.queryForObject("select cientity_id from cmdb_cientity_alert", Long.class));
        consume(message("b", "Critical", 12L).toJSONString());
        consume(message("a", "Healthy", null).toJSONString());
        Assert.assertEquals(1, count());
        Assert.assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", jdbc.queryForObject("select alert_uuid from cmdb_cientity_alert", String.class));
    }

    /** 未映射级别为活动级别 0，查询、状态投影均返回来源与不明级别显示。 */
    @Test
    public void unknownLevelRemainsActiveAndIsVisible() {
        consume(message("a", "Warn", 11L).toJSONString());
        Assert.assertEquals(Integer.valueOf(0), jdbc.queryForObject("select level from cmdb_cientity_alert", Integer.class));
        List<CiEntityStatusVo> status = mapper.listCiEntityStatus(Collections.singletonList(11L));
        CiEntityAlertVo alert = status.get(0).getAlertList().get(0);
        Assert.assertEquals("informant", alert.getSource());
        Assert.assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", alert.getAlertUuid());
        Assert.assertNotNull(alert.getLevelName());
        Assert.assertNotNull(alert.getLevelColor());
        CiEntityAlertVo search = new CiEntityAlertVo();
        Assert.assertEquals(1, mapper.searchCiEntityAlertCount(search));
        Assert.assertEquals(alert.getAlertUuid(), mapper.searchCiEntityAlert(search).get(0).getAlertUuid());
    }

    /** 解除关联、空字段被 JSON 省略时仍清理旧告警；未关联状态不新增。 */
    @Test
    public void missingAssociationClearsOnlyExistingSource() {
        consume(message("a", "Critical", 11L).toJSONString());
        consume(message("a", "Critical", null).toJSONString());
        Assert.assertEquals(0, count());
        consume(message("b", "Critical", null).toJSONString());
        Assert.assertEquals(0, count());
    }

    /** 其他租户、非服务、旧协议及错误 JSON 不进入事务落库。 */
    @Test
    public void invalidAndForeignMessagesAreSkipped() {
        JSONObject data = message("a", "Critical", 11L);
        data.put("tenant_uuid", "another-tenant"); consume(data.toJSONString());
        data = message("a", "Critical", 11L); data.put("category", "os_stat"); consume(data.toJSONString());
        data = message("a", "Critical", 11L); data.remove("alert_uuid"); consume(data.toJSONString());
        data = message("a", "Critical", 11L); data.put("cientity_id", 1.5); consume(data.toJSONString());
        consume("不是JSON");
        Assert.assertEquals(0, count());
    }

    /** 旧保存、按 ID 删除、巡检清理都不能覆盖来源告警。 */
    @Test
    public void legacyOperationsCannotMutateSourceAlerts() {
        consume(message("a", "Critical", 11L).toJSONString());
        CiEntityAlertVo legacy = new CiEntityAlertVo();
        legacy.setId(jdbc.queryForObject("select id from cmdb_cientity_alert", Long.class));
        legacy.setCiEntityId(11L); legacy.setMetricName("services/test"); legacy.setLevel(9);
        Assert.assertNull(mapper.getCiEntityAlert(legacy));
        mapper.updateCiEntityAlert(legacy);
        mapper.deleteCiEntityAlertById(legacy.getId());
        mapper.deleteCiEntityAlertByCiEntityIdAndLevelList(11L, Collections.singletonList(7));
        Assert.assertEquals(1, count());
        Assert.assertEquals(Integer.valueOf(7), jdbc.queryForObject("select level from cmdb_cientity_alert", Integer.class));
    }

    /** 秒和毫秒沿用源模块的统一时间解释。 */
    @Test
    public void acceptsSecondAndMillisecondTimestamps() {
        JSONObject data = message("a", "Critical", 11L);
        data.put("timestamp", 1793664000L);
        consume(data.toJSONString());
        Assert.assertEquals(1793664000000L, jdbc.queryForObject("select alert_time from cmdb_cientity_alert", java.sql.Timestamp.class).getTime());
        data.put("timestamp", 1793664000123L);
        consume(data.toJSONString());
        Assert.assertEquals(1793664000123L, jdbc.queryForObject("select alert_time from cmdb_cientity_alert", java.sql.Timestamp.class).getTime());
    }

    /** 历史无 UUID 告警仍按旧指标身份更新和删除。 */
    @Test
    public void legacyAlertsStillWorkWithNullIdentifiers() {
        CiEntityAlertVo legacy = new CiEntityAlertVo();
        legacy.setId(99L); legacy.setCiEntityId(11L); legacy.setMetricName("旧监控指标"); legacy.setLevel(1);
        mapper.insertCiEntityAlert(legacy);
        Assert.assertEquals(Long.valueOf(99), mapper.getCiEntityAlert(legacy).getId());
        legacy.setLevel(3); mapper.updateCiEntityAlert(legacy);
        Assert.assertEquals(3, mapper.getCiEntityAlert(legacy).getLevel());
        mapper.deleteCiEntityAlertById(99L);
        Assert.assertEquals(0, count());
    }

    /** 数据库错误必须传播给 Kafka，不允许被坏消息校验吞掉。 */
    @Test
    public void databaseFailuresEscapeConsumer() {
        jdbc.execute("drop table cmdb_cientity_alertlevel");
        try {
            consume(message("a", "Critical", 11L).toJSONString());
            Assert.fail("数据库失败必须传播");
        } catch (org.springframework.dao.DataAccessException expected) {
            Assert.assertEquals(0, count());
        }
    }

    /** 构造带稳定标识的原始消息。 */
    public JSONObject message(String digit, String state, Long ciEntityId) {
        JSONObject data = new JSONObject();
        data.put("alert_uuid", String.join("", Collections.nCopies(32, digit)));
        data.put("source", "informant"); data.put("category", "services"); data.put("tenant_uuid", "test-tenant");
        data.put("state", state); data.put("cientity_id", ciEntityId); data.put("title", "服务异常");
        data.put("ip", "127.0.0.1"); data.put("obj_path", "services/test"); data.put("timestamp", 1793664000000L);
        return data;
    }

    /** 统计已提交告警，确保不是会话缓存中的结果。 */
    public int count() { return jdbc.queryForObject("select count(*) from cmdb_cientity_alert", Integer.class); }

    /** 仅在测试侧为生产组件注入实际依赖。 */
    private void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
