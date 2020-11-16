package codedriver.module.cmdb.process.stephandler;

import codedriver.framework.process.constvalue.IProcessStepHandlerType;

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

    public static String getHandler(String _handler) {
        for (CmdbProcessStepHandlerType s : CmdbProcessStepHandlerType.values()) {
            if (s.getHandler().equals(_handler)) {
                return s.getHandler();
            }
        }
        return null;
    }

    public static String getName(String _handler) {
        for (CmdbProcessStepHandlerType s : CmdbProcessStepHandlerType.values()) {
            if (s.getHandler().equals(_handler)) {
                return s.getName();
            }
        }
        return "";
    }

    public static String getType(String _handler) {
        for (CmdbProcessStepHandlerType s : CmdbProcessStepHandlerType.values()) {
            if (s.getHandler().equals(_handler)) {
                return s.getType();
            }
        }
        return "";
    }

    public String getType() {
        return type;
    }

}
