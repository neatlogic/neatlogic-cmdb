/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

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
	public Integer getSort() {
		return 6;
	}

	@Override
	public List<Class<? extends AuthBase>> getIncludeAuths() {
		return Collections.singletonList(CMDB_BASE.class);
	}
}
