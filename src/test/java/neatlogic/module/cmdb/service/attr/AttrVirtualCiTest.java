package neatlogic.module.cmdb.service.attr;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.attr.AttrNameRepeatException;
import neatlogic.framework.cmdb.exception.attr.AttrVirtualCiDeniedException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.service.rel.RelFilterI18nTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** 验证视图字段独立维护在任何写入及处理器调用之前被拒绝。 */
public class AttrVirtualCiTest extends RelFilterI18nTestBase {
    private AttrServiceImpl service;
    private final Map<Long, CiVo> models = new HashMap<>();
    private AttrVo stored;
    private int nameChecks;

    /** 注入只允许读取的替身，任何写入或表结构操作都会直接使测试失败。 */
    @Before public void setUpService() throws Exception {
        service = new AttrServiceImpl();
        CiVo physical = new CiVo(); physical.setId(1L); physical.setIsVirtual(0);
        CiVo virtual = new CiVo(); virtual.setId(2L); virtual.setIsVirtual(1);
        models.put(1L, physical); models.put(2L, virtual);
        stored = attr(2L);
        inject("ciMapper", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{CiMapper.class},
                (p, m, a) -> models.get(a[0])));
        inject("attrMapper", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AttrMapper.class}, (p, m, a) -> {
            if ("getAttrById".equals(m.getName())) { return stored; }
            if ("checkAttrNameIsRepeat".equals(m.getName())) { nameChecks++; return 1; }
            throw new AssertionError("禁止进入属性写入或后续处理：" + m.getName());
        }));
        inject("ciSchemaMapper", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{CiSchemaMapper.class},
                (p, m, a) -> { throw new AssertionError("禁止访问视图表结构"); }));
    }

    /** 虚拟模型新增和删除均在执行属性处理器前失败。 */
    @Test public void rejectsVirtualInsertAndDelete() {
        Assert.assertThrows(AttrVirtualCiDeniedException.class, () -> service.insertAttr(attr(2L)));
        Assert.assertThrows(AttrVirtualCiDeniedException.class, () -> service.deleteAttr(attr(2L)));
        Assert.assertEquals(0, nameChecks);
    }

    /** 请求伪造为普通模型时，仍按持久化属性归属拒绝更新及删除。 */
    @Test public void rejectsForgedPhysicalOwner() {
        Assert.assertThrows(AttrVirtualCiDeniedException.class, () -> service.updateAttr(attr(1L)));
        Assert.assertThrows(AttrVirtualCiDeniedException.class, () -> service.deleteAttr(attr(1L)));
    }

    /** 普通模型字段也不能被更新到虚拟模型。 */
    @Test public void rejectsVirtualTarget() {
        stored = attr(1L);
        Assert.assertThrows(AttrVirtualCiDeniedException.class, () -> service.updateAttr(attr(2L)));
    }

    /** 普通模型仍进入原有重复名称校验，不被新增的视图限制拦截。 */
    @Test public void physicalModelRetainsNameValidation() {
        Assert.assertThrows(AttrNameRepeatException.class, () -> service.insertAttr(attr(1L)));
        Assert.assertEquals(1, nameChecks);
    }

    /** 模型缺失沿用明确的业务异常。 */
    @Test public void rejectsMissingModel() {
        Assert.assertThrows(CiNotFoundException.class, () -> service.insertAttr(attr(99L)));
    }

    /** 专用异常使用真实中英文资源，避免向用户显示翻译键。 */
    @Test public void translatesVirtualModelError() {
        Assert.assertTrue(new AttrVirtualCiDeniedException().getMessage().contains("视图配置"));
        setLocale(java.util.Locale.ENGLISH);
        Assert.assertTrue(new AttrVirtualCiDeniedException().getMessage().contains("view configuration"));
    }

    /** 创建携带显式身份的测试属性，不触发额外 ID 生成。 */
    private AttrVo attr(Long ciId) {
        AttrVo attr = new AttrVo(); attr.setId(10L); attr.setCiId(ciId); attr.setName("field");
        return attr;
    }

    /** 仅在测试中注入 Mapper，生产对象不增加测试装配接口。 */
    private void inject(String name, Object value) throws Exception {
        Field field = AttrServiceImpl.class.getDeclaredField(name); field.setAccessible(true); field.set(service, value);
    }
}
