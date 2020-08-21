package codedriver.module.cmdb.dto.prop;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.constvalue.PropHandlerType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class PropVo extends BasePageVo {
    @JSONField(serialize = false)
    private transient String keyword;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "英文名称，唯一", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "中文名称，唯一", type = ApiParamType.STRING)
    private String label;
    @EntityField(name = "说明", type = ApiParamType.STRING)
    private String description;
    @EntityField(name = "控件", type = ApiParamType.STRING)
    private String handler;
    @EntityField(name = "控件名称", type = ApiParamType.STRING)
    private String handlerName;
    @EntityField(name = "引用次数", type = ApiParamType.INTEGER)
    private int invokeCount;
    @EntityField(name = "配置", type = ApiParamType.JSONOBJECT)
    private JSONObject config;
    @EntityField(name = "图标", type = ApiParamType.STRING)
    private String icon;
    @JSONField(serialize = false)
    private transient String configStr;

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(String configStr) {
        try {
            this.config = JSONObject.parseObject(configStr);
        } catch (Exception ex) {

        }
    }

    public String getConfigStr() {
        if (StringUtils.isBlank(configStr) && config != null) {
            configStr = config.toJSONString();
        }
        return configStr;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getHandlerName() {
        if (StringUtils.isBlank(handlerName) && StringUtils.isNotBlank(handler)) {
            handlerName = PropHandlerType.getText(handler);
        }
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public int getInvokeCount() {
        return invokeCount;
    }

    public void setInvokeCount(int invokeCount) {
        this.invokeCount = invokeCount;
    }

    public String getIcon() {
        if (StringUtils.isBlank(icon) && StringUtils.isNotBlank(handler)) {
            icon = PropHandlerType.getIcon(handler);
        }
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

}
