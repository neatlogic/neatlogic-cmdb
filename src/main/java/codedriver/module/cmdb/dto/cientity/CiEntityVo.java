package codedriver.module.cmdb.dto.cientity;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;

public class CiEntityVo extends BasePageVo {
    @JSONField(serialize = false)
    private transient String keyword;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "创建人", type = ApiParamType.STRING)
    private String fcu;
    @EntityField(name = "创建时间", type = ApiParamType.LONG)
    private Date fcd;
    @EntityField(name = "修改人", type = ApiParamType.STRING)
    private String lcu;
    @EntityField(name = "修改时间", type = ApiParamType.LONG)
    private Date lcd;
    @EntityField(name = "状态", type = ApiParamType.STRING)
    private String status;
    @EntityField(name = "是否锁定编辑", type = ApiParamType.INTEGER)
    private Integer isLocked = 0;
    @EntityField(name = "属性列表", type = ApiParamType.JSONARRAY)
    private List<AttrEntityVo> attrEntityList;
    @EntityField(name = "关系列表", type = ApiParamType.JSONARRAY)
    private List<RelEntityVo> relEntityList;
    @EntityField(name = "属性过滤器列表", type = ApiParamType.JSONARRAY)
    private List<AttrFilterVo> attrFilterList;
    @EntityField(name = "关系过滤器列表", type = ApiParamType.JSONARRAY)
    private List<RelFilterVo> relFilterList;
    @JSONField(serialize = false)
    private transient String inputType;// 更新时设置输入方式
    @JSONField(serialize = false)
    private transient String editMode = EditModeType.GLOBAL.getValue();
    @JSONField(serialize = false)
    private transient Long transactionId;

    public CiEntityVo() {

    }

    public CiEntityVo(CiEntityTransactionVo ciEntityTransactionVo) {
        this.id = ciEntityTransactionVo.getCiEntityId();
        this.ciId = ciEntityTransactionVo.getCiId();
    }

    @JSONField(serialize = false)
    public AttrEntityVo getAttrEntityByAttrId(Long attrId) {
        if (CollectionUtils.isNotEmpty(this.attrEntityList)) {
            for (AttrEntityVo attrEntityVo : this.attrEntityList) {
                if (attrEntityVo.getAttrId().equals(attrId)) {
                    attrEntityVo.setCiEntityId(this.getId());
                    return attrEntityVo;
                }
            }
        }
        return null;
    }

    @JSONField(serialize = false)
    public RelEntityVo getRelEntityByRelId(Long relId) {
        if (CollectionUtils.isNotEmpty(this.relEntityList)) {
            for (RelEntityVo relEntityVo : this.relEntityList) {
                if (relEntityVo.getRelId().equals(relId)) {
                    return relEntityVo;
                }
            }
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

    public String getFcu() {
        return fcu;
    }

    public void setFcu(String fcu) {
        this.fcu = fcu;
    }

    public Date getFcd() {
        return fcd;
    }

    public void setFcd(Date fcd) {
        this.fcd = fcd;
    }

    public String getLcu() {
        return lcu;
    }

    public void setLcu(String lcu) {
        this.lcu = lcu;
    }

    public Date getLcd() {
        return lcd;
    }

    public void setLcd(Date lcd) {
        this.lcd = lcd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Integer isLocked) {
        this.isLocked = isLocked;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<AttrEntityVo> getAttrEntityList() {
        return attrEntityList;
    }

    public void setAttrEntityList(List<AttrEntityVo> attrEntityList) {
        this.attrEntityList = attrEntityList;
    }

    public List<RelEntityVo> getRelEntityList() {
        return relEntityList;
    }

    public void setRelEntityList(List<RelEntityVo> relEntityList) {
        this.relEntityList = relEntityList;
    }

    public List<AttrFilterVo> getAttrFilterList() {
        return attrFilterList;
    }

    public void setAttrFilterList(List<AttrFilterVo> attrFilterList) {
        this.attrFilterList = attrFilterList;
    }

    public List<RelFilterVo> getRelFilterList() {
        return relFilterList;
    }

    public void setRelFilterList(List<RelFilterVo> relFilterList) {
        this.relFilterList = relFilterList;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getEditMode() {
        return editMode;
    }

    public void setEditMode(String editMode) {
        this.editMode = editMode;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

}
