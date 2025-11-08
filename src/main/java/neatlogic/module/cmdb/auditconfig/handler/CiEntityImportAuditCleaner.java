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

package neatlogic.module.cmdb.auditconfig.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auditconfig.core.AuditCleanerBase;
import neatlogic.framework.healthcheck.dao.mapper.DatabaseFragmentMapper;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CiEntityImportAuditCleaner extends AuditCleanerBase {
    @Resource
    private ImportMapper importMapper;
    @Resource
    private DatabaseFragmentMapper databaseFragmentMapper;

    @Override
    public String getName() {
        return "CIENTITY-IMPORT-AUDIT";
    }

    @Override
    protected void myClean(int dayBefore) {
        importMapper.deleteAuditByDayBefore(dayBefore);
        databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "cmdb_import_audit");
    }
}
