package neatlogic.module.cmdb.service.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.util.SpringContextUtil;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        StaticMessageSource source = new StaticMessageSource();
        for (String language : new String[]{"zh", "en"}) {
            Path path = Paths.get("../neatlogic-resources/localconfig/i18n/language_" + language + ".json");
            JSONObject translations = JSONObject.parseObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            Locale locale = Locale.forLanguageTag(language);
            for (String key : translations.keySet()) {
                if (key.startsWith("exception.cmdb.relfilterinvalidexception.")) {
                    source.addMessage(key, locale, translations.getString(key));
                }
            }
        }
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
