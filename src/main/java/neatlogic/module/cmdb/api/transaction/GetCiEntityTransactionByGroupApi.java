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
import neatlogic.framework.cmdb.dto.transaction.TransactionDetailVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.transaction.TransactionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityTransactionByGroupApi extends PrivateApiComponentBase {

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private TransactionService transactionService;


    @Override
    public String getToken() {
        return "/cmdb/cientitytransactiongroup/get";
    }

    @Override
    public String getName() {
        return "根据事务组获取事务详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "transactionGroupId", isRequired = true, type = ApiParamType.LONG, desc = "事务组id")})
    @Output({@Param(explode = TransactionDetailVo.class, desc = "事务信息及详细修改信息")})
    @Description(desc = "根据事务组获取事务详细信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionGroupId = jsonObj.getLong("transactionGroupId");
        List<TransactionVo> transactionList = transactionMapper.getTransactionByGroupId(transactionGroupId);
        return transactionService.getTransactionDetailList(transactionList);
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}
