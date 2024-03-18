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

package neatlogic.module.cmdb.api.transaction;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class GetBrotherTransactionCountApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;


    @Override
    public String getToken() {
        return "/cmdb/transaction/brothercount";
    }

    @Override
    public String getName() {
        return "获取同组事务数量";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "transactionGroupId", isRequired = true, type = ApiParamType.LONG, desc = "事务组id"),
            @Param(name = "transactionId", isRequired = true, type = ApiParamType.LONG, desc = "事务id")
    })
    @Output({
            @Param(name = "数量", type = ApiParamType.INTEGER)
    })
    @Description(desc = "获取同组事务数量接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("transactionId");
        Long transactionGroupId = jsonObj.getLong("transactionGroupId");
        return transactionMapper.getBrotherTransactionCountByTransactionGroupId(transactionId, transactionGroupId);
    }


}
