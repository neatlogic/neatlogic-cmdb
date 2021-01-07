package codedriver.module.cmdb.enums;

public enum SchemaActionType {
    INSERT("insert"), UPDATE("update"), DELETE("delete");
    private final String type;

    private SchemaActionType(String _type) {
        this.type = _type;
    }

    public String toString() {
        return type;
    }

}
