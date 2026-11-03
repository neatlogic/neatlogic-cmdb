package neatlogic.module.cmdb.mq.subscribe;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.mq.core.SubscribeHandlerBase;
import neatlogic.framework.mq.dto.SubscribeVo;
import neatlogic.framework.util.$;
import neatlogic.module.cmdb.service.cientity.InformantAlertService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

/** 消费 Informant 服务状态，格式错误直接跳过，数据库异常交给 Kafka 重投。 */
@Component
public class InformantAlertSubscribe extends SubscribeHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(InformantAlertSubscribe.class);
    @Resource
    private InformantAlertService informantAlertService;

    /** 使用已明确归属的消息快照，不读取 Informant 或尝试猜测配置项关联。 */
    @Override
    protected void myOnMessage(SubscribeVo subscribeVo, Object message) {
        CiEntityAlertVo alert;
        boolean recovered;
        try {
            if (message == null) {
                return;
            }
            JSONObject data = JSON.parseObject(message.toString());
            if (data == null || !"services".equals(data.get("category"))) {
                return;
            }
            if (!"informant".equals(data.get("source"))) {
                logger.warn("跳过来源缺失或不匹配的 Informant 服务消息，订阅：{}", subscribeVo.getName());
                return;
            }
            if (!TenantContext.get().getTenantUuid().equals(data.get("tenant_uuid"))) {
                logger.warn("跳过租户缺失或不匹配的 Informant 消息，订阅：{}", subscribeVo.getName());
                return;
            }
            String uuid = requiredString(data, "alert_uuid");
            if (!uuid.matches("[0-9a-f]{32}")) {
                throw new IllegalArgumentException("无效的告警标识");
            }
            String state = requiredString(data, "state");
            recovered = "Healthy".equalsIgnoreCase(state);
            alert = new CiEntityAlertVo();
            alert.setAlertUuid(uuid);
            alert.setSource("informant");
            alert.setMetricValue(StringUtils.left(state, 255));
            // 恢复只依赖 UUID，不解析恢复消息中已失效的配置项或指标字段。
            if (!recovered) {
                Object entityId = data.get("cientity_id");
                if (entityId != null) {
                    long id = exactLong(entityId);
                    if (id <= 0) {
                        throw new IllegalArgumentException("配置项ID必须为正整数或空");
                    }
                    alert.setCiEntityId(id);
                }
                alert.setIp(StringUtils.left(data.getString("ip"), 50));
                alert.setMetricName(StringUtils.left(data.getString("obj_path"), 255));
                if (StringUtils.isBlank(alert.getMetricName())) {
                    alert.setMetricName(StringUtils.left(data.getString("obj_id"), 255));
                }
                alert.setAlertMessage(StringUtils.left(data.getString("title"), 500));
                alert.setAlertTime(new Date());
                if (data.get("timestamp") != null) {
                    long timestamp = exactLong(data.get("timestamp"));
                    // 沿用 Informant 展示约定，十位以内为秒，较长时间戳为毫秒。
                    if (String.valueOf(timestamp).length() <= 10) {
                        timestamp = Math.multiplyExact(timestamp, 1000L);
                    }
                    alert.setAlertTime(new Date(timestamp));
                }
            }
        } catch (RuntimeException ex) {
            // 校验和解析在事务调用外完成，不能把数据库暂时不可用误认为坏消息。
            logger.warn("跳过格式错误的 Informant 消息，订阅：{}，原因：{}", subscribeVo.getName(), ex.getMessage());
            return;
        }
        informantAlertService.synchronizeAlert(alert, recovered);
    }

    /** 严格读取消息字符串，旧消息不会被隐式转换为新协议。 */
    private String requiredString(JSONObject data, String field) {
        Object value = data.get(field);
        if (!(value instanceof String) || StringUtils.isBlank((String) value)) {
            throw new IllegalArgumentException("缺少字符串字段：" + field);
        }
        return (String) value;
    }

    /** 拒绝小数、溢出和复合类型，避免配置项 ID 被静默截断。 */
    private long exactLong(Object value) {
        if (!(value instanceof Number) && !(value instanceof String)) {
            throw new IllegalArgumentException("整数格式错误");
        }
        return new BigDecimal(value.toString()).longValueExact();
    }

    /** 返回订阅管理中使用的稳定处理器标识。 */
    @Override
    public String getName() { return "CMDB_INFORMANT_ALERT_SUBSCRIBE"; }

    /** 按用户语言显示手动订阅处理器名称。 */
    @Override
    public String getLabel() { return $.t("cmdb.alert.informantsubscribe"); }
}
