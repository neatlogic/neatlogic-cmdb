package codedriver.module.cmdb.dto.batchimport;

import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.util.SnowflakeUtil;

import java.io.Serializable;
import java.util.Date;

public class ImportAuditVo extends BasePageVo implements Serializable {

	private static final long serialVersionUID = 7432609786418756446L;
	private Long id;
	private Long ciId;
	private Long fileId;
	private String ciName;
	private String importUser;
	private String importUserName;
	private Date importDate;
	private Date finishDate;
	private Integer importCount;
	private Integer status;
	private String action;
	private String actionText;
	private String error;
	private Integer successCount;
	private Integer failedCount;
	private Integer totalCount;
	private Integer serverId;

	public Long getId() {
		if(id == null){
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

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public String getCiName() {
		return ciName;
	}

	public void setCiName(String ciName) {
		this.ciName = ciName;
	}

	public String getImportUser() {
		return importUser;
	}

	public void setImportUser(String importUser) {
		this.importUser = importUser;
	}

	public String getImportUserName() {
		return importUserName;
	}

	public void setImportUserName(String importUserName) {
		this.importUserName = importUserName;
	}

	public Date getImportDate() {
		return importDate;
	}

	public void setImportDate(Date importDate) {
		this.importDate = importDate;
	}

	public Date getFinishDate() {
		return finishDate;
	}

	public void setFinishDate(Date finishDate) {
		this.finishDate = finishDate;
	}

	public Integer getImportCount() {
		return importCount;
	}

	public void setImportCount(Integer importCount) {
		this.importCount = importCount;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setActionText(String actionText) {
		this.actionText = actionText;
	}

	public String getActionText() {
		if (action != null) {
			if (action.equals("append")) {
				return "增量导入";
			} else if (action.equals("update")) {
				return "存量导入";
			} else if (action.equals("all")) {
				return "全量导入";
			}
		}
		return actionText;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Integer getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(Integer successCount) {
		this.successCount = successCount;
	}

	public Integer getFailedCount() {
		return failedCount;
	}

	public void setFailedCount(Integer failedCount) {
		this.failedCount = failedCount;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Integer getServerId() {
		return serverId;
	}

	public void setServerId(Integer serverId) {
		this.serverId = serverId;
	}
}
