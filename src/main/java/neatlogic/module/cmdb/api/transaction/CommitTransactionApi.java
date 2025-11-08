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
import neatlogic.framework.cmdb.dto.transaction.TransactionGroupVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionStatusVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.exception.transaction.TransactionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class CommitTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/transaction/commit";
    }

    @Override
    public String getName() {
        return "提交事务";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", isRequired = true, type = ApiParamType.LONG, desc = "事务id")
    })
    @Output({
            @Param(explode = TransactionStatusVo[].class)
    })
    @Description(desc = "提交事务接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("id");
        TransactionGroupVo transactionGroup = transactionMapper.getTransactionGroupByTransactionId(transactionId);
        if (transactionGroup != null) {
            List<TransactionVo> transactionList = transactionMapper.getTransactionByGroupId(transactionGroup.getId());
            transactionGroup.setTransactionList(transactionList);
        } else {
            TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
            if (transactionVo == null) {
                throw new TransactionNotFoundException(transactionId);
            }
            transactionGroup = new TransactionGroupVo();
            transactionGroup.addTransaction(transactionVo);
        }
        ciEntityService.commitTransactionGroup(transactionGroup);
        return null;
    }


}
