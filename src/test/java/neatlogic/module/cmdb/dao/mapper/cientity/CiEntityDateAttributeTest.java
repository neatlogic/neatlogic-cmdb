package neatlogic.module.cmdb.dao.mapper.cientity;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.validator.DatetimeAttrFormatIrregularException;
import neatlogic.framework.dao.plugin.CompressHandler;
import neatlogic.framework.dao.plugin.Md5Handler;
import neatlogic.module.cmdb.attrvaluehandler.handler.DateTimeValueHandler;
import neatlogic.module.cmdb.attrvaluehandler.handler.DateValueHandler;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class CiEntityDateAttributeTest {
    /** 删除快照使用的精简查询只对日期类属性处理历史全零值。 */
    @Test
    public void projectsZeroDatesAsNullBeforeJdbcMapping() throws Exception {
        TenantContext previousTenant = TenantContext.get();
        TenantContext.init("date_attr_test");
        try {
            Configuration configuration = new Configuration();
            configuration.getTypeAliasRegistry().registerAlias("CompressHandler", CompressHandler.class);
            configuration.getTypeAliasRegistry().registerAlias("Md5Handler", Md5Handler.class);
            String resource = "neatlogic/module/cmdb/dao/mapper/cientity/CiEntityMapper.xml";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream);
                new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
            }

            AttrVo date = ordinaryAttr(21L, "date");
            AttrVo dateTime = ordinaryAttr(22L, "datetime");
            AttrVo text = ordinaryAttr(23L, "text");
            CiVo model = new CiVo();
            model.setId(10L);
            model.setIsVirtual(0);
            CiEntityVo entity = new CiEntityVo(100L);
            entity.setCiList(List.of(model));
            entity.setAttrList(List.of(date, dateTime, text));

            String sql = configuration.getMappedStatement(CiEntityMapper.class.getName() + ".getCiEntityByIdLite")
                    .getBoundSql(entity).getSql().replaceAll("\\s+", " ");
            for (AttrVo attr : List.of(date, dateTime)) {
                String column = attr.getCiTableName() + ".`" + attr.getId() + "`";
                assertTrue(sql.contains("CASE WHEN LEFT(CAST(" + column + " AS CHAR), 10) = '0000-00-00'"
                        + " THEN NULL ELSE " + column + " END AS `attr_" + attr.getId() + "`"));
            }
            assertTrue(sql.contains("AS `attr_22`, " + text.getCiTableName() + ".`23` AS `attr_23`,"));
        } finally {
            TenantContext.get().release();
            TenantContext.init(previousTenant);
        }
    }

    /** 保存拒绝全零和超出日历范围的日期，同时接受有效日期。 */
    @Test
    public void validatesDateInputsStrictly() {
        checkCalendarInput(new DateValueHandler(), "", "2024-02-29");
        checkCalendarInput(new DateTimeValueHandler(), " 00:00:00", "2024-02-29 00:00:00");
    }

    /** 验证不同日期类型共用的空值和日历校验约定。 */
    private void checkCalendarInput(IAttrValueHandler handler, String timeSuffix, String validDate) {
        AttrVo attr = ordinaryAttr(21L, handler.getType());
        for (String invalidDate : List.of("0000-00-00", "2024-01-00", "2023-02-29")) {
            JSONArray input = new JSONArray(List.of(invalidDate + timeSuffix));
            assertThrows(DatetimeAttrFormatIrregularException.class, () -> handler.transferValueListToSave(attr, input));
        }
        JSONArray input = new JSONArray();
        input.add("");
        input.add(validDate);
        handler.transferValueListToSave(attr, input);
        assertEquals(List.of(validDate), input);
    }

    /** 提供普通动态表属性元数据，无需启动模块工厂。 */
    private AttrVo ordinaryAttr(Long id, String type) {
        AttrVo attr = new AttrVo() {
            @Override
            public Boolean isNeedTargetCi() {
                return false;
            }

            @Override
            public Boolean getIsInvokeAttr() {
                return false;
            }
        };
        attr.setId(id);
        attr.setCiId(10L);
        attr.setType(type);
        attr.setLabel("测试属性");
        attr.setName("test_attr");
        return attr;
    }
}
