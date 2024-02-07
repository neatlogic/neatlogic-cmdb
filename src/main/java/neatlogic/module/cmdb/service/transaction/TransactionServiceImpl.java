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

package neatlogic.module.cmdb.service.transaction;

import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    @Resource
    private TransactionMapper transactionMapper;

    public List<TransactionVo> searchTransaction(TransactionVo transactionVo) {
        int rowNum = transactionMapper.searchTransactionCount(transactionVo);
        if (rowNum > 0) {
            transactionVo.setRowNum(rowNum);
            List<Long> transactionIdList = transactionMapper.searchTransactionId(transactionVo);
            if (CollectionUtils.isNotEmpty(transactionIdList)) {
                return transactionMapper.getTransactionByIdList(transactionIdList);
            }
        }
        return new ArrayList<>();
    }
}
