package codedriver.module.cmdb.dto.transaction;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.constvalue.InputFrom;
import codedriver.framework.cmdb.constvalue.TransactionStatus;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

public class TransactionVo extends BasePageVo {
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "状态", type = ApiParamType.STRING, member = TransactionStatus.class)
    private String status = TransactionStatus.UNCOMMIT.getValue();
    @EntityField(name = "状态文本", type = ApiParamType.STRING)
    private String statusText;
    @EntityField(name = "输入来源", type = ApiParamType.STRING, member = InputFrom.class)
    private String inputFrom = InputFrom.PAGE.getValue();
    @EntityField(name = "输入来源文本", type = ApiParamType.STRING)
    private String inputFromText;
    @EntityField(name = "更新属性数量", type = ApiParamType.INTEGER)
    private int updateAttrCount;
    @EntityField(name = "更新关系数量", type = ApiParamType.INTEGER)
    private int updateRelCount;
    private String source;
    @EntityField(name = "创建用户", type = ApiParamType.STRING)
    private String createUser;
    @EntityField(name = "创建用户名", type = ApiParamType.STRING)
    private String createUserName;
    @EntityField(name = "提交用户", type = ApiParamType.STRING)
    private String commitUser;
    @EntityField(name = "提交用户名", type = ApiParamType.STRING)
    private String commitUserName;
    @JSONField(serialize = false)
    private transient Date expireTime;
    @EntityField(name = "创建时间", type = ApiParamType.LONG)
    private Date createTime;
    @EntityField(name = "提交时间", type = ApiParamType.LONG)
    private Date commitTime;
    @EntityField(name = "异常", type = ApiParamType.STRING)
    private String error;
    @EntityField(name = "配置项事务信息", type = ApiParamType.JSONOBJECT)
    private CiEntityTransactionVo ciEntityTransactionVo;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInputFrom() {
        return inputFrom;
    }

    public void setInputFrom(String inputFrom) {
        this.inputFrom = inputFrom;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCreateUser() {
        if (StringUtils.isBlank(createUser)) {
            createUser = UserContext.get().getUserUuid(true);
        }
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getCommitUser() {
        if (StringUtils.isBlank(commitUser)) {
            commitUser = UserContext.get().getUserUuid(true);
        }
        return commitUser;
    }

    public void setCommitUser(String commitUser) {
        this.commitUser = commitUser;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public CiEntityTransactionVo getCiEntityTransactionVo() {
        return ciEntityTransactionVo;
    }

    public void setCiEntityTransactionVo(CiEntityTransactionVo ciEntityTransactionVo) {
        this.ciEntityTransactionVo = ciEntityTransactionVo;
    }

    public Long getCiEntityId() {
        return ciEntityId;
    }

    public void setCiEntityId(Long ciEntityId) {
        this.ciEntityId = ciEntityId;
    }

    public String getStatusText() {
        if (StringUtils.isNotBlank(status) && StringUtils.isNotBlank(statusText)) {
            statusText = TransactionStatus.getText(status);
        }
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getInputFromText() {
        if (StringUtils.isNotBlank(inputFrom) && StringUtils.isBlank(inputFromText)) {
            inputFromText = InputFrom.getText(inputFrom);
        }
        return inputFromText;
    }

    public void setInputFromText(String inputFromText) {
        this.inputFromText = inputFromText;
    }

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    public String getCommitUserName() {
        return commitUserName;
    }

    public void setCommitUserName(String commitUserName) {
        this.commitUserName = commitUserName;
    }

    public int getUpdateAttrCount() {
        return updateAttrCount;
    }

    public void setUpdateAttrCount(int updateAttrCount) {
        this.updateAttrCount = updateAttrCount;
    }

    public int getUpdateRelCount() {
        return updateRelCount;
    }

    public void setUpdateRelCount(int updateRelCount) {
        this.updateRelCount = updateRelCount;
    }

}
