package codedriver.module.cmdb.dto.ci;

import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;

public class CiViewVo {
    @EntityField(name = "模型Id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "属性Id", type = ApiParamType.LONG)
    private Long attrId;
    @EntityField(name = "关系Id", type = ApiParamType.LONG)
    private Long relId;
    @EntityField(name = "排序", type = ApiParamType.INTEGER)
    private Integer sort;
    @EntityField(name = "显示方式，none:不显示，all:全部显示，list:仅列表显示，detail:仅明细显示", type = ApiParamType.INTEGER)
    private String showType = ShowType.ALL.getValue();

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

    public Long getCiId() {
        return ciId;
    }

    public void setCiId(Long ciId) {
        this.ciId = ciId;
    }

    public Long getAttrId() {
        return attrId;
    }

    public void setAttrId(Long attrId) {
        this.attrId = attrId;
    }

    public Long getRelId() {
        return relId;
    }

    public void setRelId(Long relId) {
        this.relId = relId;
    }
}
