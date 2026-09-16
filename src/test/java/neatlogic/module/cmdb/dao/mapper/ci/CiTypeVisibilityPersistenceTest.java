package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

/** 验证层级显示配置的持久化兼容性，不连接数据库或启动 Spring。 */
public class CiTypeVisibilityPersistenceTest {
    private static final String NAMESPACE = CiTypeMapper.class.getName() + ".";
    private static final String PROPERTY = "isShowInCiEntityQuery";
    private static final String COLUMN = "is_showincientityquery";
    private Configuration configuration;

    /** 加载实际 Mapper，同时为关联缓存提供不依赖其他 Mapper 的替身。 */
    @Before
    public void loadMapper() throws Exception {
        configuration = new Configuration();
        configuration.addCache(new PerpetualCache("neatlogic.module.cmdb.dao.mapper.ci.CiMapper"));
        String resource = "neatlogic/module/cmdb/dao/mapper/ci/CiTypeMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    /** 旧请求未提供字段时必须保留数据库状态，显式关闭或开启才更新对应列。 */
    @Test
    public void updatePreservesOmittedSettingAndPersistsExplicitValues() {
        CiTypeVo type = new CiTypeVo();
        type.setId(100L);
        Assert.assertNull(type.getIsShowInCiEntityQuery());
        BoundSql omitted = sql("updateCiType", type);
        Assert.assertFalse(omitted.getSql().contains(COLUMN));
        Assert.assertFalse(hasVisibilityParameter(omitted));

        for (Integer value : Arrays.asList(0, 1)) {
            type.setIsShowInCiEntityQuery(value);
            BoundSql explicit = sql("updateCiType", type);
            Assert.assertTrue(explicit.getSql().contains("`" + COLUMN + "` = ?"));
            Assert.assertTrue(hasVisibilityParameter(explicit));
            Assert.assertEquals(value, configuration.newMetaObject(explicit.getParameterObject()).getValue(PROPERTY));
        }

        type.setIsShowInCiEntityQuery(null);
        Assert.assertFalse(sql("updateCiType", type).getSql().contains(COLUMN));
    }

    /** 新建旧请求通过数据库表达式默认开启，同时保留显式的零值。 */
    @Test
    public void insertDefaultsOnlyMissingSettingToEnabled() {
        CiTypeVo type = new CiTypeVo();
        type.setId(100L);
        for (Integer value : Arrays.asList(null, 0, 1)) {
            type.setIsShowInCiEntityQuery(value);
            BoundSql insert = sql("insertCiType", type);
            Assert.assertTrue(insert.getSql().contains("`" + COLUMN + "`"));
            Assert.assertTrue(insert.getSql().replaceAll("\\s+", " ").contains("COALESCE(?, 1)"));
            Assert.assertTrue(hasVisibilityParameter(insert));
            Assert.assertEquals(value, configuration.newMetaObject(insert.getParameterObject()).getValue(PROPERTY));
        }
    }

    /** 所有层级读取入口均返回该字段，未指定条件时仍返回隐藏层级。 */
    @Test
    public void readsReturnSettingWithoutFilteringHiddenLevels() {
        CiTypeVo type = new CiTypeVo();
        Assert.assertNull(type.getIsShowInCiEntityQuery());
        assertProjection(sql("searchCiType", type));
        assertProjection(sql("getCiTypeById", 100L));
        assertProjection(sql("getCiTypeByName", "层级"));
        assertProjection(sql("getCiTypeListByIdList", Collections.singletonMap("list", Arrays.asList(100L, 101L))));
        Assert.assertTrue(configuration.newMetaObject(type).hasSetter(PROPERTY));
    }

    /** 层级查询只在明确传入条件时按显示配置过滤，零值也不能被忽略。 */
    @Test
    public void searchFiltersOnlyExplicitVisibilityValues() {
        CiTypeVo type = new CiTypeVo();
        Assert.assertFalse(hasVisibilityParameter(sql("searchCiType", type)));
        for (Integer value : Arrays.asList(0, 1)) {
            type.setIsShowInCiEntityQuery(value);
            BoundSql filtered = sql("searchCiType", type);
            Assert.assertTrue(filtered.getSql().contains("`" + COLUMN + "` = ?"));
            Assert.assertTrue(hasVisibilityParameter(filtered));
            Assert.assertEquals(value, configuration.newMetaObject(filtered.getParameterObject()).getValue(PROPERTY));
        }
    }

    /** 生成真实动态 SQL，检查参数遗漏和条件分支。 */
    private BoundSql sql(String statement, Object parameter) {
        return configuration.getMappedStatement(NAMESPACE + statement).getBoundSql(parameter);
    }

    /** 检查新增字段以 DTO 属性名返回，且仅出现在投影中。 */
    private void assertProjection(BoundSql boundSql) {
        String statement = boundSql.getSql().replaceAll("\\s+", " ");
        Assert.assertTrue(statement.contains("`" + COLUMN + "` AS " + PROPERTY));
        Assert.assertEquals(statement.indexOf(COLUMN), statement.lastIndexOf(COLUMN));
        Assert.assertFalse(hasVisibilityParameter(boundSql));
    }

    /** 判断实际 SQL 参数映射是否绑定新增配置字段。 */
    private boolean hasVisibilityParameter(BoundSql boundSql) {
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            if (PROPERTY.equals(mapping.getProperty())) {
                Assert.assertEquals(Integer.class, mapping.getJavaType());
                return true;
            }
        }
        return false;
    }
}
