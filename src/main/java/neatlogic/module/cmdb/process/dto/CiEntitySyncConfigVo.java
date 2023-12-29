package neatlogic.module.cmdb.process.dto;

import java.util.List;

public class CiEntitySyncConfigVo {
    private Long id;
    private String uuid;
    private Long ciId;
    private String ciName;
    private String ciLabel;
    private String ciIcon;
    /** 是否是起始模型 **/
    private Integer isStart;
    /** 单个和批量 **/
    private String createPolicy;
    /** 追加或替换 **/
    private String action;
    /** 批量数据 **/
    private CiEntitySyncBatchDataSourceVo batchDataSource;
    /** 全局属性、属性和关系映射列表 **/
    private List<CiEntitySyncMappingVo> mappingList;
    /** 同步策略，全局模式或局部模式 **/
    private String editMode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getCiId() {
        return ciId;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
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

    public String getCiIcon() {
        return ciIcon;
    }

    public void setCiIcon(String ciIcon) {
        this.ciIcon = ciIcon;
    }

    public Integer getIsStart() {
        return isStart;
    }

    public void setIsStart(Integer isStart) {
        this.isStart = isStart;
    }

    public String getCreatePolicy() {
        return createPolicy;
    }

    public void setCreatePolicy(String createPolicy) {
        this.createPolicy = createPolicy;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public CiEntitySyncBatchDataSourceVo getBatchDataSource() {
        return batchDataSource;
    }

    public void setBatchDataSource(CiEntitySyncBatchDataSourceVo batchDataSource) {
        this.batchDataSource = batchDataSource;
    }

    public List<CiEntitySyncMappingVo> getMappingList() {
        return mappingList;
    }

    public void setMappingList(List<CiEntitySyncMappingVo> mappingList) {
        this.mappingList = mappingList;
    }

    public String getEditMode() {
        return editMode;
    }

    public void setEditMode(String editMode) {
        this.editMode = editMode;
    }
}
