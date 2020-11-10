package codedriver.module.cmdb.form.handler;

import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.process.constvalue.IProcessFormHandler;

public enum CmdbFormHandlerType implements IProcessFormHandler {
    CIENTITYSELECT("cientityselect", "配置项修改组件", "ts-m-cmdb", "form", null, null, null);

    private String handler;
    private String handlerName;
    private FormHandlerType handlerType;
    private ParamType paramType;
    private String dataType;
    private String icon;
    private String type;

    private CmdbFormHandlerType(String _handler, String _handlerName, String _icon, String _type,
        FormHandlerType _handlerType, ParamType _paramType, String _dataType) {
        this.handler = _handler;
        this.handlerName = _handlerName;
        this.icon = _icon;
        this.type = _type;
        this.handlerType = _handlerType;
        this.paramType = _paramType;
        this.dataType = _dataType;
    }

    @Override
    public String getHandler() {
        return handler;
    }

    @Override
    public String getHandlerName() {
        return handlerName;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Expression getExpression() {
        return this.paramType.getDefaultExpression();
    }

    @Override
    public ParamType getParamType() {
        return paramType;
    }

    public FormHandlerType getHandlerType() {
        return handlerType;
    }

}
