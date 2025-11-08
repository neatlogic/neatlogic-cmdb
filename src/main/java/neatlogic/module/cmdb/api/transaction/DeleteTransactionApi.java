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

package neatlogic.module.cmdb.api.transaction;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
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
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteTransactionApi extends PrivateApiComponentBase {

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;


    @Override
    public String getToken() {
        return "/cmdb/transaction/delete";
    }

    @Override
    public String getName() {
        return "nmcat.deletetransactionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", isRequired = true, type = ApiParamType.LONG, desc = "term.cmdb.transactionid"),
            @Param(name = "ciId", isRequired = true, type = ApiParamType.LONG, desc = "term.cmdb.ciid")})
    @Description(desc = "nmcat.deletetransactionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("id");
        TransactionGroupVo transactionGroup = transactionMapper.getTransactionGroupByTransactionId(transactionId);
        if (transactionGroup != null) {
            List<TransactionVo> transactionList = transactionMapper.getTransactionByGroupId(transactionGroup.getId());
            for (TransactionVo transactionVo : transactionList) {
                if (transactionVo.getStatus().equals(TransactionStatus.COMMITED.getValue())) {
                    throw new TransactionStatusIrregularException(TransactionStatus.COMMITED);
                } else if (transactionVo.getStatus().equals(TransactionStatus.RECOVER.getValue())) {
                    throw new TransactionStatusIrregularException(TransactionStatus.RECOVER);
                }
                if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(transactionVo.getCiId()).checkCiEntityIsInGroup(transactionVo.getCiEntityId(), GroupType.MAINTAIN).check()) {
                    throw new TransactionAuthException();
                }
                transactionMapper.deleteTransactionById(transactionVo.getId());
                // 解除配置项修改锁定
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setId(transactionVo.getCiEntityId());
                ciEntityVo.setIsLocked(0);
                ciEntityMapper.updateCiEntityLockById(ciEntityVo);
            }
        } else {
            TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
            if (transactionVo.getStatus().equals(TransactionStatus.COMMITED.getValue())) {
                throw new TransactionStatusIrregularException(TransactionStatus.COMMITED);
            } else if (transactionVo.getStatus().equals(TransactionStatus.RECOVER.getValue())) {
                throw new TransactionStatusIrregularException(TransactionStatus.RECOVER);
            }
            if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(transactionVo.getCiId()).checkCiEntityIsInGroup(transactionVo.getCiEntityId(), GroupType.MAINTAIN).check()) {
                throw new TransactionAuthException();
            }
            transactionMapper.deleteTransactionById(transactionVo.getId());
            // 解除配置项修改锁定
            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setId(transactionVo.getCiEntityId());
            ciEntityVo.setIsLocked(0);
            ciEntityMapper.updateCiEntityLockById(ciEntityVo);
        }
        return null;
    }

}