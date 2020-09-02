package codedriver.module.cmdb.dto.transaction;

import java.util.List;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;

public class AttrEntityTransactionVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;// 由于需要在SQL批量写入，所以这里使用数据库自增id
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "属性id", type = ApiParamType.LONG)
    private Long attrId;
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "值列表", type = ApiParamType.JSONARRAY)
    private List<String> valueList;

    public AttrEntityTransactionVo() {

    }

    public AttrEntityTransactionVo(AttrEntityVo attrEntityVo) {
        ciEntityId = attrEntityVo.getCiEntityId();
        attrId = attrEntityVo.getAttrId();
        // 获取转换后的值
        valueList = attrEntityVo.getTransferValueList();
    }

    public Long getCiEntityId() {
        return ciEntityId;
    }

    public void setCiEntityId(Long ciEntityId) {
        this.ciEntityId = ciEntityId;
    }

    public Long getAttrId() {
        return attrId;
    }

    public void setAttrId(Long attrId) {
        this.attrId = attrId;
    }

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

}
