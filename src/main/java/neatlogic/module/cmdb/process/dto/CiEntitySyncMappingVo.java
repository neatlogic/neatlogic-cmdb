package neatlogic.module.cmdb.process.dto;

import com.alibaba.fastjson.JSONArray;

import java.util.List;

public class CiEntitySyncMappingVo {
    private String key;
    private String mappingMode;
    private String column;
    private List<CiEntitySyncFilterVo> filterList;
    private JSONArray valueList;

    public CiEntitySyncMappingVo() {}

    public CiEntitySyncMappingVo(CiEntitySyncMappingVo ciEntitySyncMappingVo) {
        this.key = ciEntitySyncMappingVo.getKey();
        this.mappingMode = ciEntitySyncMappingVo.getMappingMode();
        this.column = ciEntitySyncMappingVo.getColumn();
        this.filterList = ciEntitySyncMappingVo.getFilterList();
        this.valueList = ciEntitySyncMappingVo.getValueList();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMappingMode() {
        return mappingMode;
    }

    public void setMappingMode(String mappingMode) {
        this.mappingMode = mappingMode;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public List<CiEntitySyncFilterVo> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<CiEntitySyncFilterVo> filterList) {
        this.filterList = filterList;
    }

    public JSONArray getValueList() {
        return valueList;
    }

    public void setValueList(JSONArray valueList) {
        this.valueList = valueList;
    }
}
