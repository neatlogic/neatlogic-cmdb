/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.transaction;

import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
/*
FIXME :将来如果想列出所有属性就继续写这个类
 */

//@Service
//@AuthAction(action = CMDB_BASE.class)
//@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityTransactionApi_new extends PrivateApiComponentBase {

    @Autowired
    private TransactionMapper transactionMapper;

    @Resource
    private AttrMapper attrMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiViewMapper ciViewMapper;


    @Override
    public String getToken() {
        return "/cmdb/cientitytransaction/get";
    }

    @Override
    public String getName() {
        return "获取配置项事务详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", isRequired = true, type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "ciEntityId", isRequired = true, type = ApiParamType.LONG, desc = "配置项id"),
            @Param(name = "transactionId", isRequired = true, type = ApiParamType.LONG, desc = "事务id")})
    @Output({@Param(explode = CiEntityTransactionVo.class)})
    @Description(desc = "获取配置项事务详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long transactionId = jsonObj.getLong("transactionId");
        Long ciId = jsonObj.getLong("ciId");
        CiEntityTransactionVo ciEntityTransactionVo =
                transactionMapper.getCiEntityTransactionByTransactionIdAndCiEntityId(transactionId, ciEntityId);
        JSONObject oldCiEntityObj = null;
        if (StringUtils.isNotBlank(ciEntityTransactionVo.getSnapshot())) {
            oldCiEntityObj = JSONObject.parseObject(ciEntityTransactionVo.getSnapshot());
        }
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        JSONArray dataList = new JSONArray();
        for (CiViewVo ciView : ciViewList) {
            if (ciView.getType().equals("attr") || ciView.getType().startsWith("rel")) {
                JSONObject dataObj = new JSONObject();
                dataObj.put("id", ciView.getItemId());
                dataObj.put("name", ciView.getItemName());
                dataObj.put("label", ciView.getItemLabel());
                if (ciView.getType().equals("attr")) {
                    dataObj.put("type", "attr");
                } else {
                    dataObj.put("type", "rel");
                }
                if (dataObj.getString("type").equals("attr")) {
                    JSONObject attrData = ciEntityTransactionVo.getAttrEntityDataByAttrId(ciView.getItemId());
                    Optional<AttrVo> filterAttr = attrList.stream().filter(attr -> attr.getId().equals(ciView.getItemId())).findFirst();
                    if (filterAttr.isPresent()) {
                        AttrVo attrVo = filterAttr.get();
                        if (MapUtils.isNotEmpty(attrData)) {
                            dataObj.put("newValue", buildAttrObj(attrVo, attrData.getJSONArray("valueList")));
                        }

                        if (MapUtils.isNotEmpty(oldCiEntityObj)) {
                            JSONObject oldAttrEntityData = oldCiEntityObj.getJSONObject("attrEntityData");
                            if (MapUtils.isNotEmpty(oldAttrEntityData) && oldAttrEntityData.containsKey("attr_" + ciView.getItemId())) {
                                dataObj.put("oldValue", buildAttrObj(attrVo, oldAttrEntityData.getJSONObject("attr_" + ciView.getItemId()).getJSONArray("valueList")));
                                if (!dataObj.containsKey("newValue")) {
                                    dataObj.put("newValue", buildAttrObj(attrVo, oldAttrEntityData.getJSONObject("attr_" + ciView.getItemId()).getJSONArray("valueList")));
                                } else {
                                    dataObj.put("action", "update");
                                }
                            }
                        }

                        if (dataObj.containsKey("oldValue")) {
                            dataObj.put("action", "newattr");
                        }

                    }
                }

                dataList.add(dataObj);
            }
        }

/*
        if (MapUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityData())) {
            JSONObject oldAttrEntityData = null;
            if (StringUtils.isNotBlank(ciEntityTransactionVo.getSnapshot())) {
                JSONObject oldCiEntityObj = JSONObject.parseObject(ciEntityTransactionVo.getSnapshot());
                oldAttrEntityData = oldCiEntityObj.getJSONObject("attrEntityData");

            }
            for (String key : ciEntityTransactionVo.getAttrEntityData().keySet()) {
                JSONObject dataObj = new JSONObject();
                JSONObject attrObj = ciEntityTransactionVo.getAttrEntityData().getJSONObject(key);
                Long attrId = Long.parseLong(key.replace("attr_", ""));

                Optional<AttrVo> filterAttr = attrList.stream().filter(attr -> attr.getId().equals(attrId)).findFirst();
                if (filterAttr.isPresent()) {
                    AttrVo attrVo = filterAttr.get();
                    dataObj.put("newValue", buildAttrObj(attrVo, attrObj.getJSONArray("valueList")));
                    if (MapUtils.isNotEmpty(oldAttrEntityData) && oldAttrEntityData.containsKey(key)) {
                        dataObj.put("oldValue", buildAttrObj(attrVo, oldAttrEntityData.getJSONObject(key).getJSONArray("valueList")));
                    }
                    //如果整个newValueList都不存在表示原来使用的属性已经删除，这时候就不需要再显示新旧值了
                } else {
                    //如果属性已删除，尝试使用snapshot数据还原原来的值
                    if (MapUtils.isNotEmpty(oldAttrEntityData) && oldAttrEntityData.containsKey(key)) {
                        AttrVo attrVo = JSONObject.toJavaObject(oldAttrEntityData.getJSONObject(key), AttrVo.class);
                        dataObj.put("oldValue", buildAttrObj(attrVo, oldAttrEntityData.getJSONObject(key).getJSONArray("valueList")));
                    }
                    dataObj.put("action", "delattr");
                }
                dataObj.put("id", attrId);
                dataObj.put("name", attrObj.getString("name"));
                dataObj.put("label", attrObj.getString("label"));
                dataObj.put("type", "attr");
                dataList.add(dataObj);
            }
        }

        if (MapUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityData())) {
            JSONObject oldRelEntityData = null;
            if (StringUtils.isNotBlank(ciEntityTransactionVo.getSnapshot())) {
                JSONObject oldCiEntityObj = JSONObject.parseObject(ciEntityTransactionVo.getSnapshot());
                oldRelEntityData = oldCiEntityObj.getJSONObject("relEntityData");
            }
            for (String key : ciEntityTransactionVo.getRelEntityData().keySet()) {
                JSONObject dataObj = new JSONObject();
                JSONObject relObj = ciEntityTransactionVo.getRelEntityData().getJSONObject(key);
                Long relId = Long.parseLong(key.split("_")[1]);
                dataObj.put("id", relId);
                dataObj.put("name", relObj.getString("name"));
                dataObj.put("label", relObj.getString("label"));
                dataObj.put("direction", relObj.getString("direction"));
                dataObj.put("type", "rel");
                JSONArray newValueList = new JSONArray();
                //因为关系的修改只有insert和delete两种，显示对比时需要去掉删除的目标
                for (int i = 0; i < relObj.getJSONArray("valueList").size(); i++) {
                    JSONObject valueObj = relObj.getJSONArray("valueList").getJSONObject(i);
                    if (!valueObj.containsKey("action") || !valueObj.getString("action").equals("delete")) {
                        //补充ciEntityName
                        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(valueObj.getLong("ciEntityId"));
                        if (ciEntityVo != null) {
                            valueObj.put("ciEntityName", ciEntityVo.getName());
                        }
                        valueObj.put("action", "insert");
                    }
                    newValueList.add(valueObj);
                }

                if (MapUtils.isNotEmpty(oldRelEntityData) && oldRelEntityData.containsKey(key)) {
                    //补充ciEntityName
                    JSONArray oldValueList = oldRelEntityData.getJSONObject(key).getJSONArray("valueList");
                    for (int i = 0; i < oldValueList.size(); i++) {
                        JSONObject valueObj = oldValueList.getJSONObject(i);
                        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(valueObj.getLong("ciEntityId"));
                        if (ciEntityVo != null) {
                            valueObj.put("ciEntityName", ciEntityVo.getName());
                        }
                        //补充原来的值
                        boolean isExists = false;
                        for (int j = 0; j < newValueList.size(); j++) {
                            JSONObject newV = newValueList.getJSONObject(j);
                            if (newV.getLong("ciEntityId").equals(valueObj.getLong("ciEntityId"))) {
                                isExists = true;
                                break;
                            }
                        }
                        if (!isExists) {
                            newValueList.add(valueObj);
                        }
                    }
                    dataObj.put("oldValue", oldValueList);
                }
                //清除删除信息
                for (int j = newValueList.size() - 1; j >= 0; j--) {
                    JSONObject newV = newValueList.getJSONObject(j);
                    if (newV.containsKey("action") && newV.getString("action").equals("delete")) {
                        newValueList.remove(j);
                    }
                }
                dataObj.put("newValue", newValueList);
                dataList.add(dataObj);
            }
        }
*/
        return dataList;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    private JSONObject buildAttrObj(AttrVo attrVo, JSONArray valueList) {
        JSONObject attrObj = new JSONObject();
        attrObj.put("type", attrVo.getType());
        attrObj.put("name", attrVo.getName());
        attrObj.put("label", attrVo.getLabel());
        attrObj.put("config", attrVo.getConfig(true));//克隆一个config对象，避免json序列化出错
        attrObj.put("targetCiId", attrVo.getTargetCiId());
        attrObj.put("valueList", valueList);
        attrObj.put("actualValueList", AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList));
        return attrObj;
    }

    private static String getRelGroupKey(RelEntityTransactionVo relEntityTransactionVo) {
        //如果值为空则使用空格作占位符
        if (relEntityTransactionVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
            return (relEntityTransactionVo.getRelId() != null ? relEntityTransactionVo.getRelId() : " ") + "#" + (relEntityTransactionVo.getDirection() != null ? relEntityTransactionVo.getDirection() : " ") + "#"
                    + (relEntityTransactionVo.getToLabel() != null ? relEntityTransactionVo.getToLabel() : " ") + "#" + (relEntityTransactionVo.getTypeId() != null ? relEntityTransactionVo.getTypeId() : " ");
        } else {
            return (relEntityTransactionVo.getRelId() != null ? relEntityTransactionVo.getRelId() : " ") + "#" + (relEntityTransactionVo.getDirection() != null ? relEntityTransactionVo.getDirection() : " ") + "#"
                    + (relEntityTransactionVo.getFromLabel() != null ? relEntityTransactionVo.getFromLabel() : " ") + "#" + (relEntityTransactionVo.getTypeId() != null ? relEntityTransactionVo.getTypeId() : " ");
        }
    }
}
