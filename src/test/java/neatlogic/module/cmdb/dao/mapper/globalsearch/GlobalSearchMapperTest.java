package neatlogic.module.cmdb.dao.mapper.globalsearch;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.module.cmdb.api.globalsearch.SearchCiEntityCountApi;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 使用真实 Mapper 和隔离数据库验证 ID、全文索引合并及旧入口兼容性。 */
public class GlobalSearchMapperTest {
    private SqlSession session;
    private GlobalSearchMapper mapper;
    private JdbcTemplate jdbc;

    /** 构造无索引配置项、重复索引命中和其他模型的文本命中。 */
    @Before
    public void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        String resource = "neatlogic/module/cmdb/dao/mapper/globalsearch/GlobalSearchMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        // 会话连接保持内存数据库存活，所有测试数据仅存在于该连接生命周期内。
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(session.getConnection(), true));
        jdbc.execute("create table cmdb_ci (id bigint primary key, name varchar(50), label varchar(50), icon varchar(50))");
        jdbc.execute("create table cmdb_cientity (id bigint primary key, ci_id bigint)");
        jdbc.execute("create table fulltextindex_target_cmdb (target_id bigint, target_type varchar(50))");
        jdbc.execute("create table fulltextindex_field_cmdb (target_id bigint, word_id bigint)");
        jdbc.execute("create table fulltextindex_word (id bigint primary key, word varchar(100))");
        jdbc.execute("insert into cmdb_ci values (1,'host','主机','host'), (2,'database','数据库','database')");
        jdbc.execute("insert into cmdb_cientity values (9007199254740993,1), (201,1), (202,2)");
        jdbc.execute("insert into fulltextindex_target_cmdb values (201,'cientity'), (202,'cientity')");
        jdbc.execute("insert into fulltextindex_word values (1,'201'), (2,'201-extra'), (3,'mysql'), (4,'192.168.0.22')");
        jdbc.execute("insert into fulltextindex_field_cmdb values (201,1), (201,2), (202,1), (202,3), (202,4)");
        session.commit();
        mapper = session.getMapper(GlobalSearchMapper.class);
    }

    /** 关闭会话并释放隔离数据库。 */
    @After
    public void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    /** 完整长整数 ID 不依赖任何索引，首尾空白按原有条件规范化。 */
    @Test
    public void exactIdWithoutIndexIsFoundThroughApi() throws Exception {
        assertCount(search(" 9007199254740993 "), 1L, 1);
        assertCount(mapper.searchCiEntityCountByKeyword(new ArrayList<>(), 9007199254740993L), 1L, 1);
    }

    /** ID 和多个索引同时命中只计一次，其他模型的文本命中仍保留。 */
    @Test
    public void idAndTextMatchesAreMergedWithoutDoubleCounting() throws Exception {
        List<CiVo> results = search("201");
        Assert.assertEquals(2, results.size());
        assertCount(results, 1L, 1);
        assertCount(results, 2L, 1);
    }

    /** 不存在的完整 ID 和无文本命中的 ID 前缀均不能匹配其他配置项。 */
    @Test
    public void unknownIdAndIdPrefixDoNotMatch() throws Exception {
        Assert.assertTrue(search("9007199254740994").isEmpty());
        Assert.assertTrue(search("900719925474099").isEmpty());
    }

    /** 普通名称和 IP 继续使用原有分词索引。 */
    @Test
    public void textAndIpSearchRemainAvailable() throws Exception {
        assertCount(search("mysql"), 2L, 1);
        assertCount(search("192.168.0.22"), 2L, 1);
    }

    /** 超出 Long 范围仍按文本匹配，不因转换失败而报错。 */
    @Test
    public void overflowingNumberRemainsTextSearch() throws Exception {
        jdbc.execute("insert into fulltextindex_word values (5,'9223372036854775808')");
        jdbc.execute("insert into fulltextindex_field_cmdb values (202,5)");
        session.commit();
        assertCount(search("9223372036854775808"), 2L, 1);
    }

    /** MyBatis 代理执行旧方法时仍仅按分词检索，并正确调用新的映射语句。 */
    @Test
    public void legacyWordOnlyEntryRemainsCompatible() {
        Assert.assertTrue(mapper.searchCiEntityCountByWord(new ArrayList<>(Collections.singletonList("9007199254740993"))).isEmpty());
        assertCount(mapper.searchCiEntityCountByWord(new ArrayList<>(Collections.singletonList("mys"))), 2L, 1);
        Assert.assertTrue(mapper.searchCiEntityCountByWord(new ArrayList<>()).isEmpty());
    }

    /** 在测试侧注入真实 Mapper，覆盖 API 关键字解析和返回结构。 */
    @SuppressWarnings("unchecked")
    private List<CiVo> search(String keyword) throws Exception {
        SearchCiEntityCountApi api = new SearchCiEntityCountApi();
        Field field = SearchCiEntityCountApi.class.getDeclaredField("globalSearchMapper");
        field.setAccessible(true);
        field.set(api, mapper);
        JSONObject request = new JSONObject();
        request.put("keyword", keyword);
        JSONObject result = (JSONObject) api.myDoService(request);
        Assert.assertTrue(result.containsKey("wordList"));
        return (List<CiVo>) result.get("ciList");
    }

    /** 检查指定模型的去重数量，不依赖模型返回顺序。 */
    private void assertCount(List<CiVo> results, Long ciId, int expected) {
        CiVo ci = results.stream().filter(item -> ciId.equals(item.getId())).findFirst().orElse(null);
        Assert.assertNotNull(ci);
        Assert.assertEquals(expected, ci.getCiEntityCount());
    }
}
