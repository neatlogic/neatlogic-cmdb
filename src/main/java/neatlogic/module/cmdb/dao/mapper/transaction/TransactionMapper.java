/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

    List<TransactionVo> getTransactionByIdList(@Param("idList") List<Long> idList);

    List<Long> searchTransactionId(TransactionVo transactionVo);

    int searchTransactionCount(TransactionVo transactionVo);

    List<CiEntityTransactionVo> getCiEntityTransactionByTransactionIdList(@Param("transactionIdList") List<Long> transactionIdList, @Param("ciEntityId") Long ciEntityId);

    List<CiEntityTransactionVo> getCiEntityTransactionByTransactionGroupIdAndCiEntityId(@Param("transactionGroupId") Long transactionGroupId, @Param("ciEntityId") Long ciEntityId);

    CiEntityTransactionVo getCiEntityTransactionByTransactionIdAndCiEntityId(@Param("transactionId") Long transactionId, @Param("ciEntityId") Long ciEntityId);

    CiEntityTransactionVo getCiEntityTransactionByTransactionId(Long transactionId);

    int updateTransactionStatus(TransactionVo transactionVo);

    void updateCiEntityTransactionContent(CiEntityTransactionVo ciEntityTransactionVo);

    int insertCiEntityTransaction(CiEntityTransactionVo ciEntityTransactionVo);

    int insertTransactionGroup(@Param("transactionGroupId") Long transactionGroupId, @Param("transactionId") Long transactionId);

    int insertTransaction(TransactionVo transactionVo);

    int deleteTransactionByCiId(Long ciId);

    int deleteTransactionById(Long id);

    int deleteTransactionByDayBefore(int dayBefore);
}
