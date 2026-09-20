package neatlogic.module.cmdb.dao.mapper.ci;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.module.cmdb.api.ci.GetCiTreeApi;
import neatlogic.module.cmdb.api.citype.ListCiTypeApi;
import neatlogic.module.cmdb.api.citype.SearchCiTypeCiApi;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

/** 验证模型联合查询及自定义树的显式过滤边界，不连接数据库。 */
public class CiQueryVisibilityFilterTest {
    private static final String PROPERTY = "isShowInCiEntityQuery";
    private static final String COLUMN = "is_showincientityquery";
    private Configuration configuration;

    /** 保留实际 Mapper SQL，仅替换依赖租户环境的缓存配置。 */
    @Before
    public void loadMapper() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("CompressHandler", neatlogic.framework.dao.plugin.CompressHandler.class);
        String resource = "neatlogic/module/cmdb/dao/mapper/ci/CiMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            String xml = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
            xml = xml.replaceFirst("(?s)<cache\\s[^>]*>.*?</cache>",
                    "<cache type=\"org.apache.ibatis.cache.impl.PerpetualCache\"/>");
            new XMLMapperBuilder(new StringReader(xml), configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    /** 新参数默认不参与联合查询，显式零和一均生成对应条件。 */
    @Test
    public void combinedQueryRequiresExplicitVisibilityFilter() {
        CiVo condition = new CiVo();
        Assert.assertNull(condition.getIsShowInCiEntityQuery());
        assertNoVisibilityFilter(sql("searchCiTypeCi", condition));
        for (Integer value : Arrays.asList(0, 1)) {
            condition.setIsShowInCiEntityQuery(value);
            assertVisibilityFilter(sql("searchCiTypeCi", condition), value);
        }
    }

    /** 自定义树旧调用不加过滤，显式条件按模型所属层级过滤。 */
    @Test
    public void treeQueryRequiresExplicitVisibilityFilter() {
        assertNoVisibilityFilter(sql("getCiTree", Collections.singletonMap(PROPERTY, null)));
        for (Integer value : Arrays.asList(0, 1)) {
            assertVisibilityFilter(sql("getCiTree", Collections.singletonMap(PROPERTY, value)), value);
        }
    }

    /** 拓扑原有条件独立工作，不因新增字段默认值而隐藏模型。 */
    @Test
    public void topologyConditionDoesNotImplicitlyEnableQueryFilter() {
        CiVo condition = new CiVo();
        condition.setIsTypeShowInTopo(1);
        BoundSql bound = sql("searchCiTypeCi", condition);
        Assert.assertTrue(bound.getSql().contains("is_showintopo = ?"));
        assertNoVisibilityFilter(bound);
        Assert.assertTrue(bound.getParameterMappings().stream()
                .anyMatch(mapping -> "isTypeShowInTopo".equals(mapping.getProperty())));
    }

    /** 三个入口只允许显式启用过滤，不设置默认值，也不将查询条件输出到模型响应。 */
    @Test
    public void apiContractsKeepFilterOptionalAndQueryOnly() throws Exception {
        for (Class<?> api : Arrays.asList(ListCiTypeApi.class, SearchCiTypeCiApi.class, GetCiTreeApi.class)) {
            Input input = api.getDeclaredMethod("myDoService", JSONObject.class).getAnnotation(Input.class);
            Assert.assertNotNull(input);
            Param parameter = Arrays.stream(input.value()).filter(param -> PROPERTY.equals(param.name())).findFirst().orElse(null);
            Assert.assertNotNull(parameter);
            Assert.assertEquals(ApiParamType.INTEGER, parameter.type());
            Assert.assertEquals("1", parameter.rule());
            Assert.assertEquals("", parameter.defaultValue());
            Assert.assertFalse(parameter.isRequired());
        }
        JSONField field = CiVo.class.getDeclaredField(PROPERTY).getAnnotation(JSONField.class);
        Assert.assertNotNull(field);
        Assert.assertFalse(field.serialize());
    }

    /** 解析真实动态 SQL。 */
    private BoundSql sql(String statement, Object parameter) {
        return configuration.getMappedStatement(CiMapper.class.getName() + "." + statement).getBoundSql(parameter);
    }

    /** 检查新增条件和参数值同时进入最终 SQL。 */
    private void assertVisibilityFilter(BoundSql bound, Integer value) {
        Assert.assertTrue(bound.getSql().replace("`", "").replaceAll("\\s+", " ").contains(COLUMN + " = ?"));
        Assert.assertTrue(bound.getParameterMappings().stream()
                .anyMatch(mapping -> PROPERTY.equals(mapping.getProperty())));
        Assert.assertEquals(value, configuration.newMetaObject(bound.getParameterObject()).getValue(PROPERTY));
    }

    /** 检查未传参时没有新增过滤绑定，允许读取结果投影包含配置列。 */
    private void assertNoVisibilityFilter(BoundSql bound) {
        Assert.assertFalse(bound.getSql().replace("`", "").replaceAll("\\s+", " ").contains(COLUMN + " = ?"));
        for (ParameterMapping mapping : bound.getParameterMappings()) {
            Assert.assertNotEquals(PROPERTY, mapping.getProperty());
        }
    }
}
