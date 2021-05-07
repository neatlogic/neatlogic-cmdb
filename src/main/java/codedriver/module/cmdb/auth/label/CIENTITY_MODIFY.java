/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

public class CIENTITY_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "配置项管理权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "对配置项进行添加、修改和删除，对事务进行提交和修改";
    }

    @Override
    public String getAuthGroup() {
        return "cmdb";
    }

    @Override
    public Integer getSort() {
        return 4;
    }
}
