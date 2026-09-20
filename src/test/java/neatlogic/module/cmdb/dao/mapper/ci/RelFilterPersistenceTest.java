package neatlogic.module.cmdb.dao.mapper.ci;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

/** 验证实际 Mapper 的缺省兼容、显式清空及 JSON 回读，不连接数据库。 */
public class RelFilterPersistenceTest {
    private Configuration configuration;

    /** 加载生产 Mapper，并隔离关联缓存。 */
    @Before
    public void loadMapper() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("CompressHandler", neatlogic.framework.dao.plugin.CompressHandler.class);
        configuration.addCache(new PerpetualCache("neatlogic.module.cmdb.dao.mapper.ci.CiMapper"));
        String resource = "neatlogic/module/cmdb/dao/mapper/ci/RelMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    /** 缺省字段不更新，新旧导入请求可保持原条件；显式空值必须生成清空 SQL。 */
    @Test
    public void omittedAndExplicitNullRemainDistinct() {
        RelVo rel = JSON.toJavaObject(JSONObject.parseObject("{\"id\":1}"), RelVo.class);
        Assert.assertFalse(rel.isFromFilterSpecified());
        Assert.assertFalse(sql("updateRel", rel).getSql().contains("from_filter"));
        RelVo cleared = JSON.toJavaObject(JSONObject.parseObject("{\"id\":1,\"fromFilter\":null}"), RelVo.class);
        // API 另用 containsKey 显式调用 setter，不依赖不同 FastJSON 版本的空值调用行为。
        cleared.setFromFilter(null);
        Assert.assertTrue(sql("updateRel", cleared).getSql().contains("from_filter"));
        Assert.assertFalse(sql("updateRel", cleared).getSql().contains("to_filter"));
        Assert.assertNull(cleared.getFromFilterStr());
    }

    /** 数据库字符串正确还原两端 JSON；内部提交标记和存储字符串不暴露给 API。 */
    @Test
    public void jsonRoundTripAndProjection() throws Exception {
        RelVo rel = new RelVo();
        rel.setId(1L);
        JSONObject filter = JSONObject.parseObject("{\"filterCiId\":10}");
        rel.setFromFilter(filter);
        Assert.assertTrue(sql("insertRel", rel).getSql().contains("from_filter"));
        RelVo loaded = new RelVo();
        loaded.setId(1L);
        loaded.setFromFilterStr(rel.getFromFilterStr());
        Assert.assertEquals(filter, loaded.getFromFilter());
        Assert.assertFalse(loaded.isFromFilterSpecified());
        Assert.assertTrue(sql("getRelById", 1L).getSql().contains("a.from_filter AS fromFilterStr"));
        Assert.assertFalse(sql("getRelTypeByRelId", 1L).getSql().contains("from_filter"));
        Assert.assertTrue(configuration.getResultMap(RelMapper.class.getName() + ".relDetailMap").getResultMappings()
                .stream().anyMatch(mapping -> "fromFilterStr".equals(mapping.getProperty())));
        Assert.assertFalse(RelVo.class.getMethod("isFromFilterSpecified").getAnnotation(JSONField.class).serialize());
        Assert.assertFalse(RelVo.class.getMethod("getFromFilterStr").getAnnotation(JSONField.class).serialize());
    }

    /** 生成与生产运行相同的动态 SQL。 */
    private BoundSql sql(String statement, Object parameter) {
        return configuration.getMappedStatement(RelMapper.class.getName() + "." + statement).getBoundSql(parameter);
    }
}
