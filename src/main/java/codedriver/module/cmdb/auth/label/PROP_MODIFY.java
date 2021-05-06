package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class PROP_MODIFY extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "基础属性管理权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "对基础属性进行添加、修改和删除";
	}

	@Override
	public String getAuthGroup() {
		return "cmdb";
	}

	@Override
	public Integer sort() {
		return 5;
	}
}
