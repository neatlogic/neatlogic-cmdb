/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.auditconfig.handler;

import codedriver.framework.auditconfig.core.AuditCleanerBase;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TransactionAuditCleaner extends AuditCleanerBase {
    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public String getName() {
        return "CMDB-TRANSACTION";
    }

    @Override
    protected void myClean(int dayBefore) {
        transactionMapper.deleteTransactionByDayBefore(dayBefore);
    }
}
