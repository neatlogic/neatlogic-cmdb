package codedriver.module.cmdb.dto.cientity;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.elasticsearch.annotation.ESKey;
import codedriver.framework.elasticsearch.constvalue.ESKeyType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;

public class AttrEntityVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;// 由于需要在SQL批量写入，所以这里使用数据库自增id
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    @ESKey(type = ESKeyType.PKEY, name = "id")
    private Long ciEntityId;
    @EntityField(name = "属性id", type = ApiParamType.LONG)
    private Long attrId;
    @EntityField(name = "属性唯一标识", type = ApiParamType.STRING)
    private String attrName;
    @EntityField(name = "属性名称", type = ApiParamType.STRING)
    private String attrLabel;
    @EntityField(name = "属性类型", type = ApiParamType.STRING)
    private String attrType;
    @EntityField(name = "属性定义id", type = ApiParamType.LONG)
    private Long propId;
    @EntityField(name = "属性定义处理器", type = ApiParamType.STRING)
    private String propHandler;
    @EntityField(name = "属性表达式", type = ApiParamType.STRING)
    private String attrExpression;
    @JSONField(serialize = false) // 原始值，可以是任何类型，后面在拆解到valueList里
    private transient Object value;
    @EntityField(name = "入库值列表", type = ApiParamType.JSONARRAY)
    private List<String> valueList;
    @EntityField(name = "真实值列表", type = ApiParamType.JSONARRAY)
    private List<String> actualValueList;
    @EntityField(name = "生效事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "输入方式", type = ApiParamType.STRING)
    private String inputType;
    @EntityField(name = "输入方式名称", type = ApiParamType.STRING)
    private String inputTypeText;
    @EntityField(name = "状态", type = ApiParamType.STRING)
    private String status;
    @EntityField(name = "数据源头", type = ApiParamType.STRING)
    private String source;

    public AttrEntityVo() {

    }

    public AttrEntityVo(AttrEntityTransactionVo attrEntityTransactionVo) {
        this.ciEntityId = attrEntityTransactionVo.getCiEntityId();
        this.attrId = attrEntityTransactionVo.getAttrId();
        this.attrName = attrEntityTransactionVo.getAttrName();
        this.actualValueList = attrEntityTransactionVo.getActualValueList().stream().collect(Collectors.toList());
        this.transactionId = attrEntityTransactionVo.getTransactionId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getInputTypeText() {
        return inputTypeText;
    }

    public void setInputTypeText(String inputTypeText) {
        this.inputTypeText = inputTypeText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    @Override
    public int hashCode() {
        String key = "";
        if (getAttrId() != null) {
            key += getAttrId() + "_";
        }
        if (CollectionUtils.isNotEmpty(getActualValueList())) {
            key += getActualValueList().size() + "_";
            // 根据内容排序生成新数组
            List<String> sortedList = getActualValueList().stream().sorted().collect(Collectors.toList());;
            key += sortedList.stream().collect(Collectors.joining(","));
        }
        return key.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof AttrEntityVo)) {
            return false;
        }
        final AttrEntityVo attr = (AttrEntityVo)other;
        try {
            if (getAttrId().equals(attr.getAttrId())) {
                if (CollectionUtils.isNotEmpty(getActualValueList())
                    && CollectionUtils.isNotEmpty(attr.getActualValueList())) {
                    if (this.getActualValueList().size() == attr.getActualValueList().size()) {
                        for (String v : this.getActualValueList()) {
                            boolean isExists = false;
                            for (String v2 : attr.getActualValueList()) {
                                if (v.equals(v2)) {
                                    isExists = true;
                                    break;
                                } else if (HtmlUtil.encodeHtml(v).equals(v2)) {// 如果xss处理过的，尝试比较转义后的值
                                    isExists = true;
                                    break;
                                }
                            }
                            if (!isExists) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                } else if (CollectionUtils.isEmpty(this.getActualValueList())
                    && CollectionUtils.isEmpty(attr.getActualValueList())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    public List<String> getValueList() {
        if (CollectionUtils.isEmpty(this.valueList) && CollectionUtils.isNotEmpty(this.actualValueList)) {
            valueList = AttrValueUtil.getTransferValueList(this.actualValueList);
        }
        return valueList;
    }

    public void setValueList(List<String> _valueList) {
        if (CollectionUtils.isNotEmpty(_valueList)) {
            this.valueList = _valueList.stream().distinct().collect(Collectors.toList());
        } else {
            this.valueList = _valueList;
        }
    }

    public List<String> getActualValueList() {
        if (CollectionUtils.isEmpty(this.actualValueList) && CollectionUtils.isNotEmpty(this.valueList)) {
            this.actualValueList = AttrValueUtil.getActualValueList(valueList);
        }
        return this.actualValueList;
    }

    public void setActualValueList(List<String> _actualValueList) {
        if (CollectionUtils.isNotEmpty(_actualValueList)) {
            this.actualValueList = _actualValueList.stream().distinct().collect(Collectors.toList());
        } else {
            this.actualValueList = _actualValueList;
        }
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getAttrType() {
        return attrType;
    }

    public void setAttrType(String attrType) {
        this.attrType = attrType;
    }

    public String getPropHandler() {
        return propHandler;
    }

    public void setPropHandler(String propHandler) {
        this.propHandler = propHandler;
    }

    public Long getPropId() {
        return propId;
    }

    public void setPropId(Long propId) {
        this.propId = propId;
    }

    public String getAttrExpression() {
        return attrExpression;
    }

    public void setAttrExpression(String attrExpression) {
        this.attrExpression = attrExpression;
    }

    public String getAttrLabel() {
        return attrLabel;
    }

    public void setAttrLabel(String attrLabel) {
        this.attrLabel = attrLabel;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

}
