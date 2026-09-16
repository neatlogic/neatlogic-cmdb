package neatlogic.module.cmdb.service.rel;

import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException;
import neatlogic.framework.cmdb.exception.ci.RelFilterInvalidException.Reason;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/** 检查所有异常原因的真实中英文资源及保存入口参数上下文。 */
public class RelFilterExceptionI18nTest extends RelFilterI18nTestBase {
    /** 所有原因在两种语言均有翻译，不能降级显示翻译键。 */
    @Test
    public void allReasonsHaveTranslations() {
        for (Locale locale : new Locale[]{Locale.CHINESE, Locale.ENGLISH}) {
            Locale.setDefault(locale);
            for (Reason reason : Reason.values()) {
                String message = new RelFilterInvalidException(reason, "attrId").getMessage();
                Assert.assertNotEquals(reason.getKey(), message);
                Assert.assertFalse(message.contains("{0}"));
                if (Locale.ENGLISH.equals(locale)) {
                    Assert.assertFalse(message.matches(".*[\\u4e00-\\u9fff].*"));
                }
            }
        }
    }

    /** 字段、原因和异常链保持完整，中英文均能格式化动态字段名。 */
    @Test
    public void parameterContextIsTranslated() {
        RelFilterInvalidException zhCause = new RelFilterInvalidException(Reason.ID_INVALID, "filterCiId");
        RelFilterInvalidException zh = new RelFilterInvalidException("fromFilter", zhCause);
        Assert.assertEquals("过滤条件“fromFilter”无效：filterCiId必须是有效编号", zh.getMessage());
        Assert.assertSame(zhCause, zh.getCause());
        Locale.setDefault(Locale.ENGLISH);
        RelFilterInvalidException enCause = new RelFilterInvalidException(Reason.ID_INVALID, "filterCiId");
        RelFilterInvalidException en = new RelFilterInvalidException("toFilter", enCause);
        Assert.assertEquals("Invalid filter \"toFilter\": filterCiId must be a valid ID", en.getMessage());
        Assert.assertSame(enCause, en.getCause());
    }
}
