/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.transaction;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchTransactionApi extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public String getToken() {
        return "/cmdb/transaction/search";
    }

    @Override
    public String getName() {
        return "查询事务";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"),
        @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "配置项id"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页")})
    @Output({@Param(name = "tbodyList", explode = TransactionVo[].class), @Param(explode = BasePageVo.class)})
    @Description(desc = "查询事务接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        TransactionVo transactionVo = JSONObject.toJavaObject(jsonObj, TransactionVo.class);
        List<TransactionVo> transactionList = transactionMapper.searchTransaction(transactionVo);
        JSONObject returnObj = new JSONObject();
        if (transactionVo.getNeedPage() && CollectionUtils.isNotEmpty(transactionList)) {
            int rowNum = transactionMapper.searchTransactionCount(transactionVo);
            returnObj.put("rowNum", rowNum);
            returnObj.put("currentPage", transactionVo.getCurrentPage());
            returnObj.put("pageSize", transactionVo.getPageSize());
            returnObj.put("pageCount", PageUtil.getPageCount(rowNum, transactionVo.getPageSize()));
        }
        returnObj.put("tbodyList", transactionList);
        return returnObj;
    }

}
