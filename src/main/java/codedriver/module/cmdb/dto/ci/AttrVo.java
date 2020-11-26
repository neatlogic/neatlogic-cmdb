package codedriver.module.cmdb.dto.ci;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;

import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.cmdb.constvalue.InputType;
import codedriver.framework.cmdb.constvalue.SearchExpression;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.cmdb.prop.core.PropertyHandlerFactory;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class AttrVo implements Serializable {
    private static final long serialVersionUID = 8894392657221743870L;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "值类型，custom|property|expression", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "值类型名称", type = ApiParamType.STRING)
    private String typeText;
    @EntityField(name = "属性定义id", type = ApiParamType.LONG)
    private Long propId;
    @EntityField(name = "属性定义类型", type = ApiParamType.STRING)
    private String propHandler;
    @EntityField(name = "属性定义配置", type = ApiParamType.JSONOBJECT)
    private JSONObject propConfig;
    @EntityField(name = "值表达式", type = ApiParamType.STRING)
    private String expression;
    @EntityField(name = "英文名称，模型内唯一", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "中文名称，模型内唯一", type = ApiParamType.STRING)
    private String label;
    @EntityField(name = "描述", type = ApiParamType.STRING)
    private String description;
    @EntityField(name = "验证组件id", type = ApiParamType.LONG)
    private Long validatorId;
    @EntityField(name = "验证组件名称", type = ApiParamType.STRING)
    private String validatorName;
    @EntityField(name = "验证组件配置", type = ApiParamType.JSONOBJECT)
    private JSONObject validConfig;

    @EntityField(name = "是否必填", type = ApiParamType.INTEGER)
    private Integer isRequired = 0;
    @EntityField(name = "是否唯一", type = ApiParamType.INTEGER)
    private Integer isUnique = 0;
    @EntityField(name = "是否私有属性", type = ApiParamType.INTEGER)
    private Integer isPrivate;

    @EntityField(name = "录入方式，at:自动发现，mt:手动输入", type = ApiParamType.STRING)
    private String inputType = InputType.MT.getValue();
    @EntityField(name = "录入方式", type = ApiParamType.STRING)
    private String inputTypeText;
    @EntityField(name = "分组名称", type = ApiParamType.STRING)
    private String groupName;
    @EntityField(name = "是否支持搜索", type = ApiParamType.BOOLEAN)
    private boolean canSearch = false;
    @JSONField(serialize = false)
    private transient int sort;// 排序，数据来自ciViewVo
    @EntityField(name = "支持的搜索表达式列表")
    private List<ValueTextVo> expressionList;

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

    public Long getPropId() {
        return propId;
    }

    public void setPropId(Long propId) {
        this.propId = propId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(Long validatorId) {
        this.validatorId = validatorId;
    }

    public Integer getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Integer isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Integer isUnique) {
        this.isUnique = isUnique;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public JSONObject getValidConfig() {
        return validConfig;
    }

    public void setValidConfig(String validConfigStr) {
        try {
            this.validConfig = JSONObject.parseObject(validConfigStr);
        } catch (Exception ex) {
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeText() {
        if (StringUtils.isBlank(typeText) && StringUtils.isNotBlank(type)) {
            typeText = AttrType.getText(type);
        }
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Integer getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Integer isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getInputTypeText() {
        if (StringUtils.isBlank(inputTypeText) && StringUtils.isNotBlank(inputType)) {
            inputTypeText = InputType.getText(inputType);
        }
        return inputTypeText;
    }

    public String getValidatorName() {
        return validatorName;
    }

    public void setValidatorName(String validatorName) {
        this.validatorName = validatorName;
    }

    public String getPropHandler() {
        return propHandler;
    }

    public void setPropHandler(String propHandler) {
        this.propHandler = propHandler;
    }

    public JSONObject getPropConfig() {
        return propConfig;
    }

    public void setPropConfig(String propConfigStr) {
        try {
            this.propConfig = JSONObject.parseObject(propConfigStr);
        } catch (Exception ex) {

        }
    }

    public boolean getCanSearch() {
        if (StringUtils.isNotBlank(type)) {
            if (type.equals(AttrType.CUSTOM.getValue()) || type.equals(AttrType.EXPRESSION.getValue())) {
                canSearch = true;
            } else if (type.equals(AttrType.PROPERTY.getValue())) {
                if (StringUtils.isNotBlank(propHandler)) {
                    IPropertyHandler handler = PropertyHandlerFactory.getHandler(propHandler);
                    if (handler != null) {
                        canSearch = handler.canSearch();
                    }
                }
            }
        }
        return canSearch;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public List<ValueTextVo> getExpressionList() {
        if (CollectionUtils.isEmpty(this.expressionList)) {
            expressionList = new ArrayList<>();
            if (type.equals(AttrType.PROPERTY.getValue())) {
                if (StringUtils.isNotBlank(propHandler)) {
                    IPropertyHandler handler = PropertyHandlerFactory.getHandler(propHandler);
                    for (SearchExpression expression : handler.getSupportExpression()) {
                        expressionList.add(new ValueTextVo(expression.getExpression(), expression.getText()));
                    }
                }
            } else if (type.equals(AttrType.CUSTOM.getValue())) {
                for (SearchExpression expression : SearchExpression.values()) {
                    expressionList.add(new ValueTextVo(expression.getExpression(), expression.getText()));
                }
            }
        }
        return expressionList;
    }

}
