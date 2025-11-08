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
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
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
