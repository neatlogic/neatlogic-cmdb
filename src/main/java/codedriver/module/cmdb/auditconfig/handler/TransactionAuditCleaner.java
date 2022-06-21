/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auditconfig.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auditconfig.core.AuditCleanerBase;
import codedriver.framework.healthcheck.dao.mapper.DatabaseFragmentMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TransactionAuditCleaner extends AuditCleanerBase {
    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private DatabaseFragmentMapper databaseFragmentMapper;

    @Override
    public String getName() {
        return "CMDB-TRANSACTION";
    }

    @Override
    protected void myClean(int dayBefore) {
        transactionMapper.deleteTransactionByDayBefore(dayBefore);
        databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "cmdb_transaction");
        databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "cmdb_cientity_transaction");
        databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "cmdb_transactiongroup");
    }
}
