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
    public Integer sort() {
        return 4;
    }
}
