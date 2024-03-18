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
