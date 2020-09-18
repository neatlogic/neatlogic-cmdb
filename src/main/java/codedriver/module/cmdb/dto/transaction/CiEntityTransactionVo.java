package codedriver.module.cmdb.dto.transaction;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;

public class CiEntityTransactionVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @EntityField(name = "编辑模式", type = ApiParamType.ENUM, member = EditModeType.class)
    private String editMode = EditModeType.GLOBAL.getValue();
    @EntityField(name = "操作", type = ApiParamType.ENUM, member = TransactionActionType.class)
    private String action;
    @JSONField(serialize = false)
    private transient List<AttrEntityTransactionVo> attrEntityTransactionList;
    @JSONField(serialize = false)
    private transient List<RelEntityTransactionVo> relEntityTransactionList;

    public CiEntityTransactionVo() {

    }

    public CiEntityTransactionVo(CiEntityVo ciEntityVo) {
        ciId = ciEntityVo.getCiId();
        ciEntityId = ciEntityVo.getId();
    }

    @JSONField(serialize = false)
    public AttrEntityTransactionVo getAttrEntityTransactionByAttrId(Long attrId) {
        if (CollectionUtils.isNotEmpty(this.attrEntityTransactionList)) {
            for (AttrEntityTransactionVo attrEntityVo : this.attrEntityTransactionList) {
                if (attrEntityVo.getAttrId().equals(attrId)) {
                    attrEntityVo.setCiEntityId(this.getCiEntityId());
                    return attrEntityVo;
                }
            }
        }
        return null;
    }

    @JSONField(serialize = false)
    public List<RelEntityTransactionVo> getRelEntityTransactionByRelId(Long relId, String direction) {
        if (CollectionUtils.isNotEmpty(this.relEntityTransactionList)) {
            List<RelEntityTransactionVo> relEntityTransactionList = new ArrayList<>();
            for (RelEntityTransactionVo relEntityVo : this.relEntityTransactionList) {
                if (relEntityVo.getRelId().equals(relId) && relEntityVo.getDirection().equals(direction)) {
                    relEntityTransactionList.add(relEntityVo);
                }
            }
            return relEntityTransactionList;
        }
        return null;
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

    public Long getCiEntityId() {
        // 创建配置项的时候没有配置项id，则生成一个新id
        if (ciEntityId == null) {
            ciEntityId = SnowflakeUtil.uniqueLong();
        }
        return ciEntityId;
    }

    public void setCiEntityId(Long ciEntityId) {
        this.ciEntityId = ciEntityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public List<AttrEntityTransactionVo> getAttrEntityTransactionList() {
        return attrEntityTransactionList;
    }

    public void setAttrEntityTransactionList(List<AttrEntityTransactionVo> attrEntityTransactionList) {
        this.attrEntityTransactionList = attrEntityTransactionList;
    }

    public List<RelEntityTransactionVo> getRelEntityTransactionList() {
        return relEntityTransactionList;
    }

    public void setRelEntityTransactionList(List<RelEntityTransactionVo> relEntityTransactionList) {
        this.relEntityTransactionList = relEntityTransactionList;
    }

    public String getEditMode() {
        return editMode;
    }

    public void setEditMode(String editMode) {
        this.editMode = editMode;
    }

}
