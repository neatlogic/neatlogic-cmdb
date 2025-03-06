/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.notify.enums;

import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.notify.core.INotifyParam;
import neatlogic.framework.util.$;

public enum CmdbNotifyParam implements INotifyParam {
    CIID("ciId", "模型id", ParamType.NUMBER),
    CI_NAME("ciName", "模型唯一标识", ParamType.STRING),
    CI_LABEL("ciLabel", "模型名称", ParamType.STRING),
    CIENTITY_ID("ciEntityId", "配置项id", ParamType.NUMBER),
    CIENTITY_NAME("ciEntityName", "配置项名称", ParamType.STRING),
    INVALID_CIENTITY_LIST("invalidCiEntityList", "不合规配置项列表", ParamType.ARRAY, "<#if DATA.invalidCiEntityList?? && (DATA.invalidCiEntityList?size > 0)>\n" +
            "\t<#list DATA.invalidCiEntityList as item>\n" +
            "\t\t${item.name}\n" +
            "\t\t<#if item_has_next>,</#if>\n" +
            "\t</#list>\n" +
            "</#if>");
    private final String value;
    private final String text;
    private final ParamType paramType;
    private String freemarkerTemplate;

    CmdbNotifyParam(String value, String text, ParamType paramType) {
        this(value, text, paramType, null);
    }

    CmdbNotifyParam(String value, String text, ParamType paramType, String freemarkerTemplate) {
        this.value = value;
        this.text = text;
        this.paramType = paramType;
        this.freemarkerTemplate = freemarkerTemplate;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return $.t(text);
    }

    @Override
    public ParamType getParamType() {
        return paramType;
    }

    @Override
    public String getFreemarkerTemplate() {
        if (freemarkerTemplate == null && paramType != null) {
            freemarkerTemplate = paramType.getFreemarkerTemplate(value);
        }
        return freemarkerTemplate;
    }
}
