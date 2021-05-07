/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class CMDB_BASE extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "配置管理基础权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "拥有此权限才能使用配置管理功能";
    }

    @Override
    public String getAuthGroup() {
        return "cmdb";
    }

    @Override
    public Integer getSort() {
        return 1;
    }
}
