/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.enums;

public enum CiAttrType {
    SIMPLE("simple", "简单属性"), COMPLEX("complex", "复杂属性"), CUSTOM("custom", "自定义属性");

    private final String value;
    private final String text;

    CiAttrType(String _value, String _text) {
        this.value = _value;
        this.text = _text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static String getText(String name) {
        for (CiAttrType s : CiAttrType.values()) {
            if (s.getValue().equals(name)) {
                return s.getText();
            }
        }
        return "";
    }
}
