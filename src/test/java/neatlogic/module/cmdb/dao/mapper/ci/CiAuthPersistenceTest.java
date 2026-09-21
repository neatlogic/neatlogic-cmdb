package neatlogic.module.cmdb.dao.mapper.ci;

import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** 验证模型授权批量 SQL 能被 MyBatis 正确解析并生成预期语句。 */
public class CiAuthPersistenceTest {
    private static final String NAMESPACE = CiAuthMapper.class.getName() + ".";
    private Configuration configuration;

    @Before
    public void loadMapper() throws Exception {
        configuration = new Configuration();
        configuration.addCache(new PerpetualCache("neatlogic.module.cmdb.dao.mapper.ci.CiMapper"));
        String resource = "neatlogic/module/cmdb/dao/mapper/ci/CiAuthMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @Test
    public void batchInsertIsIdempotent() {
        CiAuthVo first = auth(1L, "cimanage", "role", "role-a");
        CiAuthVo second = auth(2L, "cientityquery", "team", "team-a");
        Map<String, Object> params = new HashMap<>();
        params.put("ciAuthList", Arrays.asList(first, second));

        String sql = normalize(sql("insertCiAuthList", params).getSql());

        Assert.assertTrue(sql.startsWith("INSERT INTO `cmdb_ci_auth`"));
        Assert.assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
        Assert.assertEquals(8, sql("insertCiAuthList", params).getParameterMappings().size());
    }

    @Test
    public void batchDeleteUsesAllTargetIds() {
        Map<String, Object> params = new HashMap<>();
        params.put("ciIdList", Arrays.asList(1L, 2L, 3L));

        BoundSql boundSql = sql("deleteCiAuthByCiIdList", params);
        String sql = normalize(boundSql.getSql());

        Assert.assertTrue(sql.contains("WHERE `ci_id` IN ( ? , ? , ? )"));
        Assert.assertEquals(3, boundSql.getParameterMappings().size());
    }

    private BoundSql sql(String statement, Object parameter) {
        return configuration.getMappedStatement(NAMESPACE + statement).getBoundSql(parameter);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private CiAuthVo auth(Long ciId, String action, String authType, String authUuid) {
        CiAuthVo authVo = new CiAuthVo();
        authVo.setCiId(ciId);
        authVo.setAction(action);
        authVo.setAuthType(authType);
        authVo.setAuthUuid(authUuid);
        return authVo;
    }
}
