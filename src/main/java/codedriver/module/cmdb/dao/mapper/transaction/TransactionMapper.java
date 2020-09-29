package codedriver.module.cmdb.dao.mapper.transaction;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionVo;

public interface TransactionMapper {

    public TransactionVo getTransactionById(Long transactionId);

    public List<TransactionVo> searchTransaction(TransactionVo transactionVo);

    public int searchTransactionCount(TransactionVo transactionVo);

    public List<AttrEntityTransactionVo> getAttrEntityTransactionByTransactionId(Long transactionId);

    public List<AttrEntityTransactionVo> getAttrEntityTransactionByTransactionIdAndCiEntityId(
        @Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    public List<RelEntityTransactionVo> getRelEntityTransactionByTransactionIdAndCiEntityId(
        @Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    public List<CiEntityTransactionVo> getCiEntityTransactionByTransactionId(Long transactionId);

    public CiEntityTransactionVo getCiEntityTransactionByTransactionIdAndCiEntityId(
        @Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    public int updateTransactionStatus(TransactionVo transactionVo);

    public int insertRelEntityTransaction(RelEntityTransactionVo relEntityTransactionVo);

    public int insertAttrEntityTransaction(AttrEntityTransactionVo attrEntityTransactionVo);

    public int insertCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    public int insertTransaction(TransactionVo transactionVo);

    public int deleteTransactionByCiId(Long ciId);
}
