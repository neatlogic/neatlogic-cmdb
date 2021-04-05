package codedriver.module.cmdb.process.stephandler;

import codedriver.framework.process.stephandler.core.IProcessStepHandlerType;

public enum CmdbProcessStepHandlerType implements IProcessStepHandlerType {
    CIENTITYSYNC("cientitysync", "process", "配置项同步");

    private String handler;
    private String name;
    private String type;

    private CmdbProcessStepHandlerType(String _handler, String _type, String _name) {
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
