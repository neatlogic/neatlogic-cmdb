package codedriver.module.cmdb.dto.transaction;

import java.util.ArrayList;
import java.util.List;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class TransactionGroupVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "事务id", type = ApiParamType.JSONARRAY)
    private List<Long> transactionIdList;

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Long> getTransactionIdList() {
        return transactionIdList;
    }

    public void setTransactionIdList(List<Long> transactionIdList) {
        this.transactionIdList = transactionIdList;
    }

    public void addTransactionId(Long transactionId) {
        if (this.transactionIdList == null) {
            this.transactionIdList = new ArrayList<>();
        }
        if (!this.transactionIdList.contains(transactionId)) {
            this.transactionIdList.add(transactionId);
        }
    }
}
