/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the Sustainable Use License (SUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.service.cientity;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.match.CiEntityMatchCandidateVo;
import neatlogic.framework.cmdb.dto.cientity.match.CiEntityMatchConditionVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityMatchReadPrivilegeException;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMatchMapper;
import neatlogic.module.cmdb.service.cientity.match.CiEntityMatchServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 用真实 MyBatis 查询和隔离内存数据库验证严格匹配，不连接租户数据库。 */
public class CiEntityMatchServiceTest {
    private SqlSession session;
    private JdbcTemplate jdbc;
    private CiEntityMatchServiceImpl service;
    private CiVo model;
    private AttrVo address;
    private AttrVo port;
    private TenantContext previous;
    private int queries;

    /** 构造抽象模型表、继承属性表及真实子模型配置项。 */
    @Before
    public void setup() throws Exception {
        previous = TenantContext.get();
        TenantContext.init("match_test");
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), source));
        String resource = "neatlogic/module/cmdb/dao/mapper/cientity/CiEntityMatchMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(input);
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(config).openSession();
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(session.getConnection(), true));
        jdbc.execute("create schema neatlogic_match_test_data");
        jdbc.execute("create table cmdb_ci(id bigint primary key,name varchar(50),label varchar(50),is_virtual int,is_abstract int)");
        jdbc.execute("create table cmdb_cientity(id bigint primary key,ci_id bigint,name varchar(50))");
        jdbc.execute("create table cmdb_cientity_expiredtime(cientity_id bigint,expired_time timestamp)");
        jdbc.execute("create table neatlogic_match_test_data.cmdb_10(cientity_id bigint primary key)");
        jdbc.execute("create table neatlogic_match_test_data.cmdb_5(cientity_id bigint primary key,`51` varchar(100),`52` decimal(30,4),`51_hash` varchar(32))");
        jdbc.execute("insert into cmdb_ci values(10,'middleware','中间件',0,1),(11,'java','Java实例',0,0),(12,'nginx','Nginx实例',0,0),(13,'virtual','虚拟',1,0)");
        jdbc.execute("insert into cmdb_cientity values(101,11,'app-java'),(102,12,'app-nginx'),(103,11,'expired'),(104,13,'virtual')");
        jdbc.execute("insert into neatlogic_match_test_data.cmdb_10 values(101),(102),(103),(104)");
        jdbc.execute("insert into neatlogic_match_test_data.cmdb_5 values(101,'192.0.2.1',8080,null),(102,'192.0.2.2',8081,null),(103,'192.0.2.1',8080,null),(104,'192.0.2.1',8080,null)");
        jdbc.update("update neatlogic_match_test_data.cmdb_5 set `51_hash`=? where cientity_id in (101,103,104)", neatlogic.framework.util.Md5Util.encryptMD5("192.0.2.1"));
        jdbc.update("update neatlogic_match_test_data.cmdb_5 set `51_hash`=? where cientity_id=102", neatlogic.framework.util.Md5Util.encryptMD5("192.0.2.2"));
        jdbc.execute("insert into cmdb_cientity_expiredtime values(103,'2000-01-01 00:00:00')");
        model = new CiVo(); model.setId(10L); model.setName("middleware"); model.setLabel("中间件"); model.setIsAbstract(1); model.setIsVirtual(0);
        CiVo ancestor = new CiVo(); ancestor.setId(5L);
        address = attr(51L, "text"); port = attr(52L, "number");
        service = new CiEntityMatchServiceImpl();
        inject("ciMapper", proxy(CiMapper.class, (method, args) -> {
            if ("getCiById".equals(method)) { return model; }
            if ("getUpwardCiListByLR".equals(method)) { return Arrays.asList(ancestor, model); }
            throw new AssertionError(method);
        }));
        inject("attrMapper", proxy(AttrMapper.class, (method, args) -> Collections.unmodifiableList(Arrays.asList(address, port))));
        CiEntityMatchMapper mapper = session.getMapper(CiEntityMatchMapper.class);
        inject("ciEntityMatchMapper", proxy(CiEntityMatchMapper.class, (method, args) -> {
            queries++;
            return mapper.match((CiVo) args[0], (List) args[1]);
        }));
    }

    /** 释放会话和测试租户上下文，避免污染其他测试。 */
    @After
    public void cleanup() {
        if (session != null) { session.close(); }
        TenantContext.get().release();
        TenantContext.init(previous);
    }

    /** 抽象范围含继承属性，实际返回子模型编号并排除过期和虚拟候选。 */
    @Test
    public void abstractMatchReturnsRealModelAndInheritedProperties() {
        List<CiEntityMatchCandidateVo> found = match(condition(51L, "192.0.2.1"), condition(52L, "8080.00"));
        Assert.assertEquals(1, found.size());
        Assert.assertEquals(Long.valueOf(101), found.get(0).getId());
        Assert.assertEquals(Long.valueOf(11), found.get(0).getCiId());
        Assert.assertEquals("Java实例", found.get(0).getCiLabel());
    }

    /** 来源数组按任一相等处理，数值数组不能误转为范围命中中间端口。 */
    @Test
    public void arraysMatchAnyValueWithoutNumericRangeExpansion() {
        Assert.assertEquals(2, match(condition(51L, "192.0.2.1", "192.0.2.2")).size());
        Assert.assertEquals(0, match(condition(52L, "8079", "8082")).size());
        Assert.assertEquals(2, match(condition(52L, "8080", "8081")).size());
    }

    /** 属性删除、类型改变或来源缺失时，在执行 SQL 前失败而非缩减条件。 */
    @Test
    public void invalidConditionsNeverReachQuery() {
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(99L, "x")));
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(51L, "")));
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(51L, "x", null)));
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(52L, "not-number")));
        CiEntityMatchConditionVo changed = condition(52L, "8080");
        changed.setExpectedType("text");
        Assert.assertThrows(ApiRuntimeException.class, () -> match(changed));
        address.setType("expression");
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(51L, "x")));
        address.setType("text"); address.setCiId(999L);
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(51L, "x")));
        Assert.assertEquals(0, queries);
    }

    /** 相同属性重复配置不得静默变更语义；虚拟目标模型禁止执行。 */
    @Test
    public void duplicatesAndVirtualTargetsAreRejected() {
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(51L, "x"), condition(51L, "y")));
        model.setIsVirtual(1);
        Assert.assertThrows(ApiRuntimeException.class, () -> match(condition(51L, "x")));
        Assert.assertEquals(0, queries);
    }

    /** 候选数量上限在数据库侧生效，不因多命中加载全部配置项。 */
    @Test
    public void distinctCandidateLimitIsTwo() {
        jdbc.execute("insert into cmdb_cientity values(105,11,'third')");
        jdbc.execute("insert into neatlogic_match_test_data.cmdb_10 values(105)");
        jdbc.execute("insert into neatlogic_match_test_data.cmdb_5 values(105,'192.0.2.1',8080,null)");
        Assert.assertEquals(2, match(condition(52L, 8080, 8081)).size());
    }

    /** 用户态必须同时具有 CMDB 模块及模型全量读取权限，后台匹配契约不依赖用户授权。 */
    @Test
    public void userPermissionRequiresModuleAndModelReadAccess() throws Exception {
        neatlogic.framework.asynchronization.threadlocal.UserContext oldUser = neatlogic.framework.asynchronization.threadlocal.UserContext.get();
        Field userMapper = neatlogic.framework.auth.core.AuthActionChecker.class.getDeclaredField("userMapper");
        userMapper.setAccessible(true); Object oldMapper = userMapper.get(null);
        Field checkerInstance = neatlogic.module.cmdb.service.ci.CiAuthChecker.class.getDeclaredField("instance");
        checkerInstance.setAccessible(true); Object oldChecker = checkerInstance.get(null);
        final List<neatlogic.framework.dto.UserAuthVo> permissions = new java.util.ArrayList<>();
        try {
            java.lang.reflect.Constructor<neatlogic.framework.asynchronization.threadlocal.UserContext> constructor = neatlogic.framework.asynchronization.threadlocal.UserContext.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            neatlogic.framework.asynchronization.threadlocal.UserContext context = constructor.newInstance();
            context.setUserUuid("ci-match-reader");
            context.setAuthenticationInfoVo(new neatlogic.framework.dto.AuthenticationInfoVo("ci-match-reader"));
            neatlogic.framework.asynchronization.threadlocal.UserContext.init(context);
            userMapper.set(null, proxy(neatlogic.framework.dao.mapper.UserMapper.class, (name, args) -> permissions));
            neatlogic.module.cmdb.service.ci.CiAuthChecker checker = new neatlogic.module.cmdb.service.ci.CiAuthChecker();
            Field authMapper = neatlogic.module.cmdb.service.ci.CiAuthChecker.class.getDeclaredField("ciAuthMapper");
            authMapper.setAccessible(true);
            authMapper.set(checker, proxy(neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper.class, (name, args) -> Collections.emptyList()));
            Assert.assertThrows(CiEntityMatchReadPrivilegeException.class, () -> service.requireReadPrivilege(10L));
            neatlogic.framework.dto.UserAuthVo base = new neatlogic.framework.dto.UserAuthVo(); base.setAuth("CMDB_BASE"); permissions.add(base);
            Assert.assertThrows(CiEntityMatchReadPrivilegeException.class, () -> service.requireReadPrivilege(10L));
            neatlogic.framework.dto.UserAuthVo modelAccess = new neatlogic.framework.dto.UserAuthVo(); modelAccess.setAuth("CI_MODIFY"); permissions.add(modelAccess);
            service.requireReadPrivilege(10L);
            // 模型失效属于查询异常，调用方可以继续尝试其他适用规则。
            model.setIsVirtual(1);
            ApiRuntimeException virtual = Assert.assertThrows(ApiRuntimeException.class, () -> service.requireReadPrivilege(10L));
            Assert.assertFalse(virtual instanceof CiEntityMatchReadPrivilegeException);
            model = null;
            ApiRuntimeException missing = Assert.assertThrows(ApiRuntimeException.class, () -> service.requireReadPrivilege(10L));
            Assert.assertFalse(missing instanceof CiEntityMatchReadPrivilegeException);
            Assert.assertEquals(0, queries);
        } finally {
            userMapper.set(null, oldMapper); checkerInstance.set(null, oldChecker);
            neatlogic.framework.asynchronization.threadlocal.UserContext.get().release();
            if (oldUser != null) { neatlogic.framework.asynchronization.threadlocal.UserContext.init(oldUser); }
        }
    }

    /** 构建来源条件，保留空值以验证严格校验。 */
    private CiEntityMatchConditionVo condition(Long id, Object... values) {
        CiEntityMatchConditionVo result = new CiEntityMatchConditionVo(); result.setAttrId(id);
        JSONArray array = new JSONArray(); array.addAll(Arrays.asList(values)); result.setValueList(array); return result;
    }

    /** 构建从祖先模型继承的属性。 */
    private AttrVo attr(Long id, String type) { AttrVo attr = new AttrVo(); attr.setId(id); attr.setCiId(5L); attr.setType(type); return attr; }

    /** 保持测试调用与跨模块契约一致。 */
    private List<CiEntityMatchCandidateVo> match(CiEntityMatchConditionVo... values) { return service.match(10L, Arrays.asList(values)); }

    /** 仅在测试反射注入，不为生产代码增加装配入口。 */
    private void inject(String name, Object value) throws Exception { Field f = CiEntityMatchServiceImpl.class.getDeclaredField(name); f.setAccessible(true); f.set(service, value); }

    /** 为测试创建只读元数据替身。 */
    private <T> T proxy(Class<T> type, Call call) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> call.invoke(method.getName(), args))); }

    private interface Call { Object invoke(String name, Object[] args); }
}
