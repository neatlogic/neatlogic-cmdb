/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.notify.handler;

import neatlogic.framework.notify.core.INotifyPolicyHandlerGroup;

public enum CmdbNotifyGroup implements INotifyPolicyHandlerGroup {
    CMDB("cmdb", "配置管理");
    private final String value;
    private final String text;

    CmdbNotifyGroup(String value, String text) {
        this.value = value;
        this.text = text;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return text;
    }
}
