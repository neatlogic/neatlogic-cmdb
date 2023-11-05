package neatlogic.module.cmdb.process.dto;

import java.util.List;

public class CiEntitySyncBatchDataSourceVo {
    private String attributeUuid;
    private List<CiEntitySyncFilterVo> filterList;

    public String getAttributeUuid() {
        return attributeUuid;
    }

    public void setAttributeUuid(String attributeUuid) {
        this.attributeUuid = attributeUuid;
    }

    public List<CiEntitySyncFilterVo> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<CiEntitySyncFilterVo> filterList) {
        this.filterList = filterList;
    }
}
