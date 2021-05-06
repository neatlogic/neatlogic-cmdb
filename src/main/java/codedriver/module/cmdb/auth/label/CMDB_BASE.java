package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class CMDB_BASE extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "配置管理基础权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "查看配置";
	}

	@Override
	public String getAuthGroup() {
		return "cmdb";
	}

	@Override
	public Integer sort() {
		return 1;
	}
}
