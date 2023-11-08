package neatlogic.module.cmdb.process.constvalue;

import neatlogic.framework.process.audithandler.core.IProcessTaskAuditDetailType;

public enum CmdbAuditDetailType implements IProcessTaskAuditDetailType {

    CMDBSYNCMESSAGE("cmdbsyncmessage", "CMDB配置项同步信息", "cmdbSyncMessage", "oldCmdbSyncMessage", 20, false),
    ;
    private String value;
    private String text;
    private String paramName;
    private String oldDataParamName;
    private int sort;
    private boolean needCompression;

    CmdbAuditDetailType(String value, String text, String paramName, String oldDataParamName, int sort, boolean needCompression) {
        this.value = value;
        this.text = text;
        this.paramName = paramName;
        this.oldDataParamName = oldDataParamName;
        this.sort = sort;
        this.needCompression = needCompression;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getParamName() {
        return paramName;
    }

    @Override
    public String getOldDataParamName() {
        return oldDataParamName;
    }

    @Override
    public int getSort() {
        return sort;
    }

    @Override
    public boolean getNeedCompression() {
        return needCompression;
    }
}
