package codedriver.module.cmdb.dto.transaction;

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
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "入库值列表", type = ApiParamType.JSONARRAY)
    private List<String> valueList;
    @EntityField(name = "真实值列表", type = ApiParamType.JSONARRAY)
    private List<String> actualValueList;
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
        valueList = attrEntityVo.getValueList();
        actualValueList = attrEntityVo.getActualValueList();
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
        if (!(other instanceof AttrEntityTransactionVo)) {
            return false;
        }
        final AttrEntityTransactionVo attr = (AttrEntityTransactionVo)other;
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

    public void addActualValue(String v) {
        if (CollectionUtils.isEmpty(actualValueList) && CollectionUtils.isNotEmpty(valueList)) {
            actualValueList = AttrValueUtil.getActualValueList(this.valueList);
        }
        if (!actualValueList.contains(v)) {
            actualValueList.add(v);
        }
    }

    public List<String> getActualValueList() {
        if (CollectionUtils.isEmpty(actualValueList) && CollectionUtils.isNotEmpty(valueList)) {
            actualValueList = AttrValueUtil.getActualValueList(this.valueList);
        }
        return actualValueList;
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
        if (CollectionUtils.isEmpty(valueList) && CollectionUtils.isNotEmpty(actualValueList)) {
            valueList = AttrValueUtil.getTransferValueList(actualValueList);
        }
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            this.valueList = valueList.stream().distinct().collect(Collectors.toList());
        } else {
            this.valueList = valueList;
        }
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

    public void setActualValueList(List<String> actualValueList) {
        if (CollectionUtils.isNotEmpty(actualValueList)) {
            this.actualValueList = actualValueList.stream().distinct().collect(Collectors.toList());
        } else {
            this.actualValueList = actualValueList;
        }
    }

}
