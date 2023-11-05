package neatlogic.module.cmdb.process.dto;

import java.util.List;

public class CiEntitySyncVo {
    private String failPolicy;
    private Integer rerunStepToSync;
    private List<CiEntitySyncConfigVo> configList;

    public String getFailPolicy() {
        return failPolicy;
    }

    public void setFailPolicy(String failPolicy) {
        this.failPolicy = failPolicy;
    }

    public Integer getRerunStepToSync() {
        return rerunStepToSync;
    }

    public void setRerunStepToSync(Integer rerunStepToSync) {
        this.rerunStepToSync = rerunStepToSync;
    }

    public List<CiEntitySyncConfigVo> getConfigList() {
        return configList;
    }

    public void setConfigList(List<CiEntitySyncConfigVo> configList) {
        this.configList = configList;
    }
}
