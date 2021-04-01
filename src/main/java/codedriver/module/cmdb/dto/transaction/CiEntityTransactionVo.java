package codedriver.module.cmdb.dto.transaction;

import codedriver.framework.cmdb.constvalue.EditModeType;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CiEntityTransactionVo {
    static Logger logger = LoggerFactory.getLogger(CiEntityTransactionVo.class);
    @JSONField(serialize = false)
    private transient String ciEntityUuid;// 批量添加时的临时ID，由前端生成
    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "模型id", type = ApiParamType.LONG)
    private Long ciId;
    @EntityField(name = "配置项id", type = ApiParamType.LONG)
    private Long ciEntityId;
    @EntityField(name = "配置项名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "事务id", type = ApiParamType.LONG)
    private Long transactionId;
    @JSONField(serialize = false)
    private transient TransactionActionType transactionMode;// 事务动作（删除、添加或修改）
    @EntityField(name = "编辑模式", type = ApiParamType.ENUM, member = EditModeType.class)
    private String editMode = EditModeType.GLOBAL.getValue();
    @EntityField(name = "操作", type = ApiParamType.ENUM, member = TransactionActionType.class)
    private String action;
    @EntityField(name = "操作文本", type = ApiParamType.STRING)
    private String actionText;
    @EntityField(name = "属性对象，以'attr_'+attrId为key", type = ApiParamType.JSONOBJECT)
    private JSONObject attrEntityData;
    @EntityField(name = "关系对象，以'relfrom_'+relId或'relto_'+relId为key", type = ApiParamType.JSONOBJECT)
    private JSONObject relEntityData;
    @JSONField(serialize = false)
    private transient List<AttrEntityTransactionVo> attrEntityTransactionList;
    @JSONField(serialize = false)
    private transient List<RelEntityTransactionVo> relEntityTransactionList;
    @JSONField(serialize = false)
    private transient String snapshot;// 修改前的快照
    @JSONField(serialize = false)
    private transient String content;//修改内容
    @JSONField(serialize = false)
    private transient CiEntityVo oldCiEntityVo;//就配置项信息

    public CiEntityTransactionVo() {

    }

    public CiEntityTransactionVo(CiEntityVo ciEntityVo) {
        ciId = ciEntityVo.getCiId();
        ciEntityId = ciEntityVo.getId();
    }

    @JSONField(serialize = false)
    public AttrEntityTransactionVo getAttrEntityTransactionByAttrId(Long attrId) {
        if (CollectionUtils.isNotEmpty(this.attrEntityTransactionList)) {
            for (AttrEntityTransactionVo attrEntityVo : this.attrEntityTransactionList) {
                if (attrEntityVo.getAttrId().equals(attrId)) {
                    attrEntityVo.setCiEntityId(this.getCiEntityId());
                    return attrEntityVo;
                }
            }
        }
        return null;
    }

    public CiEntityVo getOldCiEntityVo() {
        return oldCiEntityVo;
    }

    public void setOldCiEntityVo(CiEntityVo oldCiEntityVo) {
        this.oldCiEntityVo = oldCiEntityVo;
    }

    @JSONField(serialize = false)
    public List<RelEntityTransactionVo> getRelEntityTransactionByRelId(Long relId, String direction) {
        if (CollectionUtils.isNotEmpty(this.relEntityTransactionList)) {
            List<RelEntityTransactionVo> relEntityTransactionList = new ArrayList<>();
            for (RelEntityTransactionVo relEntityVo : this.relEntityTransactionList) {
                if (relEntityVo.getRelId().equals(relId) && relEntityVo.getDirection().equals(direction)) {
                    relEntityTransactionList.add(relEntityVo);
                }
            }
            return relEntityTransactionList;
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

    public Long getCiEntityId() {
        // 创建配置项的时候没有配置项id，则生成一个新id
        if (ciEntityId == null) {
            ciEntityId = SnowflakeUtil.uniqueLong();
        }
        return ciEntityId;
    }

    public void setCiEntityId(Long ciEntityId) {
        this.ciEntityId = ciEntityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public List<AttrEntityTransactionVo> getAttrEntityTransactionList() {
        if (CollectionUtils.isEmpty(attrEntityTransactionList) && MapUtils.isNotEmpty(attrEntityData)) {
            attrEntityTransactionList = new ArrayList<>();
            for (String key : attrEntityData.keySet()) {
                Long attrId = null;
                try {
                    attrId = Long.parseLong(key.replace("attr_", ""));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                if (attrId != null) {
                    AttrEntityTransactionVo attrEntityVo = new AttrEntityTransactionVo();
                    attrEntityVo.setAttrId(attrId);
                    JSONObject attrDataObj = attrEntityData.getJSONObject(key);
                    JSONArray valueObjList = attrDataObj.getJSONArray("valueList");
                    attrEntityVo.setValueList(valueObjList.stream().map(Object::toString).collect(Collectors.toList()));
                    attrEntityTransactionList.add(attrEntityVo);
                }
            }
        }
        return attrEntityTransactionList;
    }


    public List<RelEntityTransactionVo> getRelEntityTransactionList() {
        if (relEntityTransactionList == null) {
            relEntityTransactionList = new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(relEntityTransactionList) && MapUtils.isNotEmpty(relEntityData)) {
            for (String key : relEntityData.keySet()) {
                JSONArray relDataList = relEntityData.getJSONArray(key);
                if (key.startsWith("relfrom_")) {// 当前配置项处于from位置
                    if (CollectionUtils.isNotEmpty(relDataList)) {
                        for (int i = 0; i < relDataList.size(); i++) {
                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                            RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo();
                            relEntityVo.setRelId(Long.parseLong(key.replace("relfrom_", "")));
                            relEntityVo.setToCiEntityId(relEntityObj.getLong("ciEntityId"));
                            relEntityVo.setDirection(RelDirectionType.FROM.getValue());
                            relEntityVo.setFromCiEntityId(this.getCiEntityId());
                            relEntityVo.setAction(RelActionType.INSERT.getValue());// 默认是添加关系
                            relEntityTransactionList.add(relEntityVo);
                        }
                    }
                } else if (key.startsWith("relto_")) {// 当前配置项处于to位置
                    if (CollectionUtils.isNotEmpty(relDataList)) {
                        for (int i = 0; i < relDataList.size(); i++) {
                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                            RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo();
                            relEntityVo.setRelId(Long.parseLong(key.replace("relto_", "")));
                            relEntityVo.setFromCiEntityId(relEntityObj.getLong("ciEntityId"));
                            relEntityVo.setDirection(RelDirectionType.TO.getValue());
                            relEntityVo.setToCiEntityId(this.getCiEntityId());
                            relEntityVo.setAction(RelActionType.INSERT.getValue());// 默认是添加关系
                            relEntityTransactionList.add(relEntityVo);
                        }
                    }
                }
            }
        }
        return relEntityTransactionList;
    }

    /**
     * 数据库写入时通过此方法取得json值
     *
     * @return json
     */
    @JSONField(serialize = false)
    public String getContent() {
        if (StringUtils.isBlank(content)) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("attrEntityData", this.getAttrEntityData());
            jsonObj.put("relEntityData", this.getRelEntityData());
            content = jsonObj.toJSONString();
        }
        return content;
    }

    public void setContent(String content) {
        if (StringUtils.isNotBlank(content)) {
            try {
                JSONObject jsonObj = JSONObject.parseObject(content);
                this.attrEntityData = jsonObj.getJSONObject("attrEntityData");
                this.relEntityData = jsonObj.getJSONObject("relEntityData");
            } catch (Exception ignored) {

            }
        }
    }

    public String getEditMode() {
        return editMode;
    }

    public void setEditMode(String editMode) {
        this.editMode = editMode;
    }

    public String getActionText() {
        if (StringUtils.isNotBlank(action) && StringUtils.isBlank(actionText)) {
            actionText = TransactionActionType.getText(action);
        }
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }


    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCiEntityUuid() {
        return ciEntityUuid;
    }

    public void setCiEntityUuid(String ciEntityUuid) {
        this.ciEntityUuid = ciEntityUuid;
    }

    public TransactionActionType getTransactionMode() {
        return transactionMode;
    }

    public void setTransactionMode(TransactionActionType transactionMode) {
        this.transactionMode = transactionMode;
    }

    public JSONObject getAttrEntityData() {
        return attrEntityData;
    }

    public void setAttrEntityData(JSONObject attrEntityData) {
        this.attrEntityData = attrEntityData;
    }

    public JSONObject getRelEntityData() {
        return relEntityData;
    }

    public void setRelEntityData(JSONObject relEntityData) {
        this.relEntityData = relEntityData;
    }
}
