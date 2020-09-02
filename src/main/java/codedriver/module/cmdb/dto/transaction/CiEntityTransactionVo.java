package codedriver.module.cmdb.dto.transaction;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;

public class CiEntityTransactionVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "操作", type = ApiParamType.ENUM, member = TransactionActionType.class)
    private String action;
    @JSONField(serialize = false)
    private transient List<AttrEntityTransactionVo> attrEntityTransactionList;

    public CiEntityTransactionVo() {

    }

    public CiEntityTransactionVo(CiEntityVo ciEntityVo) {
        ciId = ciEntityVo.getCiId();
        ciEntityId = ciEntityVo.getId();
    }

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCiId() {
        return ciId;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
    }

    public Long getCiEntityId() {
        return ciEntityId;
    }

    public void setCiEntityId(Long ciEntityId) {
        this.ciEntityId = ciEntityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public List<AttrEntityTransactionVo> getAttrEntityTransactionList() {
        return attrEntityTransactionList;
    }

    public void setAttrEntityTransactionList(List<AttrEntityTransactionVo> attrEntityTransactionList) {
        this.attrEntityTransactionList = attrEntityTransactionList;
    }

}
