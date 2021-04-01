package codedriver.module.cmdb.dto.cientity;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.elasticsearch.annotation.ESKey;
import codedriver.framework.elasticsearch.constvalue.ESKeyType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CiEntityVo extends BasePageVo {
    @JSONField(serialize = false)
    private transient String keyword;
    @JSONField(serialize = false) // 根据空格拆开关键字
    private transient List<String> keywordList;
    @EntityField(name = "id", type = ApiParamType.LONG)
    @ESKey(type = ESKeyType.PKEY, name = "id")
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "模型唯一标识", type = ApiParamType.STRING)
    private String ciName;
    @EntityField(name = "模型名称", type = ApiParamType.STRING)
    private String ciLabel;
    @EntityField(name = "模型图标", type = ApiParamType.STRING)
    private String ciIcon;
    @EntityField(name = "配置项名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "模型类型id", type = ApiParamType.LONG)
    private Long typeId;
    @EntityField(name = "模型类型名称", type = ApiParamType.STRING)
    private String typeName;
    @EntityField(name = "创建人", type = ApiParamType.STRING)
    private String fcu;
    @EntityField(name = "创建时间", type = ApiParamType.LONG)
    private Date fcd;
    @EntityField(name = "修改人", type = ApiParamType.STRING)
    private String lcu;
    @EntityField(name = "修改时间", type = ApiParamType.LONG)
    private Date lcd;
    @EntityField(name = "状态", type = ApiParamType.STRING)
    private String status;
    @EntityField(name = "是否锁定编辑", type = ApiParamType.INTEGER)
    private Integer isLocked = 0;
    // @EntityField(name = "属性列表", type = ApiParamType.JSONARRAY)
    @JSONField(serialize = false)
    private transient List<AttrEntityVo> attrEntityList;
    @EntityField(name = "属性对象，以'attr_'+attrId为key", type = ApiParamType.JSONOBJECT)
    private JSONObject attrEntityData;
    // @EntityField(name = "关系列表", type = ApiParamType.JSONARRAY)
    @JSONField(serialize = false)
    private transient List<RelEntityVo> relEntityList;
    @EntityField(name = "关系对象，以'relfrom_'+relId或'relto_'+relId为key", type = ApiParamType.JSONOBJECT)
    private JSONObject relEntityData;
    // @EntityField(name = "属性过滤器列表", type = ApiParamType.JSONARRAY)
    @JSONField(serialize = false)
    private transient List<AttrFilterVo> attrFilterList;
    @JSONField(serialize = false)
    private transient List<RelFilterVo> relFilterList;
    @JSONField(serialize = false)//当前配置项所涉及的所有模型，包括自己
    private transient List<CiVo> ciList;
    @JSONField(serialize = false)//当前配置项包含的所有属性
    private transient List<AttrVo> attrList;
    @JSONField(serialize = false)//当前配置项包含的所有关系
    private transient List<RelVo> relList;
    @JSONField(serialize = false)
    private transient String inputType;// 更新时设置输入方式
    @JSONField(serialize = false)
    private transient String editMode = EditModeType.GLOBAL.getValue();
    @JSONField(serialize = false)
    private transient Long transactionId;
    @JSONField(serialize = false) // 需要返回的属性列表，为空代表返回所有属性
    private transient List<Long> attrIdList;
    @JSONField(serialize = false) // 需要返回的关系列表，为空代表返回所有关系
    private transient List<Long> relIdList;
    @JSONField(serialize = false)
    private transient List<Long> groupIdList;// 查询时使用的群组id
    @JSONField(serialize = false)
    private transient List<Long> idList;// 需要查询的id列表
    @EntityField(name = "当前用户权限情况", type = ApiParamType.JSONOBJECT)
    private Map<String, Boolean> authData;
    @JSONField(serialize = false)//动态属性
    private transient Map<String, Object> attrEntityMap;

    public CiEntityVo() {

    }

    public CiEntityVo(Long id) {
        this.id = id;
    }

    public CiEntityVo(CiEntityTransactionVo ciEntityTransactionVo) {
        this.id = ciEntityTransactionVo.getCiEntityId();
        this.ciId = ciEntityTransactionVo.getCiId();
        this.name = ciEntityTransactionVo.getName();
    }

    /**
     * 获取表名
     *
     * @return 表名
     */
    @JSONField(serialize = false)
    public String getCiTableName() {
        return TenantContext.get().getDataDbName() + ".`cmdb_" + this.getCiId() + "`";
    }

    @JSONField(serialize = false)
    @Deprecated
    public AttrEntityVo getAttrEntityByAttrId(Long attrId) {
        if (CollectionUtils.isNotEmpty(this.attrEntityList)) {
            for (AttrEntityVo attrEntityVo : this.attrEntityList) {
                if (attrEntityVo.getAttrId().equals(attrId)) {
                    attrEntityVo.setCiEntityId(this.getId());
                    return attrEntityVo;
                }
            }
        }
        return null;
    }

    @JSONField(serialize = false)
    @Deprecated
    public RelEntityVo getRelEntityByRelId(Long relId) {
        if (CollectionUtils.isNotEmpty(this.relEntityList)) {
            for (RelEntityVo relEntityVo : this.relEntityList) {
                if (relEntityVo.getRelId().equals(relId)) {
                    return relEntityVo;
                }
            }
        }
        return null;
    }

    public Long getId() {
        if (id == null) {
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

    public List<CiVo> getCiList() {
        return ciList;
    }

    public void setCiList(List<CiVo> ciList) {
        this.ciList = ciList;
    }

    public String getFcu() {
        if (StringUtils.isBlank(fcu)) {
            return UserContext.get().getUserUuid(true);
        }
        return fcu;
    }

    public void setFcu(String fcu) {
        this.fcu = fcu;
    }

    public Date getFcd() {
        return fcd;
    }

    public void setFcd(Date fcd) {
        this.fcd = fcd;
    }

    public String getLcu() {
        if (StringUtils.isBlank(lcu)) {
            return UserContext.get().getUserUuid(true);
        }
        return lcu;
    }

    public void setLcu(String lcu) {
        this.lcu = lcu;
    }

    public Date getLcd() {
        return lcd;
    }

    public void setLcd(Date lcd) {
        this.lcd = lcd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Integer isLocked) {
        this.isLocked = isLocked;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void addAttrEntity(AttrEntityVo attrEntityVo) {
        if (attrEntityList == null) {
            attrEntityList = new ArrayList<>();
        }
        if (!attrEntityList.contains(attrEntityVo)) {
            attrEntityList.add(attrEntityVo);
        }
    }

    public void addRelEntity(RelEntityVo relEntityVo) {
        if (relEntityList == null) {
            relEntityList = new ArrayList<>();
        }
        if (!relEntityList.contains(relEntityVo)) {
            relEntityList.add(relEntityVo);
        }
    }

    public void setAttrEntityList(List<AttrEntityVo> attrEntityList) {
        this.attrEntityList = attrEntityList;
    }

    public void setRelEntityList(List<RelEntityVo> relEntityList) {
        this.relEntityList = relEntityList;
    }

    public List<AttrFilterVo> getAttrFilterList() {
        return attrFilterList;
    }

    public void setAttrFilterList(List<AttrFilterVo> attrFilterList) {
        this.attrFilterList = attrFilterList;
    }

    public List<RelFilterVo> getRelFilterList() {
        return relFilterList;
    }

    public void setRelFilterList(List<RelFilterVo> relFilterList) {
        this.relFilterList = relFilterList;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getEditMode() {
        return editMode;
    }

    public void setEditMode(String editMode) {
        this.editMode = editMode;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName(String nameExpression) {
        if (CollectionUtils.isNotEmpty(attrEntityList)) {
            if (StringUtils.isBlank(nameExpression)) {
                for (AttrEntityVo attrEntityVo : attrEntityList) {
                    if ("name".equals(attrEntityVo.getAttrName())) {
                        JSONObject attrData = getAttrEntityData();
                        if (attrData != null && attrData.containsKey("attr_" + attrEntityVo.getAttrId())) {
                            return attrData.getJSONObject("attr_" + attrEntityVo.getAttrId()).getJSONArray("valueList")
                                    .stream().map(Object::toString).collect(Collectors.joining("_"));
                        }
                    }
                }
            } else {
                String regex = "\\{([^}]+?)}";
                Matcher matcher = Pattern.compile(regex).matcher(nameExpression);
                Set<String> labelSet = new HashSet<>();
                while (matcher.find()) {
                    labelSet.add(matcher.group(1));
                }
                String ciEntityName = nameExpression;
                if (!labelSet.isEmpty()) {
                    for (AttrEntityVo attrEntityVo : attrEntityList) {
                        if (labelSet.contains(attrEntityVo.getAttrName())) {
                            StringBuilder value = new StringBuilder();
                            for (String v : attrEntityVo.getActualValueList()) {
                                if (!value.toString().equals("")) {
                                    value.append(";");
                                }
                                value.append(v);
                            }
                            ciEntityName = ciEntityName.replace("{" + attrEntityVo.getAttrName() + "}", value.toString());
                            labelSet.remove(attrEntityVo.getAttrName());
                        }
                    }
                }
                return ciEntityName;
            }
        }
        return "";
    }

    public String getName() {
        return name;
    }

    public List<Long> getAttrIdList() {
        return attrIdList;
    }

    public void setAttrIdList(List<Long> attrIdList) {
        this.attrIdList = attrIdList;
    }

    public List<Long> getRelIdList() {
        return relIdList;
    }

    public void setRelIdList(List<Long> relIdList) {
        this.relIdList = relIdList;
    }

    private static final String regex = "\\{([^}]+?)}";

    /**
     * 添加一个属性数据值
     *
     * @param attrId      属性id
     * @param value       属性值
     * @param actualValue 真实属性值（如果是下拉换成真实的应用值）
     */
    public void addAttrEntityDataValue(Long attrId, String value, String actualValue) {
        if (StringUtils.isNotBlank(value) && attrEntityData.containsKey("attr_" + attrId)) {
            JSONObject attrObj = attrEntityData.getJSONObject("attr_" + attrId);
            JSONArray valueList = attrObj.getJSONArray("valueList");
            JSONArray actualValueList = attrObj.getJSONArray("actualValueList");
            if (!valueList.contains(value)) {
                valueList.add(value);
                actualValueList.add(actualValue);
            }
        }
    }


    /**
     * 检查是否有数据项
     *
     * @param attrId 属性id
     * @return true/false
     */
    public boolean hasAttrEntityData(Long attrId) {
        return attrEntityData != null && attrEntityData.containsKey("attr_" + attrId);
    }

    /**
     * 根据属性id获取属性值
     *
     * @param attrId 属性id
     * @return 包含数据的json
     */
    public JSONObject getAttrEntityDataByAttrId(Long attrId) {
        if (attrEntityData != null) {
            return attrEntityData.getJSONObject("attr_" + attrId);
        }
        return null;
    }

    /**
     * 添加一个属性数据项
     *
     * @param attrId  属性id
     * @param attrObj 属性数据项
     */
    public void addAttrEntityData(Long attrId, JSONObject attrObj) {
        if (attrEntityData == null) {
            attrEntityData = new JSONObject();
        }
        attrEntityData.put("attr_" + attrId, attrObj);
    }

    public JSONObject getAttrEntityData() {
        return attrEntityData;
    }

    public List<AttrEntityVo> getAttrEntityList() {
        if (attrEntityList == null) {
            attrEntityList = new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(attrEntityList) && MapUtils.isNotEmpty(attrEntityData)) {
            for (String key : attrEntityData.keySet()) {
                AttrEntityVo attrEntityVo = new AttrEntityVo();
                JSONObject attrEntityObj = attrEntityData.getJSONObject(key);
                attrEntityVo.setAttrId(Long.parseLong(key.replace("attr_", "")));
                attrEntityVo.setAttrType(attrEntityObj.getString("type"));
                attrEntityVo.setAttrName(attrEntityObj.getString("name"));
                attrEntityVo.setAttrLabel(attrEntityObj.getString("label"));
                List<String> valueList = new ArrayList<>();
                for (int i = 0; i < attrEntityObj.getJSONArray("valueList").size(); i++) {
                    valueList.add(attrEntityObj.getJSONArray("valueList").getString(i));
                }
                attrEntityVo.setValueList(valueList);

                List<String> actualValueList = new ArrayList<>();
                for (int i = 0; i < attrEntityObj.getJSONArray("actualValueList").size(); i++) {
                    actualValueList.add(attrEntityObj.getJSONArray("actualValueList").getString(i));
                }
                attrEntityVo.setActualValueList(actualValueList);
                attrEntityList.add(attrEntityVo);
            }
        }
        return attrEntityList;
    }

    @Deprecated
    public JSONObject getAttrEntityData_bak() {
        if (attrEntityData == null) {
            attrEntityData = new JSONObject();
        }
        String keyprefix = "attr_";
        if (MapUtils.isEmpty(attrEntityData) && CollectionUtils.isNotEmpty(this.attrEntityList)) {
            for (AttrEntityVo attrEntityVo : this.attrEntityList) {
                JSONObject attrObj = new JSONObject();
                attrObj.put("type", attrEntityVo.getAttrType());
                attrObj.put("name", attrEntityVo.getAttrName());
                attrObj.put("label", attrEntityVo.getAttrLabel());//需要保存label信息，当属性被删除后可以使用
                if (attrEntityVo.getAttrType().equals(AttrType.EXPRESSION.getValue())) {
                    String v = "";
                    if (StringUtils.isNotBlank(attrEntityVo.getAttrExpression())) {
                        v = attrEntityVo.getAttrExpression();
                        Matcher matcher = Pattern.compile(regex).matcher(v);
                        Set<String> labelSet = new HashSet<>();
                        while (matcher.find()) {
                            labelSet.add(matcher.group(1));
                        }
                        Iterator<String> it = labelSet.iterator();
                        while (it.hasNext()) {
                            String k = it.next();
                            for (AttrEntityVo attrentity : this.attrEntityList) {
                                // 跳过自己或expression类的属性
                                if (attrentity.getAttrId().equals(attrEntityVo.getAttrId())
                                        || attrentity.getAttrType().equals(AttrType.EXPRESSION.getValue())) {
                                    continue;
                                }
                                // 用唯一标识或名称都可以匹配
                                if (k.equalsIgnoreCase(attrentity.getAttrName())
                                        || k.equalsIgnoreCase(attrentity.getAttrLabel())) {
                                    if (CollectionUtils.isNotEmpty(attrentity.getValueList())) {
                                        v = v.replace("{" + k + "}",
                                                String.join("_", attrentity.getValueList()));
                                    } else {
                                        v = v.replace("{" + k + "}", "");
                                    }
                                    it.remove();
                                }
                            }
                        }
                    }
                    JSONArray vl = new JSONArray();
                    vl.add(v);
                    attrObj.put("valueList", vl);
                } else if (attrEntityVo.getAttrType().equals(AttrType.PROPERTY.getValue())) {
                    attrObj.put("valueList", attrEntityVo.getValueList());
                } else if (attrEntityVo.getAttrType().equals(AttrType.CUSTOM.getValue())) {
                    attrObj.put("valueList", attrEntityVo.getValueList());
                }

                attrEntityData.put(keyprefix + attrEntityVo.getAttrId(), attrObj);
            }
        }
        return attrEntityData;
    }

    public List<RelEntityVo> getRelEntityList() {
        if (relEntityList == null) {
            relEntityList = new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(relEntityList) && MapUtils.isNotEmpty(relEntityData)) {
            for (String key : relEntityData.keySet()) {
                JSONObject relEntityObj = relEntityData.getJSONObject(key);
                JSONArray valueList = relEntityObj.getJSONArray("valueList");
                for (int i = 0; i < valueList.size(); i++) {
                    JSONObject valueObj = valueList.getJSONObject(i);
                    RelEntityVo relEntityVo = new RelEntityVo();
                    relEntityVo.setRelId(relEntityObj.getLong("relId"));
                    relEntityVo.setDirection(relEntityObj.getString("direction"));
                    relEntityVo.setRelName(relEntityObj.getString("name"));
                    relEntityVo.setRelLabel(relEntityObj.getString("label"));
                    if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        relEntityVo.setToCiId(valueObj.getLong("ciId"));
                        relEntityVo.setToCiEntityId(valueObj.getLong("ciEntityId"));
                        relEntityVo.setToCiEntityName(valueObj.getString("ciEntityName"));
                    } else {
                        relEntityVo.setFromCiId(valueObj.getLong("ciId"));
                        relEntityVo.setFromCiEntityId(valueObj.getLong("ciEntityId"));
                        relEntityVo.setFromCiEntityName(valueObj.getString("ciEntityName"));
                    }
                    relEntityList.add(relEntityVo);
                }
            }
        }
        return relEntityList;
    }

    /**
     * 添加一个关系数据值
     *
     * @param relId     关系id
     * @param direction 方向
     * @param valueObj  关系值
     */
    public void addRelEntityDataValue(Long relId, String direction, JSONObject valueObj) {
        if (MapUtils.isNotEmpty(valueObj) && relEntityData.containsKey("rel" + direction + "_" + relId)) {
            JSONObject relObj = relEntityData.getJSONObject("rel" + direction + "_" + relId);
            JSONArray valueList = relObj.getJSONArray("valueList");
            if (!valueList.contains(valueObj)) {
                valueList.add(valueObj);
            }
        }
    }

    /**
     * 检查是否有数据项
     *
     * @param relId 关系id
     * @return true/false
     */
    public boolean hasRelEntityData(Long relId, String direction) {
        return relEntityData != null && relEntityData.containsKey("rel" + direction + "_" + relId);
    }

    /**
     * 根据关系id获取属性值
     *
     * @param relId 关系id
     * @return 包含数据的json
     */
    public JSONObject getRelEntityDataByRelId(Long relId, String direction) {
        if (relEntityData != null) {
            return relEntityData.getJSONObject("rel" + direction + "_" + relId);
        }
        return null;
    }

    /**
     * 添加一个属性数据项
     *
     * @param relId  关系id
     * @param relObj 关系数据项
     */
    public void addRelEntityData(Long relId, String direction, JSONObject relObj) {
        if (relEntityData == null) {
            attrEntityData = new JSONObject();
        }
        attrEntityData.put("rel" + direction + "_" + relId, relObj);
    }


    public JSONObject getRelEntityData() {
        return relEntityData;
    }

    @Deprecated
    public JSONObject getRelEntityData_old() {
        if (relEntityData == null) {
            relEntityData = new JSONObject();
        }
        if (MapUtils.isEmpty(relEntityData) && CollectionUtils.isNotEmpty(this.relEntityList)) {
            for (RelEntityVo relEntityVo : this.relEntityList) {
                String keyprefix = "rel" + relEntityVo.getDirection() + "_";
                if (!relEntityData.containsKey(keyprefix + relEntityVo.getRelId())) {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("valueList", new JSONArray());
                    relEntityData.put(keyprefix + relEntityVo.getRelId(), dataObj);
                }
                JSONObject dataObj = relEntityData.getJSONObject(keyprefix + relEntityVo.getRelId());
                dataObj.put("name", relEntityVo.getRelName());
                dataObj.put("label", relEntityVo.getRelLabel());
                JSONObject targetObj = new JSONObject();
                if (relEntityVo.getDirection().equals("from")) {
                    targetObj.put("ciId", relEntityVo.getToCiId());
                    targetObj.put("ciEntityId", relEntityVo.getToCiEntityId());
                    targetObj.put("ciEntityName", relEntityVo.getToCiEntityName());
                } else {
                    targetObj.put("ciId", relEntityVo.getFromCiId());
                    targetObj.put("ciEntityId", relEntityVo.getFromCiEntityId());
                    targetObj.put("ciEntityName", relEntityVo.getFromCiEntityName());
                }
                dataObj.getJSONArray("valueList").add(targetObj);
            }
        }
        return relEntityData;
    }

    public List<Long> getGroupIdList() {
        return groupIdList;
    }

    public void setGroupIdList(List<Long> groupIdList) {
        this.groupIdList = groupIdList;
    }

    public List<Long> getIdList() {
        return idList;
    }

    public void setIdList(List<Long> idList) {
        this.idList = idList;
    }

    public Map<String, Boolean> getAuthData() {
        return authData;
    }

    public void setAuthData(Map<String, Boolean> authData) {
        this.authData = authData;
    }

    public String getCiName() {
        return ciName;
    }

    public void setCiName(String ciName) {
        this.ciName = ciName;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof CiEntityVo)) {
            return false;
        }
        final CiEntityVo ciEntityVo = (CiEntityVo) other;
        return getId().equals(ciEntityVo.getId());
    }

    public String getCiIcon() {
        return ciIcon;
    }

    public void setCiIcon(String ciIcon) {
        this.ciIcon = ciIcon;
    }

    public String getCiLabel() {
        return ciLabel;
    }

    public void setCiLabel(String ciLabel) {
        this.ciLabel = ciLabel;
    }

    public Map<String, Object> getAttrEntityMap() {
        return attrEntityMap;
    }

    public void setAttrEntityMap(Map<String, Object> attrEntityMap) {
        this.attrEntityMap = attrEntityMap;
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
}
