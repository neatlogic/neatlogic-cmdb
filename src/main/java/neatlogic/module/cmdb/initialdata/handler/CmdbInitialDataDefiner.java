/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
