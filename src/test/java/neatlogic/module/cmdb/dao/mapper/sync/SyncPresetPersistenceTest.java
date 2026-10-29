package neatlogic.module.cmdb.dao.mapper.sync;

import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.dao.plugin.CompressHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

/** 解析真实 MyBatis 配置，验证预设字段读取和局部更新的边界。 */
public class SyncPresetPersistenceTest {
    private static final String NAMESPACE = SyncMapper.class.getName() + ".";
    private Configuration configuration;

    /** 使用实际压缩处理器加载同步映射 XML。 */
    @Before
    public void loadMapper() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("CompressHandler", CompressHandler.class);
        String resource = "neatlogic/module/cmdb/dao/mapper/sync/SyncMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    /** 详情、批量和执行全部使用的查询都必须读取持久化预设。 */
    @Test
    public void executionAndDetailQueriesReadPreset() {
        Assert.assertTrue(sql("getSyncCiCollectionById", 1L).contains("default_condition AS defaultConditionStr"));
        Assert.assertTrue(sql("getSyncCiCollectionByIdList", Collections.singletonMap("idList", new ArrayList<>(Collections.singletonList(1L))))
                .contains("default_condition AS defaultConditionStr"));
        Assert.assertTrue(sql("searchSyncCiCollection", new SyncCiCollectionVo()).contains("default_condition AS defaultConditionStr"));
        for (String map : new String[]{"syncCiCollectionResultMap", "syncCiCollectionMap"}) {
            ResultMapping mapping = configuration.getResultMap(NAMESPACE + map).getResultMappings().stream()
                    .filter(item -> "defaultConditionStr".equals(item.getProperty())).findFirst().orElse(null);
            Assert.assertNotNull(mapping);
            Assert.assertTrue(mapping.getTypeHandler() instanceof CompressHandler);
        }
    }

    /** 独立保存仅修改预设和审计字段，普通映射编辑不覆盖预设。 */
    @Test
    public void presetUpdateDoesNotResetSyncStateOrMappings() {
        SyncCiCollectionVo mapping = new SyncCiCollectionVo();
        mapping.setId(1L);
        mapping.setDefaultConditionList(Collections.emptyList());
        String sql = sql("updateSyncCiCollectionCondition", mapping);
        Assert.assertTrue(sql, sql.contains("default_condition"));
        Assert.assertTrue(sql, sql.contains("lcu") && sql.contains("lcd"));
        Assert.assertFalse(sql, sql.contains("last_sync_date"));
        Assert.assertFalse(sql, sql.contains("collect_mode"));
        Assert.assertTrue(sql, sql.contains("WHERE id = ?"));
        BoundSql bound = configuration.getMappedStatement(NAMESPACE + "updateSyncCiCollectionCondition").getBoundSql(mapping);
        Assert.assertTrue(bound.getParameterMappings().stream()
                .anyMatch(item -> "defaultConditionStr".equals(item.getProperty()) && item.getTypeHandler() instanceof CompressHandler));
        Assert.assertFalse(sql("updateSyncCiCollection", mapping).contains("default_condition"));
    }

    /** 归一化空白后检查实际生成的 SQL。 */
    private String sql(String id, Object parameter) {
        return configuration.getMappedStatement(NAMESPACE + id).getBoundSql(parameter).getSql().replaceAll("\\s+", " ").trim();
    }
}
