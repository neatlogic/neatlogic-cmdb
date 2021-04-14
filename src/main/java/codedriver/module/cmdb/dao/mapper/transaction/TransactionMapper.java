/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.transaction;

import codedriver.framework.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper {

    public TransactionVo getTransactionById(Long transactionId);

    public List<TransactionVo> searchTransaction(TransactionVo transactionVo);

    public int searchTransactionCount(TransactionVo transactionVo);

    public List<CiEntityTransactionVo>
    getCiEntityTransactionByTransactionIdList(@Param("transactionIdList") List<Long> transactionIdList, @Param("ciEntityId") Long ciEntityId);

    public List<AttrEntityTransactionVo> getAttrEntityTransactionByTransactionIdAndCiEntityId(
            @Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    public List<RelEntityTransactionVo> getRelEntityTransactionByTransactionIdAndCiEntityId(
            @Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    public CiEntityTransactionVo getCiEntityTransactionByTransactionIdAndCiEntityId(
            @Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    public int updateTransactionStatus(TransactionVo transactionVo);

    public int insertRelEntityTransaction(RelEntityTransactionVo relEntityTransactionVo);

    public int insertAttrEntityTransaction(AttrEntityTransactionVo attrEntityTransactionVo);

    public int insertCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    public int insertTransactionGroup(@Param("transactionGroupId") Long transactionGroupId,
                                      @Param("transactionId") Long transactionId);

    public int insertTransaction(TransactionVo transactionVo);

    public int deleteTransactionByCiId(Long ciId);
}
