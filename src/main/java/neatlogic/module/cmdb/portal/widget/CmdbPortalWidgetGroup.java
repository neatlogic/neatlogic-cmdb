/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.cmdb.portal.widget;

import neatlogic.framework.portal.widget.core.IPortalWidgetGroup;

public enum CmdbPortalWidgetGroup implements IPortalWidgetGroup {
    cmdbGroup1("cmdbGroup1", "配置管理分组1", 1),
    cmdbGroup2("cmdbGroup2", "配置管理分组2", 2),
    ;

    private final String value;
    private final String text;
    private final Integer sort;

    CmdbPortalWidgetGroup(String value, String text, Integer sort) {
        this.value = value;
        this.text = text;
        this.sort = sort;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public Integer getSort() {
        return this.sort;
    }
}
