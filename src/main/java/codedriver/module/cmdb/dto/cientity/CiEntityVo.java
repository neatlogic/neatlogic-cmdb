package codedriver.module.cmdb.dto.cientity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;

public class CiEntityVo extends BasePageVo {
    @JSONField(serialize = false)
    private transient String keyword;
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "配置项名称", type = ApiParamType.STRING)
    private String name;
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
    // @EntityField(name = "关系过滤器列表", type = ApiParamType.JSONARRAY)
    @JSONField(serialize = false)
    private transient List<RelFilterVo> relFilterList;
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

    public CiEntityVo() {

    }

    public CiEntityVo(CiEntityTransactionVo ciEntityTransactionVo) {
        this.id = ciEntityTransactionVo.getCiEntityId();
        this.ciId = ciEntityTransactionVo.getCiId();
        this.name = ciEntityTransactionVo.getName();
    }

    @JSONField(serialize = false)
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

    public String getFcu() {
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

    public List<AttrEntityVo> getAttrEntityList() {
        return attrEntityList;
    }

    public void setAttrEntityList(List<AttrEntityVo> attrEntityList) {
        this.attrEntityList = attrEntityList;
    }

    public List<RelEntityVo> getRelEntityList() {
        return relEntityList;
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

    public String getName() {
        if (StringUtils.isBlank(name)) {
            for (AttrEntityVo attrEntityVo : attrEntityList) {
                if ("name".equals(attrEntityVo.getAttrName())) {
                    JSONObject attrData = getAttrEntityData();
                    if (attrData != null && attrData.containsKey("attr_" + attrEntityVo.getAttrId())) {
                        return attrData.getJSONObject("attr_" + attrEntityVo.getAttrId()).getJSONArray("valueList")
                            .stream().map(v -> v.toString()).collect(Collectors.joining("_"));
                    }
                }
            }
        }
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

    public void setName(String name) {
        this.name = name;
    }

    private static final String regex = "\\{([^\\}]+?)\\}";

    public JSONObject getAttrEntityData() {
        String keyprefix = "attr_";
        if (MapUtils.isEmpty(attrEntityData) && CollectionUtils.isNotEmpty(this.attrEntityList)) {
            if (attrEntityData == null) {
                attrEntityData = new JSONObject();
            }
            for (AttrEntityVo attrEntityVo : this.attrEntityList) {
                JSONObject attrObj = new JSONObject();
                attrObj.put("type", attrEntityVo.getAttrType());
                attrObj.put("name", attrEntityVo.getAttrName());
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
                                    if (CollectionUtils.isNotEmpty(attrentity.getActualValueList())) {
                                        v = v.replace("{" + k + "}",
                                            attrentity.getActualValueList().stream().collect(Collectors.joining("_")));
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
                    attrObj.put("handler", attrEntityVo.getPropHandler());
                    attrObj.put("valueList", attrEntityVo.getActualValueList());
                    attrObj.put("propId", attrEntityVo.getPropId());
                } else if (attrEntityVo.getAttrType().equals(AttrType.CUSTOM.getValue())) {
                    attrObj.put("valueList", attrEntityVo.getActualValueList());
                }

                attrEntityData.put(keyprefix + attrEntityVo.getAttrId(), attrObj);
            }
        }
        return attrEntityData;
    }

    public void setAttrEntityData(JSONObject attrEntityData) {
        this.attrEntityData = attrEntityData;
    }

    public JSONObject getRelEntityData() {
        if (MapUtils.isEmpty(relEntityData) && CollectionUtils.isNotEmpty(this.relEntityList)) {
            if (relEntityData == null) {
                relEntityData = new JSONObject();
            }
            for (RelEntityVo relEntityVo : this.relEntityList) {
                String keyprefix = "rel" + relEntityVo.getDirection() + "_";
                if (!relEntityData.containsKey(keyprefix + relEntityVo.getRelId())) {
                    relEntityData.put(keyprefix + relEntityVo.getRelId(), new JSONArray());
                }
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
                relEntityData.getJSONArray(keyprefix + relEntityVo.getRelId()).add(targetObj);
            }
        }
        return relEntityData;
    }

    public void setRelEntityData(JSONObject relEntityData) {
        this.relEntityData = relEntityData;
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

}
