/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    List<TransactionVo> getTransactionByIdList(@Param("idList") List<Long> idList);

    List<Long> searchTransactionId(TransactionVo transactionVo);

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
