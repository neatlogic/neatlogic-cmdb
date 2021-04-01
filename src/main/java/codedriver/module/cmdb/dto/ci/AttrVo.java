package codedriver.module.cmdb.dto.ci;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.constvalue.InputType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.enums.AttrType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AttrVo implements Serializable {
    private static final long serialVersionUID = 8894392657221743870L;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id，模型id为0代表基础属性", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "值类型", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "值类型名称", type = ApiParamType.STRING)
    private String typeText;
    @EntityField(name = "模型唯一标识", type = ApiParamType.STRING)
    private String ciName;
    @EntityField(name = "模型名称", type = ApiParamType.STRING)
    private String ciLabel;
    @EntityField(name = "目标模型配置项", type = ApiParamType.LONG)
    private Long targetCiId;
    @EntityField(name = "属性配置", type = ApiParamType.JSONOBJECT)
    private JSONObject config;
    @JSONField(serialize = false)
    private transient String configStr;
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
    @EntityField(name = "验证组件类")
    private String validatorHandler;
    @EntityField(name = "验证组件配置", type = ApiParamType.JSONOBJECT)
    private JSONObject validConfig;

    @EntityField(name = "是否必填", type = ApiParamType.INTEGER)
    private Integer isRequired = 0;
    @EntityField(name = "是否唯一", type = ApiParamType.INTEGER)
    private Integer isUnique = 0;
    @EntityField(name = "是否私有属性", type = ApiParamType.INTEGER)
    private Integer isPrivate;
    @EntityField(name = "是否继承属性", type = ApiParamType.INTEGER)
    private Integer isExtended;
    @EntityField(name = "是否需要关联目标模型", type = ApiParamType.BOOLEAN)
    private Boolean needTargetCi;
    @EntityField(name = "是否有额外配置", type = ApiParamType.BOOLEAN)
    private Boolean needConfig;
    @EntityField(name = "是否需要一整行显示编辑组件", type = ApiParamType.BOOLEAN)
    private Boolean needWholeRow;

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
    @JSONField(serialize = false)
    private transient CiVo ciVo;//根据模型生成属性表名
    @JSONField(serialize = false)
    private transient List<Column> columnList;


    public static class Column {
        private String name;
        private String label;
        private String type;
        private String jdbcType;
        private boolean needIndex = false;//是否需要fulltext索引

        public String getName() {
            return name;
        }

        public void setName(String name) {
            if (StringUtils.isNotBlank(name)) {
                this.name = name.trim().toLowerCase();
            }
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            if (StringUtils.isNotBlank(label)) {
                this.label = label.trim().toLowerCase();
            }
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getJdbcType() {
            if (StringUtils.isBlank(jdbcType) && StringUtils.isNotBlank(type)) {
                if (type.equalsIgnoreCase("string")) {
                    jdbcType = "text";
                    needIndex = true;
                } else if (type.equalsIgnoreCase("number")) {
                    //TODO 后期增加精度设置
                    jdbcType = "decimal(10,0)";
                } else {
                    jdbcType = "text";
                    needIndex = true;
                }
            }
            return jdbcType;
        }

        public boolean isNeedIndex() {
            return needIndex;
        }

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
        } catch (Exception ignored) {
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
            AttrType at = AttrType.get(type);
            if (at != null) {
                typeText = at.getText();
            }
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


    public boolean getCanSearch() {
        if (StringUtils.isNotBlank(type)) {
            canSearch = true;
            //FIXME 要处理
            /*if (type.equals(AttrType.CUSTOM.getValue()) || type.equals(AttrType.EXPRESSION.getValue())) {
                canSearch = true;
            } else if (type.equals(AttrType.PROPERTY.getValue())) {
                if (StringUtils.isNotBlank(propHandler)) {
                    IPropertyHandler handler = PropertyHandlerFactory.getHandler(propHandler);
                    if (handler != null) {
                        canSearch = handler.canSearch();
                    }
                }
            }*/
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
        if (CollectionUtils.isEmpty(this.expressionList) && StringUtils.isNotBlank(type)) {
            expressionList = new ArrayList<>();
            //FIXME 需处理
            /*if (type.equals(AttrType.PROPERTY.getValue())) {
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
            }*/
        }
        return expressionList;
    }

    public Integer getIsExtended() {
        return isExtended;
    }

    public void setIsExtended(Integer isExtended) {
        this.isExtended = isExtended;
    }

    public String getValidatorHandler() {
        return validatorHandler;
    }

    public void setValidatorHandler(String validatorHandler) {
        this.validatorHandler = validatorHandler;
    }

    public Long getTargetCiId() {
        return targetCiId;
    }

    public void setTargetCiId(Long targetCiId) {
        this.targetCiId = targetCiId;
    }

    public JSONObject getConfig() {
        if (config == null && StringUtils.isNotBlank(configStr)) {
            try {
                config = JSONObject.parseObject(configStr);
            } catch (Exception ignored) {

            }
        }
        return config;
    }

    public void setConfig(JSONObject config) {
        this.config = config;
    }

    public String getConfigStr() {
        if (StringUtils.isBlank(configStr) && config != null) {
            configStr = config.toJSONString();
        }
        return configStr;
    }

    public void setConfigStr(String configStr) {
        this.configStr = configStr;
    }

    public CiVo getCiVo() {
        return ciVo;
    }

    public void setCiVo(CiVo ciVo) {
        this.ciVo = ciVo;
    }


    /**
     * 获取表名
     *
     * @return 表名
     */
    @JSONField(serialize = false)
    public String getCiTableName() {
        return TenantContext.get().getDataDbName() + ".`cmdb_" + this.getCiId() + "`";
    }


    public List<Column> getColumnList() {
        if (columnList == null && this.getConfig() != null && this.getConfig().containsKey("dataList") && this.getConfig().get("dataList") instanceof JSONArray) {
            JSONArray dataList = this.getConfig().getJSONArray("dataList");
            this.columnList = new ArrayList<>();
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject dataObj = dataList.getJSONObject(i);
                Column column = new Column();
                column.setLabel(dataObj.getString("label"));
                column.setName(dataObj.getString("name"));
                column.setType(dataObj.getString("type"));
                if (StringUtils.isNotBlank(column.getName()) && StringUtils.isNotBlank(column.getLabel()) && StringUtils.isNotBlank(column.getType())) {
                    columnList.add(column);
                }
            }
        }
        return columnList;
    }

    public String getCiName() {
        return ciName;
    }

    public void setCiName(String ciName) {
        this.ciName = ciName;
    }

    public String getCiLabel() {
        return ciLabel;
    }

    public void setCiLabel(String ciLabel) {
        this.ciLabel = ciLabel;
    }

    public Boolean isNeedTargetCi() {
        if (needTargetCi == null && StringUtils.isNotBlank(this.type)) {
            AttrType t = AttrType.get(this.type);
            if (t != null) {
                needTargetCi = t.isNeedTargetCi();
            }
        }
        return needTargetCi;
    }

    public Boolean isNeedConfig() {
        if (needConfig == null && StringUtils.isNotBlank(this.type)) {
            AttrType t = AttrType.get(this.type);
            if (t != null) {
                needConfig = t.isNeedConfig();
            }
        }
        return needConfig;
    }

    public Boolean isNeedWholeRow() {
        if (needWholeRow == null && StringUtils.isNotBlank(this.type)) {
            AttrType t = AttrType.get(this.type);
            if (t != null) {
                needWholeRow = t.isNeedWholeRow();
            }
        }
        return needWholeRow;
    }

}
