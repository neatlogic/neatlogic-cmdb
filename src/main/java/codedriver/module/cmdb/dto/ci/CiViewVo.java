package codedriver.module.cmdb.dto.ci;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;

public class CiViewVo {
    @EntityField(name = "模型Id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "属性或关系id", type = ApiParamType.LONG)
    private Long itemId;
    @EntityField(name = "属性或关系名称", type = ApiParamType.STRING)
    private String itemName;
    @EntityField(name = "类型，attr或rel", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "类型名称", type = ApiParamType.STRING)
    private String typeText;
    @EntityField(name = "排序", type = ApiParamType.INTEGER)
    private Integer sort;
    @EntityField(name = "显示方式，none:不显示，all:全部显示，list:仅列表显示，detail:仅明细显示", type = ApiParamType.STRING)
    private String showType = ShowType.ALL.getValue();
    @EntityField(name = "显示方式名称", type = ApiParamType.STRING)
    private String showTypeText;
    @JSONField(serialize = false)
    private transient List<String> showTypeList;

    public CiViewVo() {

    }

    public CiViewVo(Long ciId, String type) {
        this.ciId = ciId;
        this.type = type;
    }

    public void addShowType(String showType) {
        if (showTypeList == null) {
            showTypeList = new ArrayList<>();
        }
        if (!showTypeList.contains(showType)) {
            showTypeList.add(showType);
        }
    }

    public Long getCiId() {
        return ciId;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getShowType() {
        return showType;
    }

    public void setShowType(String showType) {
        this.showType = showType;
    }

    public String getTypeText() {
        if (StringUtils.isBlank(typeText) && StringUtils.isNotBlank(type)) {
            if (type.equals("attr")) {
                typeText = "属性";
            } else {
                typeText = "关系";
            }
        }
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

    public String getShowTypeText() {
        if (StringUtils.isBlank(showTypeText) && StringUtils.isNotBlank(showType)) {
            showTypeText = ShowType.getText(showType);
        }
        return showTypeText;
    }

    public void setShowTypeText(String showTypeText) {
        this.showTypeText = showTypeText;
    }

    public List<String> getShowTypeList() {
        return showTypeList;
    }

    public void setShowTypeList(List<String> showTypeList) {
        this.showTypeList = showTypeList;
    }

}
