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
