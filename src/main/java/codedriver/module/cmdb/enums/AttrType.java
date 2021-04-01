/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.enums;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.IEnum;
import codedriver.framework.restful.annotation.EntityField;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public enum AttrType implements IEnum {
    TEXT("text", "文本框", "ts-code", true, false, false, false),
    DATE("date", "日期", "ts-calendar", true, false, true, false),
    TEXTAREA("textarea", "文本域", "ts-textmodule", true, false, false, true),
    SELECT("select", "下拉框", "ts-list", false, true, true, false),
    //CHECKBOX("checkbox", "复选框", "ts-check-square-o", false, true, false, false, true),
    //RADIO("radio", "单选框", "ts-round-s", true, false, true, false, true),
    FILE("file", "附件", "ts-file", false, false, true, true),
    URL("url", "链接", "ts-link", false, false, false, false),
    PASSWORD("password", "密码", "ts-eye-close", false, false, false, false),
    TABLE("table", "表格", "ts-tablechart", false, true, true, true);
    //INVOKETABLE("invoketable", "引用表格", "ts-tablechart", false, true, true, false, true);
    @EntityField(name = "唯一标识", type = ApiParamType.STRING)
    private final String name;
    @EntityField(name = "名称", type = ApiParamType.STRING)
    private final String text;
    @EntityField(name = "图标", type = ApiParamType.STRING)
    private final String icon;
    @EntityField(name = "是否允许作为引用属性", type = ApiParamType.BOOLEAN)
    private final boolean canUseForKey;
    @EntityField(name = "是否需要关联目标模型", type = ApiParamType.BOOLEAN)
    private final boolean needTargetCi;
    @EntityField(name = "是否需要配置页面", type = ApiParamType.BOOLEAN)
    private final boolean needConfig;
    @EntityField(name = "是否需要一整行显示编辑组件", type = ApiParamType.BOOLEAN)
    private final boolean needWholeRow;

    AttrType(String _name, String _text, String _icon, boolean _canUseForKey, boolean _needTargetCi, boolean _needConfig, boolean _needWholeRow) {
        this.name = _name;
        this.text = _text;
        this.icon = _icon;
        this.canUseForKey = _canUseForKey;
        this.needTargetCi = _needTargetCi;
        this.needConfig = _needConfig;
        this.needWholeRow = _needWholeRow;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isNeedTargetCi() {
        return needTargetCi;
    }

    public boolean isNeedConfig() {
        return needConfig;
    }


    public boolean isCanUseForKey() {
        return canUseForKey;
    }

    public boolean isNeedWholeRow() {
        return needWholeRow;
    }

    public static AttrType get(String name) {
        for (AttrType s : AttrType.values()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }


    @Override
    public List getValueTextList() {
        JSONArray array = new JSONArray();
        for (AttrType type : AttrType.values()) {
            array.add(new JSONObject() {
                {
                    this.put("value", type.getName());
                    this.put("text", type.getText());
                }
            });
        }
        return array;
    }
}
