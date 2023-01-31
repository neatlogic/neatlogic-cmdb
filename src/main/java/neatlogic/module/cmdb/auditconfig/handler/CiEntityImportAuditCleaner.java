/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
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
