package codedriver.module.cmdb.dto.cientity;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.elasticsearch.annotation.ESKey;
import codedriver.framework.elasticsearch.constvalue.ESKeyType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;

public class RelEntityVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "关系id", type = ApiParamType.LONG)
    private Long relId;
    @EntityField(name = "关系唯一标识", type = ApiParamType.STRING)
    private String relName;
    @EntityField(name = "来源配置项id", type = ApiParamType.LONG)
    @ESKey(type = ESKeyType.PKEY, name = "id")
    private Long fromCiEntityId;
    @EntityField(name = "来源配置项名称", type = ApiParamType.STRING)
    private String fromCiEntityName;
    @EntityField(name = "目标配置项id", type = ApiParamType.LONG)
    @ESKey(type = ESKeyType.PKEY, name = "id")
    private Long toCiEntityId;
    @EntityField(name = "目标配置项名称", type = ApiParamType.STRING)
    private String toCiEntityName;
    @EntityField(name = "生效事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "方向", type = ApiParamType.STRING)
    private String direction;
    @EntityField(name = "目标模型id", type = ApiParamType.LONG)
    private Long toCiId;
    @EntityField(name = "来源模型id", type = ApiParamType.LONG)
    private Long fromCiId;

    public RelEntityVo() {

    }

    public RelEntityVo(RelEntityTransactionVo relEntityTransactionVo) {
        relId = relEntityTransactionVo.getRelId();
        fromCiEntityId = relEntityTransactionVo.getFromCiEntityId();
        toCiEntityId = relEntityTransactionVo.getToCiEntityId();
        direction = relEntityTransactionVo.getDirection();
        transactionId = relEntityTransactionVo.getTransactionId();
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

    public String getRelName() {
        return relName;
    }

    public void setRelName(String relName) {
        this.relName = relName;
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
        if (!(other instanceof RelEntityVo)) {
            return false;
        }
        final RelEntityVo rel = (RelEntityVo)other;
        if (Objects.equal(getRelId(), rel.getRelId()) && Objects.equal(getFromCiEntityId(), rel.getFromCiEntityId())
            && Objects.equal(getToCiEntityId(), rel.getToCiEntityId())
            && Objects.equal(getDirection(), rel.getDirection())) {
            return true;
        } else {
            return false;
        }
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

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
