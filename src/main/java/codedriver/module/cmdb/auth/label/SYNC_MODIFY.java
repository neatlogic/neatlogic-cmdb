/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auth.label;

import codedriver.framework.auth.core.AuthBase;

import java.util.Collections;
import java.util.List;

public class SYNC_MODIFY extends AuthBase {

    @Override
    public String getAuthDisplayName() {
        return "配置项模型自动采集映射管理权限";
    }

    @Override
    public String getAuthIntroduction() {
        return "管理配置项模型的属性或关系与采集数据之间的映射关系，配置了映射关系后才能使用自动采集功能。";
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
        return Collections.singletonList(CMDB_BASE.class);
    }
}
