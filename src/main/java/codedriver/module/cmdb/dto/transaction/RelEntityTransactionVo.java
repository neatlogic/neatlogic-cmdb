package codedriver.module.cmdb.dto.transaction;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;

import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;

public class RelEntityTransactionVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "关系id", type = ApiParamType.LONG)
    private Long relId;
    @EntityField(name = "关系类型", type = ApiParamType.LONG)
    private Long typeId;
    @EntityField(name = "关系类型名称", type = ApiParamType.STRING)
    private String typeText;
    @EntityField(name = "上游端配置项id", type = ApiParamType.LONG)
    private Long fromCiEntityId;
    @EntityField(name = "上游端配置项名称", type = ApiParamType.STRING)
    private String fromCiEntityName;
    @EntityField(name = "下游端配置项id", type = ApiParamType.LONG)
    private Long toCiEntityId;
    @EntityField(name = "下游端配置项名称", type = ApiParamType.STRING)
    private String toCiEntityName;
    @EntityField(name = "下游端模型id", type = ApiParamType.LONG)
    private Long toCiId;
    @EntityField(name = "上游端模型id", type = ApiParamType.LONG)
    private Long fromCiId;
    @EntityField(name = "下游端名称", type = ApiParamType.STRING)
    private String toLabel;
    @EntityField(name = "上游端名称", type = ApiParamType.STRING)
    private String fromLabel;
    @EntityField(name = "方向", type = ApiParamType.STRING)
    private String direction;
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "操作，insert或deletel", type = ApiParamType.ENUM, member = RelActionType.class)
    private String action;
    @EntityField(name = "操作名称，添加或删除", type = ApiParamType.STRING)
    private String actionText;

    public RelEntityTransactionVo() {

    }

    public RelEntityTransactionVo(RelEntityTransactionVo relEntityVo) {
        this.relId = relEntityVo.getRelId();
        this.direction = relEntityVo.getDirection();
        this.toCiEntityId = relEntityVo.getToCiEntityId();
        this.fromCiEntityId = relEntityVo.getFromCiEntityId();
        this.transactionId = relEntityVo.getTransactionId();
        this.fromCiId = relEntityVo.getFromCiId();
        this.toCiId = relEntityVo.getToCiId();
        this.action = relEntityVo.getAction();
        this.fromCiEntityName = relEntityVo.getFromCiEntityName();
        this.toCiEntityName = relEntityVo.getToCiEntityName();
    }

    public RelEntityTransactionVo(RelEntityVo relEntityVo) {
        this.relId = relEntityVo.getRelId();
        this.direction = relEntityVo.getDirection();
        this.toCiEntityId = relEntityVo.getToCiEntityId();
        this.fromCiEntityId = relEntityVo.getFromCiEntityId();
        this.transactionId = relEntityVo.getTransactionId();
        this.fromCiId = relEntityVo.getFromCiId();
        this.toCiId = relEntityVo.getToCiId();
        this.fromCiEntityName = relEntityVo.getFromCiEntityName();
        this.toCiEntityName = relEntityVo.getToCiEntityName();
    }

    public RelEntityTransactionVo(RelEntityVo relEntityVo, RelActionType action) {
        this.relId = relEntityVo.getRelId();
        this.direction = relEntityVo.getDirection();
        this.toCiEntityId = relEntityVo.getToCiEntityId();
        this.fromCiEntityId = relEntityVo.getFromCiEntityId();
        this.transactionId = relEntityVo.getTransactionId();
        this.fromCiId = relEntityVo.getFromCiId();
        this.toCiId = relEntityVo.getToCiId();
        this.fromCiEntityName = relEntityVo.getFromCiEntityName();
        this.toCiEntityName = relEntityVo.getToCiEntityName();
        this.action = action.getValue();
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

    public Long getRelId() {
        return relId;
    }

    public void setRelId(Long relId) {
        this.relId = relId;
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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getFromCiEntityName() {
        return fromCiEntityName;
    }

    public void setFromCiEntityName(String fromCiEntityName) {
        this.fromCiEntityName = fromCiEntityName;
    }

    public String getToCiEntityName() {
        return toCiEntityName;
    }

    public void setToCiEntityName(String toCiEntityName) {
        this.toCiEntityName = toCiEntityName;
    }

    @Override
    public int hashCode() {
        String key = "";
        if (getRelId() != null) {
            key += getRelId() + "_";
        }
        if (getFromCiEntityId() != null) {
            key += getFromCiEntityId() + "_";
        }
        if (getToCiEntityId() != null) {
            key += getToCiEntityId() + "_";
        }
        if (StringUtils.isNotBlank(this.direction)) {
            key += this.direction;
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
        if (!(other instanceof RelEntityTransactionVo)) {
            return false;
        }
        final RelEntityTransactionVo rel = (RelEntityTransactionVo)other;
        if (Objects.equal(getRelId(), rel.getRelId()) && Objects.equal(getFromCiEntityId(), rel.getFromCiEntityId())
            && Objects.equal(getToCiEntityId(), rel.getToCiEntityId())
            && Objects.equal(getDirection(), rel.getDirection())) {
            return true;
        } else {
            return false;
        }
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getToCiId() {
        return toCiId;
    }

    public void setToCiId(Long toCiId) {
        this.toCiId = toCiId;
    }

    public Long getFromCiId() {
        return fromCiId;
    }

    public void setFromCiId(Long fromCiId) {
        this.fromCiId = fromCiId;
    }

    public String getToLabel() {
        return toLabel;
    }

    public void setToLabel(String toLabel) {
        this.toLabel = toLabel;
    }

    public String getFromLabel() {
        return fromLabel;
    }

    public void setFromLabel(String fromLabel) {
        this.fromLabel = fromLabel;
    }

    public String getActionText() {
        if (StringUtils.isNotBlank(action) && StringUtils.isBlank(actionText)) {
            actionText = RelActionType.getText(action);
        }
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public String getTypeText() {
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

}
