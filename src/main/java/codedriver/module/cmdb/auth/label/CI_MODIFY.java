package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class CI_MODIFY extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "配置项模型管理权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "对配置项模型进行添加、修改和删除";
	}

	@Override
	public String getAuthGroup() {
		return "cmdb";
	}

	@Override
	public Integer getSort() {
		return 2;
	}
}
