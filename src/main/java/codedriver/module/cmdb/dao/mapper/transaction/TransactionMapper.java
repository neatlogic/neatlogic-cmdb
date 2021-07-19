/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.transaction;

import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper {

    TransactionVo getTransactionById(Long transactionId);

    List<TransactionVo> searchTransaction(TransactionVo transactionVo);

    int searchTransactionCount(TransactionVo transactionVo);

    List<CiEntityTransactionVo> getCiEntityTransactionByTransactionIdList(@Param("transactionIdList") List<Long> transactionIdList, @Param("ciEntityId") Long ciEntityId);

    //List<AttrEntityTransactionVo> getAttrEntityTransactionByTransactionIdAndCiEntityId(@Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    //List<RelEntityTransactionVo> getRelEntityTransactionByTransactionIdAndCiEntityId(@Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    CiEntityTransactionVo getCiEntityTransactionByTransactionGroupIdAndCiEntityId(@Param("transactionGroupId") Long transactionGroupId, @Param("ciEntityId") Long ciEntityId);

    CiEntityTransactionVo getCiEntityTransactionByTransactionIdAndCiEntityId(@Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    int updateTransactionStatus(TransactionVo transactionVo);

    void updateCiEntityTransactionContent(CiEntityTransactionVo ciEntityTransactionVo);

    int insertRelEntityTransaction(RelEntityTransactionVo relEntityTransactionVo);

    // int insertAttrEntityTransaction(AttrEntityTransactionVo attrEntityTransactionVo);

    int insertCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    int insertTransactionGroup(@Param("transactionGroupId") Long transactionGroupId, @Param("transactionId") Long transactionId);

    int insertTransaction(TransactionVo transactionVo);

    int deleteTransactionByCiId(Long ciId);

    int deleteTransactionById(Long id);
}
