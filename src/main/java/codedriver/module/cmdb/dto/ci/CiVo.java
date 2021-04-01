package codedriver.module.cmdb.dto.ci;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
 * @Description: 模型实体
 * @Author: chenqiwei
 * @Date: 2021/3/16 5:08 下午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public class CiVo implements Serializable {
    private static final long serialVersionUID = -312040937798083138L;
    @JSONField(serialize = false)
    private transient String keyword;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "父亲模型id", type = ApiParamType.LONG)
    private Long parentCiId;
    @JSONField(serialize = false)
    private transient CiVo parentCi;
    @EntityField(name = "名字表达式", type = ApiParamType.STRING)
    private String nameExpression;
    @EntityField(name = "唯一标识，不能重复", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "名称，不能重复", type = ApiParamType.STRING)
    private String label;
    @EntityField(name = "备注", type = ApiParamType.STRING)
    private String description;
    @EntityField(name = "图标", type = ApiParamType.STRING)
    private String icon;
    @EntityField(name = "类型id", type = ApiParamType.LONG)
    private Long typeId;
    @EntityField(name = "类型名称", type = ApiParamType.STRING)
    private Long typeName;
    @EntityField(name = "是否私有模型，0:不是，1:是", type = ApiParamType.INTEGER)
    private Integer isPrivate = 0;
    @EntityField(name = "是否在菜单中显示，0:不显示，1:显示", type = ApiParamType.INTEGER)
    private Integer isMenu = 0;
    @EntityField(name = "是否抽象模型，0:不是，1:是", type = ApiParamType.INTEGER)
    private Integer isAbstract = 0;
    @EntityField(name = "属性定义列表", type = ApiParamType.JSONARRAY)
    private List<AttrVo> attrList;
    @EntityField(name = "关系定义列表", type = ApiParamType.JSONARRAY)
    private List<RelVo> relList;
    @EntityField(name = "授权列表", type = ApiParamType.JSONARRAY)
    private List<CiAuthVo> authList;
    @EntityField(name = "当前用户权限情况", type = ApiParamType.JSONOBJECT)
    private Map<String, Boolean> authData;
    @EntityField(name = "子模型列表", type = ApiParamType.JSONARRAY)
    private List<CiVo> children;
    @EntityField(name = "左编码", type = ApiParamType.INTEGER)
    private Integer lft;
    @EntityField(name = "右编码", type = ApiParamType.INTEGER)
    private Integer rht;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CiVo ciVo = (CiVo) o;
        return Objects.equals(getId(), ciVo.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public CiVo getParentCi() {
        return parentCi;
    }

    public void setParentCi(CiVo parentCi) {
        this.parentCi = parentCi;
    }

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public String getNameExpression() {
        return nameExpression;
    }

    public void setNameExpression(String nameExpression) {
        this.nameExpression = nameExpression;
    }

    public Integer getIsAbstract() {
        return isAbstract;
    }

    public void setIsAbstract(Integer isAbstract) {
        this.isAbstract = isAbstract;
    }

    public Integer getLft() {
        return lft;
    }

    public void setLft(Integer lft) {
        this.lft = lft;
    }

    public Integer getRht() {
        return rht;
    }

    public void setRht(Integer rht) {
        this.rht = rht;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Integer getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Integer isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Integer getIsMenu() {
        return isMenu;
    }

    public void setIsMenu(Integer isMenu) {
        this.isMenu = isMenu;
    }

    public Long getTypeName() {
        return typeName;
    }

    public void setTypeName(Long typeName) {
        this.typeName = typeName;
    }

    public List<AttrVo> getAttrList() {
        return attrList;
    }

    public void setAttrList(List<AttrVo> attrList) {
        this.attrList = attrList;
    }

    public List<RelVo> getRelList() {
        return relList;
    }

    public void setRelList(List<RelVo> relList) {
        this.relList = relList;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CiAuthVo> getAuthList() {
        return authList;
    }

    public void setAuthList(List<CiAuthVo> authList) {
        this.authList = authList;
    }

    public Map<String, Boolean> getAuthData() {
        return authData;
    }

    public void setAuthData(Map<String, Boolean> authData) {
        this.authData = authData;
    }

    public Long getParentCiId() {
        return parentCiId;
    }

    public void setParentCiId(Long parentCiId) {
        this.parentCiId = parentCiId;
    }

    public List<CiVo> getChildren() {
        return children;
    }

    public void setChildren(List<CiVo> children) {
        this.children = children;
    }

    public void addChild(CiVo ciVo) {
        if (ciVo != null) {
            if (this.children == null) {
                this.children = new ArrayList<>();
            }
            this.children.add(ciVo);
            ciVo.setParentCi(this);
        }
    }

    /**
     * 获取表名
     *
     * @return 表名
     */
    @JSONField(serialize = false)
    public String getCiTableName() {
        return TenantContext.get().getDataDbName() + ".`cmdb_" + this.getId() + "`";
    }


}
