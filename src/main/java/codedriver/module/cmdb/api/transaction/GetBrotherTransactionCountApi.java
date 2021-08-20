/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.transaction;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import com.alibaba.fastjson.JSONObject;
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
