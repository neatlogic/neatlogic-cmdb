package neatlogic.module.cmdb.process.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
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
        List<CiEntitySyncFilterVo> newFilterList = ciEntitySyncMappingVo.getFilterList();
        if (CollectionUtils.isNotEmpty(newFilterList)) {
            this.filterList = new ArrayList<>();
            for (CiEntitySyncFilterVo filter : newFilterList) {
                CiEntitySyncFilterVo filterVo = new CiEntitySyncFilterVo();
                filterVo.setColumn(filter.getColumn());
                filterVo.setExpression(filter.getExpression());
                filterVo.setValue(filter.getValue());
                this.filterList.add(filterVo);
            }
        }
        JSONArray newValueList = ciEntitySyncMappingVo.getValueList();
        if (CollectionUtils.isNotEmpty(newValueList)) {
            this.valueList = new JSONArray();
            for (int i = 0; i < newValueList.size(); i++) {
                Object obj = newValueList.get(i);
                if (obj instanceof JSONObject) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.putAll((JSONObject) obj);
                    this.valueList.add(jsonObj);
                } else {
                    this.valueList.add(obj);
                }
            }
        }
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
