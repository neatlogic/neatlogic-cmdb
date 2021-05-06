package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class RELTYPE_MODIFY extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "关系类型修改权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "对关系类型进行添加、修改和删除";
	}

	@Override
	public String getAuthGroup() {
		return "cmdb";
	}

	@Override
	public Integer sort() {
		return 6;
	}
}
