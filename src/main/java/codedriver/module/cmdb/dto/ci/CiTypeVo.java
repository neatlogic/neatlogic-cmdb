package codedriver.module.cmdb.dto.ci;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

/**
 * @Author:chenqiwei
 * @Time:Aug 18, 2020
 * @ClassName: CiTypeVo
 * @Description: TODO
 */
public class CiTypeVo {
    @JSONField(serialize = false)
    private transient String keyword;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "排序", type = ApiParamType.INTEGER)
    private Integer sort;
    @EntityField(name = "是否在菜单中显示", type = ApiParamType.INTEGER)
    private Integer isMenu = 0;
    @EntityField(name = "图标", type = ApiParamType.STRING)
    private String icon;
    @EntityField(name = "模型列表", type = ApiParamType.JSONARRAY)
    private List<CiVo> ciList;
    @EntityField(name = "模型数量", type = ApiParamType.INTEGER)
    private int ciCount;

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getIsMenu() {
        return isMenu;
    }

    public void setIsMenu(Integer isMenu) {
        this.isMenu = isMenu;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<CiVo> getCiList() {
        return ciList;
    }

    public void setCiList(List<CiVo> ciList) {
        this.ciList = ciList;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getCiCount() {
        return ciCount;
    }

    public void setCiCount(int ciCount) {
        this.ciCount = ciCount;
    }

}
