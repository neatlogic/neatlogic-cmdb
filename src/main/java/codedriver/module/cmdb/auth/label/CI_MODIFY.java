/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

import java.util.Arrays;
import java.util.List;

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

	@Override
	public List<Class<? extends AuthBase>> getIncludeAuths() {
		return Arrays.asList(CMDB_BASE.class, CIENTITY_MODIFY.class);
	}
}
