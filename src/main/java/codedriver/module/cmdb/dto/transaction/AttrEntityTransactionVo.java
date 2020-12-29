package codedriver.module.cmdb.dto.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueUtil;
import codedriver.framework.cmdb.constvalue.SaveModeType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;

public class AttrEntityTransactionVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;// 由于需要在SQL批量写入，所以这里使用数据库自增id
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "属性id", type = ApiParamType.LONG)
    private Long attrId;
    @EntityField(name = "属性名称", type = ApiParamType.STRING)
    private String attrName;
    @EntityField(name = "属性标签", type = ApiParamType.STRING)
    private String attrLabel;
    @EntityField(name = "属性定义id", type = ApiParamType.LONG)
    private Long propId;
    @EntityField(name = "属性定义处理器", type = ApiParamType.STRING)
    private String propHandler;
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "值数据列表", type = ApiParamType.JSONARRAY)
    private List<String> valueList;
    @EntityField(name = "值hash列表", type = ApiParamType.JSONARRAY)
    private List<String> valueHashList;
    @EntityField(name = "保存模式", type = ApiParamType.ENUM, member = SaveModeType.class)
    private String saveMode = SaveModeType.REPLACE.getValue();
    @EntityField(name = "操作", type = ApiParamType.ENUM, member = TransactionActionType.class)
    private String action;

    public AttrEntityTransactionVo() {

    }

    public AttrEntityTransactionVo(AttrEntityVo attrEntityVo) {
        ciEntityId = attrEntityVo.getCiEntityId();
        attrId = attrEntityVo.getAttrId();
        attrName = attrEntityVo.getAttrName();
        valueList = attrEntityVo.getValueList().stream().collect(Collectors.toList());
        transactionId = attrEntityVo.getTransactionId();
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
            List<String> sortedList = getValueList().stream().sorted().collect(Collectors.toList());;
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
        if (!(other instanceof AttrEntityTransactionVo)) {
            return false;
        }
        final AttrEntityTransactionVo attr = (AttrEntityTransactionVo)other;
        try {
            if (getAttrId().equals(attr.getAttrId())) {
                if (CollectionUtils.isNotEmpty(getValueList()) && CollectionUtils.isNotEmpty(attr.getValueList())) {
                    if (this.getValueList().size() == attr.getValueList().size()) {
                        for (String v : this.getValueList()) {
                            boolean isExists = false;
                            for (String v2 : attr.getValueList()) {
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
                } else if (CollectionUtils.isEmpty(this.getValueList())
                    && CollectionUtils.isEmpty(attr.getValueList())) {
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

    public void addValue(String v) {
        if (CollectionUtils.isEmpty(valueList) && CollectionUtils.isNotEmpty(valueHashList)) {
            valueList = AttrValueUtil.getValueList(valueHashList);
        }
        if (valueList == null) {
            valueList = new ArrayList<>();
        }
        if (!valueList.contains(v)) {
            valueList.add(v);
        }
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

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getSaveMode() {
        return saveMode;
    }

    public void setSaveMode(String saveMode) {
        this.saveMode = saveMode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAttrLabel() {
        return attrLabel;
    }

    public void setAttrLabel(String attrLabel) {
        this.attrLabel = attrLabel;
    }

    public Long getPropId() {
        return propId;
    }

    public void setPropId(Long propId) {
        this.propId = propId;
    }

    public String getPropHandler() {
        return propHandler;
    }

    public void setPropHandler(String propHandler) {
        this.propHandler = propHandler;
    }

    public List<String> getValueList() {
        if (CollectionUtils.isEmpty(valueList) && CollectionUtils.isNotEmpty(valueHashList)) {
            valueList = AttrValueUtil.getValueList(valueHashList);
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

    public List<String> getValueHashList() {
        if (CollectionUtils.isEmpty(valueHashList) && CollectionUtils.isNotEmpty(valueList)) {
            valueHashList = AttrValueUtil.getHashList(propHandler, valueList);
        }
        return valueHashList;
    }

    public void setValueHashList(List<String> valueHashList) {
        this.valueHashList = valueHashList;
    }

}
