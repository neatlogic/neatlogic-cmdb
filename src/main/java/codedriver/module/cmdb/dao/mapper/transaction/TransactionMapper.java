package codedriver.module.cmdb.dao.mapper.transaction;

import codedriver.module.cmdb.dto.transaction.TransactionVo;

public interface TransactionMapper {
    public int insertTransaction(TransactionVo transactionVo);
}
