package codedriver.module.cmdb.dto.ci;

import org.apache.commons.lang3.StringUtils;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class RelTypeVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "中文名", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "引用数", type = ApiParamType.INTEGER)
    private Integer invokeCount;

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
        if (StringUtils.isNotBlank(name)) {
            name = name.trim();
        }
        this.name = name;
    }

    public Integer getInvokeCount() {
        return invokeCount;
    }

    public void setInvokeCount(Integer invokeCount) {
        this.invokeCount = invokeCount;
    }

}
