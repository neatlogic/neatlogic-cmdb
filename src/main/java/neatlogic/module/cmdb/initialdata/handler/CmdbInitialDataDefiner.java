/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
