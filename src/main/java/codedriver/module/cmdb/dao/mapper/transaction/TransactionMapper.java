package codedriver.module.cmdb.dao.mapper.transaction;

import java.util.List;

import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionVo;

public interface TransactionMapper {
    public List<AttrEntityTransactionVo> getAttrEntityTransactionByTransactionId(Long transactionId);

    public List<CiEntityTransactionVo> getCiEntityTransactionByTransactionId(Long transactionId);

    public int updateTransactionStatus(TransactionVo transactionVo);

    public int insertAttrEntityTransaction(AttrEntityTransactionVo attrEntityTransactionVo);

    public int insertCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    public int insertTransaction(TransactionVo transactionVo);
}
