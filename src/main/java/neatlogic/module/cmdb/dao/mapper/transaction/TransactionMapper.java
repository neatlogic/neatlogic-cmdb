/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.transaction;

import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper {
    int getTransactionCountByGroupId(Long transactionGroupId);

    List<TransactionVo> getUnCommitTransactionByCiEntityIdAndAction(@Param("ciEntityId") Long ciEntityId, @Param("action") String action);

    List<Long> getBrotherTransactionIdByTransactionGroupId(@Param("transactionId") Long transactionId, @Param("transactionGroupId") Long transactionGroupId);

    int getBrotherTransactionCountByTransactionGroupId(@Param("transactionId") Long transactionId, @Param("transactionGroupId") Long transactionGroupId);

    List<TransactionVo> getTransactionByGroupId(Long transactionGroupId);

    TransactionGroupVo getTransactionGroupByTransactionId(Long transactionId);

    TransactionVo getTransactionById(Long transactionId);

    List<TransactionVo> searchTransaction(TransactionVo transactionVo);

    int searchTransactionCount(TransactionVo transactionVo);

    List<CiEntityTransactionVo> getCiEntityTransactionByTransactionIdList(@Param("transactionIdList") List<Long> transactionIdList, @Param("ciEntityId") Long ciEntityId);

    List<CiEntityTransactionVo> getCiEntityTransactionByTransactionGroupIdAndCiEntityId(@Param("transactionGroupId") Long transactionGroupId, @Param("ciEntityId") Long ciEntityId);

    CiEntityTransactionVo getCiEntityTransactionByTransactionIdAndCiEntityId(@Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    int updateTransactionStatus(TransactionVo transactionVo);

    void updateCiEntityTransactionContent(CiEntityTransactionVo ciEntityTransactionVo);

    int insertCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    int insertTransactionGroup(@Param("transactionGroupId") Long transactionGroupId, @Param("transactionId") Long transactionId);

    int insertTransaction(TransactionVo transactionVo);

    int deleteTransactionByCiId(Long ciId);

    int deleteTransactionById(Long id);

    int deleteTransactionByDayBefore(int dayBefore);
}
