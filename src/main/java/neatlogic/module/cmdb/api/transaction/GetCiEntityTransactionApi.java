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
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.RelEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionDetailVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.transaction.TransactionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityTransactionApi extends PrivateApiComponentBase {

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private TransactionService transactionService;


    @Override
    public String getToken() {
        return "/cmdb/cientitytransaction/get";
    }

    @Override
    public String getName() {
        return "获取配置项事务详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", isRequired = true, type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "ciEntityId", isRequired = true, type = ApiParamType.LONG, desc = "配置项id"),
            @Param(name = "transactionId", isRequired = true, type = ApiParamType.LONG, desc = "事务id")})
    @Output({@Param(explode = TransactionDetailVo.class, desc = "事务信息及详细修改信息")})
    @Description(desc = "获取配置项事务详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long transactionId = jsonObj.getLong("transactionId");
        Long ciId = jsonObj.getLong("ciId");
        TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);

        CiEntityTransactionVo ciEntityTransactionVo = transactionMapper.getCiEntityTransactionByTransactionIdAndCiEntityId(transactionId, ciEntityId);
        return transactionService.getTransactionDetail(transactionVo, ciEntityTransactionVo, ciId);
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    private static String getRelGroupKey(RelEntityTransactionVo relEntityTransactionVo) {
        //如果值为空则使用空格作占位符
        if (relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
            return (relEntityTransactionVo.getRelId() != null ? relEntityTransactionVo.getRelId() : " ") + "#" + (relEntityTransactionVo.getDirection() != null ? relEntityTransactionVo.getDirection() : " ") + "#"
                    + (relEntityTransactionVo.getToLabel() != null ? relEntityTransactionVo.getToLabel() : " ") + "#" + (relEntityTransactionVo.getTypeId() != null ? relEntityTransactionVo.getTypeId() : " ");
        } else {
            return (relEntityTransactionVo.getRelId() != null ? relEntityTransactionVo.getRelId() : " ") + "#" + (relEntityTransactionVo.getDirection() != null ? relEntityTransactionVo.getDirection() : " ") + "#"
                    + (relEntityTransactionVo.getFromLabel() != null ? relEntityTransactionVo.getFromLabel() : " ") + "#" + (relEntityTransactionVo.getTypeId() != null ? relEntityTransactionVo.getTypeId() : " ");
        }
    }
}
