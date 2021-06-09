/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

public class RESOURCECENTER_ACCOUNT_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "资源中心-账号管理权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "对资源中心的账号进行新增、修改、删除、编辑操作";
    }

    @Override
    public String getAuthGroup() {
        return "cmdb";
    }

    @Override
    public Integer getSort() {
        return 8;
    }

    @Override
    public List<Class<? extends AuthBase>> getIncludeAuths() {
        return Collections.singletonList(CMDB_BASE.class);
    }
}
