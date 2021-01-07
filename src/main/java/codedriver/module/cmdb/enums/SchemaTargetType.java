package codedriver.module.cmdb.enums;

public enum SchemaTargetType {
    CI("ci"), CIENTITY("cientity"), ATTR("attr"), REL("rel");
    private final String type;

    private SchemaTargetType(String _type) {
        this.type = _type;
    }

    public String toString() {
        return type;
    }

}
