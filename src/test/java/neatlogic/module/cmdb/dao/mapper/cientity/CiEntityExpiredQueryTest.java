package neatlogic.module.cmdb.dao.mapper.cientity;

import com.alibaba.fastjson.JSON;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityExpiredTimeVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/** 验证生产查询 SQL 的过期范围开关及普通查询兼容性，不连接数据库。 */
public class CiEntityExpiredQueryTest {
    private Configuration configuration;
    private TenantContext previousTenant;

    /** 加载实际 Mapper，并为动态表名提供测试租户。 */
    @Before
    public void loadMapper() throws Exception {
        previousTenant = TenantContext.get();
        TenantContext.init("expired_test");
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("CompressHandler", neatlogic.framework.dao.plugin.CompressHandler.class);
        configuration.getTypeAliasRegistry().registerAlias("Md5Handler", neatlogic.framework.dao.plugin.Md5Handler.class);
        String resource = "neatlogic/module/cmdb/dao/mapper/cientity/CiEntityMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    /** 恢复租户上下文，不影响其他测试。 */
    @After
    public void restoreTenant() {
        TenantContext.get().release();
        TenantContext.init(previousTenant);
    }

    /** 普通查询的列表和计数仍然同时排除过期配置项。 */
    @Test
    public void ordinaryQueriesStillExcludeExpiredCi() {
        CiEntityVo condition = condition();
        Assert.assertFalse(condition.isIncludeExpired());
        for (String statement : Arrays.asList("searchCiEntityId", "searchCiEntityIdCount")) {
            Assert.assertTrue(sql(statement, condition).contains("xx.expired_time >= NOW()"));
        }
    }

    /** 同步显式包含过期配置项时，列表和计数均不再加失效时间过滤。 */
    @Test
    public void internalLookupCanIncludeExpiredCi() {
        CiEntityVo condition = condition();
        condition.setIncludeExpired(true);
        for (String statement : Arrays.asList("searchCiEntityId", "searchCiEntityIdCount")) {
            Assert.assertFalse(sql(statement, condition).contains("cmdb_cientity_expiredtime"));
        }
    }

    /** 开关仅供后端使用，外部 JSON 不能改变普通查询的过期范围。 */
    @Test
    public void incomingJsonCannotEnableExpiredLookup() {
        CiEntityVo condition = JSON.parseObject("{\"includeExpired\":true}", CiEntityVo.class);
        Assert.assertFalse(condition.isIncludeExpired());
    }

    /** 复用旧数据的续期 SQL 以当前时间重新计算失效时间，并绑定原配置项编号。 */
    @Test
    public void renewalUsesCurrentTimeAndOriginalCiId() {
        CiEntityExpiredTimeVo expiry = new CiEntityExpiredTimeVo();
        expiry.setCiEntityId(300L);
        expiry.setExpiredDay(1);
        org.apache.ibatis.mapping.BoundSql bound = configuration.getMappedStatement(
                CiEntityMapper.class.getName() + ".updateCiEntityExpiredTime").getBoundSql(expiry);
        Assert.assertTrue(bound.getSql().replaceAll("\\s+", " ").contains("DATE_ADD(NOW(3), INTERVAL ? DAY)"));
        Assert.assertEquals("expiredDay", bound.getParameterMappings().get(0).getProperty());
        Assert.assertEquals("ciEntityId", bound.getParameterMappings().get(1).getProperty());
    }

    /** 构造真实模型查询的最小参数。 */
    private CiEntityVo condition() {
        CiVo ci = new CiVo();
        ci.setId(1L);
        ci.setIsVirtual(0);
        CiEntityVo condition = new CiEntityVo();
        condition.setCiId(1L);
        condition.setCiList(new ArrayList<>(Collections.singletonList(ci)));
        condition.setAttrList(new ArrayList<>());
        condition.setRelList(new ArrayList<>());
        return condition;
    }

    /** 解析生产动态 SQL，验证实际生成的过滤条件。 */
    private String sql(String statement, CiEntityVo condition) {
        return configuration.getMappedStatement(CiEntityMapper.class.getName() + "." + statement)
                .getBoundSql(condition).getSql().replaceAll("\\s+", " ");
    }
}
