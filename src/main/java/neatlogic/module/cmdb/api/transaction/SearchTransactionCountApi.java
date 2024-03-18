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

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchTransactionCountApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public String getToken() {
        return "/cmdb/transaction/search/count";
    }

    @Override
    public String getName() {
        return "查询事务数量";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "配置项id"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "commited,uncommit,recover,expired", desc = "状态"),
            @Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "事务组id")})
    @Output({@Param(name = "Return", type = ApiParamType.INTEGER, desc = "事务数量")})
    @Description(desc = "查询事务数量接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        TransactionVo transactionVo = JSONObject.toJavaObject(jsonObj, TransactionVo.class);
        return transactionMapper.searchTransactionCount(transactionVo);
    }

}
