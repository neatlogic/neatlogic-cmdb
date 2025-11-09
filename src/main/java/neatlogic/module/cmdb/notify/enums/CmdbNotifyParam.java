/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
