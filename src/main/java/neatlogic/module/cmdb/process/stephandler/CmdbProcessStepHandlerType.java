/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.process.stephandler;

import neatlogic.framework.process.stephandler.core.IProcessStepHandlerType;

public enum CmdbProcessStepHandlerType implements IProcessStepHandlerType {
    CIENTITYSYNC("cientitysync", "process", "配置项同步");

    private final String handler;
    private final String name;
    private final String type;

    CmdbProcessStepHandlerType(String _handler, String _type, String _name) {
        this.handler = _handler;
        this.type = _type;
        this.name = _name;
    }

    public String getHandler() {
        return handler;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

}
