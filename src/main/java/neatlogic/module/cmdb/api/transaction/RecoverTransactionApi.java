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
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.TransactionStatus;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.transaction.TransactionAuthException;
import neatlogic.framework.cmdb.exception.transaction.TransactionStatusIrregularException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class RecoverTransactionApi extends PrivateApiComponentBase {

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private CiEntityService ciEntityService;


    @Override
    public String getToken() {
        return "/cmdb/transaction/recover";
    }

    @Override
    public String getName() {
        return "nmcat.recovertransactionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", isRequired = true, type = ApiParamType.LONG, desc = "term.cmdb.transactionid")})
    @Description(desc = "nmcat.recovertransactionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("id");
        TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
        if (transactionVo.getStatus().equals(TransactionStatus.UNCOMMIT.getValue())) {
            throw new TransactionStatusIrregularException(TransactionStatus.UNCOMMIT);
        } else if (transactionVo.getStatus().equals(TransactionStatus.RECOVER.getValue())) {
            throw new TransactionStatusIrregularException(TransactionStatus.RECOVER);
        }
        if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(transactionVo.getCiId()).checkCiEntityIsInGroup(transactionVo.getCiEntityId(), GroupType.MAINTAIN).check()) {
            throw new TransactionAuthException();
        }
        try {
            if (transactionVo.getTransactionGroupId() == null) {
                ciEntityService.recoverCiEntity(transactionVo);
            } else {
                ciEntityService.recoverTransactionGroup(transactionVo.getTransactionGroupId());
            }
        } catch (Exception ex) {
            transactionVo.setError(ex.getMessage());
            transactionMapper.updateTransactionStatus(transactionVo);
            throw ex;
        }
        return null;
    }

}