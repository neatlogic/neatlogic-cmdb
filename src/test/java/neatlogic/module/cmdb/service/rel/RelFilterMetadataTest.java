package neatlogic.module.cmdb.service.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

/** 使用 Mapper 替身及最小翻译上下文验证元数据归属和关系方向，不连接数据库。 */
public class RelFilterMetadataTest extends RelFilterI18nTestBase {
    /** 自关联两个方向都有效，缺失方向不可被编号匹配掩盖。 */
    @Test
    public void validatesRelationDirectionAndMissingMetadata() throws Exception {
        RelVo from = new RelVo(1L, "from");
        RelVo to = new RelVo(1L, "to");
        RelFilterServiceImpl service = new RelFilterServiceImpl();
        inject(service, "relMapper", RelMapper.class, Arrays.asList(from, to));
        invoke(service, "validateRelations", "{\"relFilterList\":[{\"relId\":1,\"direction\":\"from\",\"expression\":\"is-null\"},{\"relId\":1,\"direction\":\"to\",\"expression\":\"like\"}]}", false);
        inject(service, "relMapper", RelMapper.class, Collections.singletonList(from));
        invoke(service, "validateRelations", "{\"relFilterList\":[{\"relId\":1,\"direction\":\"to\",\"expression\":\"is-null\"}]}", true);
    }

    /** 全局属性停用、失效或操作符越界均不能保存。 */
    @Test
    public void validatesGlobalAttributeAvailabilityAndOperators() throws Exception {
        GlobalAttrVo attr = new GlobalAttrVo();
        attr.setId(1L);
        attr.setIsActive(1);
        RelFilterServiceImpl service = new RelFilterServiceImpl();
        inject(service, "globalAttrMapper", GlobalAttrMapper.class, Collections.singletonList(attr));
        invoke(service, "validateGlobals", "{\"globalAttrFilterList\":[{\"attrId\":1,\"expression\":\"like\"}]}", false);
        invoke(service, "validateGlobals", "{\"globalAttrFilterList\":[{\"attrId\":1,\"expression\":\"equal\"}]}", true);
        attr.setIsActive(0);
        invoke(service, "validateGlobals", "{\"globalAttrFilterList\":[{\"attrId\":1,\"expression\":\"like\"}]}", true);
        invoke(service, "validateGlobals", "{\"globalAttrFilterList\":[{\"attrId\":2,\"expression\":\"like\"}]}", true);
    }

    /** 普通属性不属于模型时明确拒绝，不能像搜索执行器一样跳过。 */
    @Test
    public void rejectsAttributeOutsideModel() throws Exception {
        RelFilterServiceImpl service = new RelFilterServiceImpl();
        inject(service, "attrMapper", AttrMapper.class, Collections.emptyList());
        invoke(service, "validateAttributes", "{\"attrFilterList\":[{\"attrId\":1,\"expression\":\"equal\"}]}", true);
    }

    /** 只在测试中注入元数据替身，生产组件保持标准字段注入。 */
    private void inject(RelFilterServiceImpl service, String name, Class<?> type, Object result) throws Exception {
        Field field = RelFilterServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Assert.assertEquals(Long.valueOf(10L), args[0]);
            return result;
        }));
    }

    /** 调用校验边界，避免错误国际化依赖干扰业务条件断言。 */
    private void invoke(RelFilterServiceImpl service, String name, String json, boolean invalid) throws Exception {
        Method method = RelFilterServiceImpl.class.getDeclaredMethod(name, Long.class, JSONObject.class);
        method.setAccessible(true);
        try {
            method.invoke(service, 10L, JSONObject.parseObject(json));
            Assert.assertFalse("应拒绝失效条件", invalid);
        } catch (InvocationTargetException e) {
            Assert.assertTrue(invalid);
            Assert.assertTrue(e.getCause() instanceof RelFilterInvalidException);
        }
    }
}
