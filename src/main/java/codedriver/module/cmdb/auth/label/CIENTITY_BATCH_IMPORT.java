/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class CIENTITY_BATCH_IMPORT extends AuthBase {

	@Override
	public String getAuthDisplayName() {
		return "批量导入配置项权限";
	}

	@Override
	public String getAuthIntroduction() {
		return "批量导入配置项";
	}

	@Override
	public String getAuthGroup() {
		return "cmdb";
	}

	@Override
	public Integer getSort() {
		return 3;
	}
}
