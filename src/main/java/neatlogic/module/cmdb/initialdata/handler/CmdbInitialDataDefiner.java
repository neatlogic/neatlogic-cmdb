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

package neatlogic.module.cmdb.initialdata.handler;

import neatlogic.framework.initialdata.core.IInitialDataDefiner;

public class CmdbInitialDataDefiner implements IInitialDataDefiner {
    @Override
    public String getModuleId() {
        return "cmdb";
    }

    @Override
    public String[] getTables() {
        return new String[]{
                "cmdb_ci",
                "cmdb_attr",
                "cmdb_attrexpression_rel",
                "cmdb_ci_unique",
                "cmdb_citype",
                "cmdb_rel",
                "cmdb_relgroup",
                "cmdb_reltype",
                "cmdb_relativerel",
                "cmdb_view",
                "cmdb_viewconst",
                "cmdb_validator",
                "cmdb_sync_ci_collection",
                "cmdb_sync_mapping",
                "cmdb_sync_policy",
                "cmdb_sync_schedule",
                "cmdb_sync_unique",
                "cmdb_resourcecenter_entity"
        };
    }
}
