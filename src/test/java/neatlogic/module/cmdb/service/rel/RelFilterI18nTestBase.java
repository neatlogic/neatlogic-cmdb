package neatlogic.module.cmdb.service.rel;

import neatlogic.framework.i18n.ModuleJsonMessageSource;
import neatlogic.framework.util.SpringContextUtil;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticApplicationContext;

import java.lang.reflect.Field;
import java.util.Locale;

/** 使用仓库真实翻译资源初始化最小国际化环境，并在测试后恢复全局状态。 */
public abstract class RelFilterI18nTestBase {
    private ApplicationContext previousContext;
    private Locale previousLocale;
    private StaticApplicationContext context;

    /** 加载本次关系过滤错误翻译，避免用翻译键替身掩盖资源缺失。 */
    @Before
    public void initializeI18n() throws Exception {
        Field field = SpringContextUtil.class.getDeclaredField("ctx");
        field.setAccessible(true);
        previousContext = (ApplicationContext) field.get(null);
        previousLocale = Locale.getDefault();
        ModuleJsonMessageSource source = new ModuleJsonMessageSource();
        context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("messageSourceAccessor", new MessageSourceAccessor(source));
        new SpringContextUtil().setApplicationContext(context);
        Locale.setDefault(Locale.CHINESE);
    }

    /** 恢复测试前的 Spring 上下文及默认语言，不影响其他测试。 */
    @After
    public void restoreI18n() throws Exception {
        Field field = SpringContextUtil.class.getDeclaredField("ctx");
        field.setAccessible(true);
        field.set(null, previousContext);
        Locale.setDefault(previousLocale);
        if (context != null) {
            context.close();
        }
    }
}
