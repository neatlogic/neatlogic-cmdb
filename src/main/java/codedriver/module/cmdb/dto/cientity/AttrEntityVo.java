package codedriver.module.cmdb.dto.cientity;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.stream.Collectors;

public class AttrEntityVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;// 由于需要在SQL批量写入，所以这里使用数据库自增id
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "属性id", type = ApiParamType.LONG)
    private Long attrId;
    @EntityField(name = "属性唯一标识", type = ApiParamType.STRING)
    private String attrName;
    @EntityField(name = "属性名称", type = ApiParamType.STRING)
    private String attrLabel;
    @EntityField(name = "属性类型", type = ApiParamType.STRING)
    private String attrType;
    @EntityField(name = "属性表达式", type = ApiParamType.STRING)
    private String attrExpression;
    @EntityField(name = "属性配置", type = ApiParamType.JSONOBJECT)
    private JSONObject attrConfig;
    @EntityField(name = "值数据列表", type = ApiParamType.JSONARRAY)
    private List<String> valueList;
    @EntityField(name = "显示值列表", type = ApiParamType.JSONARRAY)
    private List<String> actualValueList;
    @JSONField(serialize = false)
    private transient String valueStr;//值字符串类型，如果是多值，则使用,分隔
    @JSONField(serialize = false)
    private transient String valueStrHash;//valueStr的hash值
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
    @EntityField(name = "所属配置型id", type = ApiParamType.LONG)
    private Long fromCiEntityId;
    @EntityField(name = "引用配置型id", type = ApiParamType.LONG)
    private Long toCiEntityId;
    @EntityField(name = "所属配置型模型id", type = ApiParamType.LONG)
    private Long fromCiId;
    @EntityField(name = "引用配置型模型id", type = ApiParamType.LONG)
    private Long toCiId;

    public AttrEntityVo() {

    }

    public AttrEntityVo(AttrEntityTransactionVo attrEntityTransactionVo) {
        this.ciEntityId = attrEntityTransactionVo.getCiEntityId();
        this.attrId = attrEntityTransactionVo.getAttrId();
        this.attrName = attrEntityTransactionVo.getAttrName();
        this.attrLabel = attrEntityTransactionVo.getAttrLabel();
        this.attrType = attrEntityTransactionVo.getAttrType();
        this.valueList = attrEntityTransactionVo.getValueList();
        this.transactionId = attrEntityTransactionVo.getTransactionId();
        AttrVo attr = attrEntityTransactionVo.getAttr();
        this.fromCiId = attr.getCiId();
        this.toCiId = attr.getTargetCiId();
        this.fromCiEntityId = attrEntityTransactionVo.getCiEntityId();
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
        if (CollectionUtils.isNotEmpty(getValueList())) {
            key += getValueList().size() + "_";
            // 根据内容排序生成新数组
            List<String> sortedList = getValueList().stream().sorted().collect(Collectors.toList());
            key += String.join(",", sortedList);
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
        final AttrEntityVo attr = (AttrEntityVo) other;
        try {
            if (getAttrId().equals(attr.getAttrId())) {
                if (CollectionUtils.isNotEmpty(getValueList()) && CollectionUtils.isNotEmpty(attr.getValueList())) {
                    if (this.getValueList().size() == attr.getValueList().size()) {
                        for (String v : this.getValueList()) {
                            boolean isExists = false;
                            for (String v2 : attr.getValueList()) {
                                if (v.equalsIgnoreCase(v2)) {
                                    isExists = true;
                                    break;
                                } else if (HtmlUtil.encodeHtml(v).equalsIgnoreCase(v2)) {// 如果xss处理过的，尝试比较转义后的值
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
                } else {
                    return CollectionUtils.isEmpty(this.getValueList())
                            && CollectionUtils.isEmpty(attr.getValueList());
                }
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    public List<String> getValueList() {
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
        if (CollectionUtils.isEmpty(actualValueList) && CollectionUtils.isNotEmpty(valueList)) {
            actualValueList = valueList.stream().map(v -> AttrValueHandlerFactory.getHandler(this.getAttrType()).getActualValue(attrConfig, v)).collect(Collectors.toList());
        }
        return actualValueList;
    }

    public void setActualValueList(List<String> _actualValueList) {
        if (CollectionUtils.isNotEmpty(_actualValueList)) {
            this.actualValueList = _actualValueList.stream().distinct().collect(Collectors.toList());
        } else {
            this.actualValueList = _actualValueList;
        }
    }


    /**
     * 写入数据库时通过这个属性取值，只对非引用性属性有效
     *
     * @return 值
     */
    public String getValue() {
        if (CollectionUtils.isNotEmpty(valueList)) {
            return valueList.get(0);
        }
        return null;
    }


    public String getAttrType() {
        return attrType;
    }

    public void setAttrType(String attrType) {
        this.attrType = attrType;
    }

    public JSONObject getAttrConfig() {
        return attrConfig;
    }

    public void setAttrConfig(JSONObject attrConfig) {
        this.attrConfig = attrConfig;
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


    public String getValueStr() {
        if (StringUtils.isBlank(valueStr)) {
            List<String> valueList = this.getValueList();
            if (CollectionUtils.isNotEmpty(valueList)) {
                valueStr = String.join(",", valueList);
            }
        }
        return valueStr;
    }

    public String getValueStrHash() {
        if (StringUtils.isNotBlank(this.getValueStr())) {
            valueStrHash = DigestUtils.md5DigestAsHex(this.getValueStr().getBytes());
        }
        return valueStrHash;
    }

    public void setValueStr(String valueStr) {
        this.valueStr = valueStr;
    }

    public void setValueStrHash(String valueStrHash) {
        this.valueStrHash = valueStrHash;
    }

    public Long getFromCiEntityId() {
        return fromCiEntityId;
    }

    public void setFromCiEntityId(Long fromCiEntityId) {
        this.fromCiEntityId = fromCiEntityId;
    }

    public Long getToCiEntityId() {
        return toCiEntityId;
    }

    public void setToCiEntityId(Long toCiEntityId) {
        this.toCiEntityId = toCiEntityId;
    }

    public Long getFromCiId() {
        return fromCiId;
    }

    public void setFromCiId(Long fromCiId) {
        this.fromCiId = fromCiId;
    }

    public Long getToCiId() {
        return toCiId;
    }

    public void setToCiId(Long toCiId) {
        this.toCiId = toCiId;
    }

    /**
     * 获取表名
     *
     * @return 表名
     */
    @JSONField(serialize = false)
    public String getCiTableName() {
        return TenantContext.get().getDataDbName() + ".`cmdb_" + this.getFromCiId() + "`";
    }
}
