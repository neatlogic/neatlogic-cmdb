package codedriver.module.cmdb.dto.ci;

import java.io.Serializable;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class RelTypeVo implements Serializable {
	private static final long serialVersionUID = -8584740096830547423L;
	@EntityField(name = "id", type = ApiParamType.LONG)
	private Long id;
	@EntityField(name = "中文名", type = ApiParamType.STRING)
	private String name;

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

}
