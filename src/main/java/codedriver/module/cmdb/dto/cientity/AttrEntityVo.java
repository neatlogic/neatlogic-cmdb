package codedriver.module.cmdb.dto.cientity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.HtmlUtil;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;

public class AttrEntityVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;// 由于需要在SQL批量写入，所以这里使用数据库自增id
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "属性id", type = ApiParamType.LONG)
    private Long attrId;
    @EntityField(name = "属性唯一标识", type = ApiParamType.STRING)
    private String attrName;
    @JSONField(serialize = false) // 原始值，可以是任何类型，后面在拆解到valueList里
    private transient Object value;
    @EntityField(name = "值列表", type = ApiParamType.JSONARRAY)
    private List<String> valueList;
    @EntityField(name = "真实值列表", type = ApiParamType.JSONARRAY)
    private List<String> actualValueList;
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
        // 如果事务的valuelist包含空值，代表需要删除当前属性，则无需写入attrEntity的valueList
        if (CollectionUtils.isNotEmpty(attrEntityTransactionVo.getValueList())) {
            for (String v : attrEntityTransactionVo.getValueList()) {
                if (StringUtils.isNotBlank(v)) {
                    if (this.valueList == null) {
                        this.valueList = new ArrayList<>();
                    }
                    this.valueList.add(v);
                }
            }
        }
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
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            this.valueList = valueList.stream().distinct().collect(Collectors.toList());
        } else {
            this.valueList = valueList;
        }
    }

    public List<String> getActualValueList() {
        if (CollectionUtils.isEmpty(actualValueList)) {
            actualValueList = AttrValueUtil.getActualValueList(this.valueList);
        }
        return actualValueList;
    }

    @JSONField(serialize = false)
    public List<String> getTransferValueList() {
        if (CollectionUtils.isNotEmpty(valueList)) {
            return AttrValueUtil.getTransferValueList(this.valueList);
        }
        return valueList;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
