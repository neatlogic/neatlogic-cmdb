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

import neatlogic.framework.portal.widget.core.IPortalWidget;
import neatlogic.framework.portal.widget.core.IPortalWidgetGroup;

public enum CmdbPortalWidget implements IPortalWidget {
    cmdbAssetHealth("cmdbAssetHealth", "资产健康概览", 1, CmdbPortalWidgetGroup.cmdbGroup1),
    cmdbAbnormalAsset("cmdbAbnormalAsset", "异常资产", 2, CmdbPortalWidgetGroup.cmdbGroup1),
    cmdbPendingChange("cmdbPendingChange", "待处理变更", 3, CmdbPortalWidgetGroup.cmdbGroup2),
    cmdbAssetDistribution("cmdbAssetDistribution", "资产类型与健康分布", 4, CmdbPortalWidgetGroup.cmdbGroup2),
    ;
    private final String value;
    private final String text;
    private final Integer sort;
    private final IPortalWidgetGroup group;

    CmdbPortalWidget(String value, String text, Integer sort, IPortalWidgetGroup group) {
        this.value = value;
        this.text = text;
        this.sort = sort;
        this.group = group;
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

    @Override
    public IPortalWidgetGroup getGroup() {
        return this.group;
    }
}
