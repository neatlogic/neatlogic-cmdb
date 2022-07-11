/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.initialdata.handler;

import codedriver.framework.initialdata.core.IInitialDataDefiner;

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
                "cmdb_resourcecenter_config"
        };
    }
}
