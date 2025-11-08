/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.service.transaction;

import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionDetailVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;

import java.util.List;

public interface TransactionService {
    List<TransactionVo> searchTransaction(TransactionVo transactionVo);

    /**
     * 获取配置项事务详细信息
     */
    TransactionDetailVo getTransactionDetail(TransactionVo transactionVo, CiEntityTransactionVo ciEntityTransactionVo);

    List<TransactionDetailVo> getTransactionDetailList(List<TransactionVo> transactionList);
}
