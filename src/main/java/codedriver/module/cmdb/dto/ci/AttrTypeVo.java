/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dto.ci;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.module.cmdb.enums.AttrType;

public class AttrTypeVo {
    @EntityField(name = "唯一标识", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "名称", type = ApiParamType.STRING)
    private String label;
    @EntityField(name = "图标", type = ApiParamType.STRING)
    private String icon;
    @EntityField(name = "是否需要关联目标模型", type = ApiParamType.BOOLEAN)
    private boolean needTargetCi;
    @EntityField(name = "是否需要配置页面", type = ApiParamType.BOOLEAN)
    private boolean needConfig;
    @EntityField(name = "是否需要一整行显示编辑组件", type = ApiParamType.BOOLEAN)
    private boolean needWholeRow;

    public AttrTypeVo(AttrType attrType) {
        this.setName(attrType.getName());
        this.setLabel(attrType.getText());
        this.setNeedConfig(attrType.isNeedConfig());
        this.setNeedWholeRow(attrType.isNeedWholeRow());
        this.setNeedTargetCi(attrType.isNeedTargetCi());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean getNeedTargetCi() {
        return needTargetCi;
    }

    public void setNeedTargetCi(boolean needTargetCi) {
        this.needTargetCi = needTargetCi;
    }

    public boolean getNeedConfig() {
        return needConfig;
    }

    public void setNeedConfig(boolean needConfig) {
        this.needConfig = needConfig;
    }


    public boolean isNeedWholeRow() {
        return needWholeRow;
    }

    public void setNeedWholeRow(boolean needWholeRow) {
        this.needWholeRow = needWholeRow;
    }
}
