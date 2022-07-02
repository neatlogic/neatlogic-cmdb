/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchSaveCiEntityApi extends PrivateApiComponentBase {
    //static Logger logger = LoggerFactory.getLogger(BatchSaveCiEntityApi.class);

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchsave";
    }

    @Override
    public String getName() {
        return "保存配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public JSONObject example() {
        String json = "{\n" +
                "  \"ciEntityList\": [\n" +
                "    {\n" +
                "      \"editMode\": \"global|partial\",\n" +
                "      \"attrEntityData\": {\n" +
                "        \"attr_323010784722944\": {\n" +
                "          \"valueList\": [\"测试环境\"],\n" +
                "          \"name\": \"label\",\n" +
                "          \"label\": \"显示名\",\n" +
                "          \"type\": \"text\",\n" +
                "          \"saveMode\": \"merge\"\n" +
                "        },\n" +
                "        \"attr_323010700836864\": {\n" +
                "          \"valueList\": [\"stg33\"],\n" +
                "          \"name\": \"name\",\n" +
                "          \"label\": \"唯一标识\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"relEntityData\": {\n" +
                "        \"relfrom_323010784722234\": {\n" +
                "          \"valueList\": [{\"ciEntityId\":123131231233,\"action\":\"insert|delete|replace，默认是insert，如果有一个成员的action等于replace，代表所有成员都是replace\"}]\n" +
                "        },\n" +
                "        \"relto_3230107847222234\": {\n" +
                "          \"valueList\": [{\"ciEntityUuid\":\"abcdedljklsjdfjlaskdf1233\"}]\n" +
                "        }\n" +
                "      },\n" +
                "      \"ciId\": 323010541453312,\n" +
                "      \"id\": 330340423237635,\n" +
                "      \"uuid\": \"3e3e74b1947b400aa34d7c6964f79168\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        return JSONObject.parseObject(json);
    }

    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项数据"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "是否需要提交")})
    @Description(desc = "保存配置项接口")
    @Example(example = "{\"ciEntityList\":[{\"editMode\":\"global|partial\",\"attrEntityData\":{\"attr_323010784722944\":{\"valueList\":[\"测试环境\"],\"name\":\"label\",\"label\":\"显示名\",\"type\":\"text\",\"saveMode\":\"merge\"},\"attr_323010700836864\":{\"valueList\":[\"stg33\"],\"name\":\"name\",\"label\":\"唯一标识\",\"type\":\"text\"}},\"ciId\":323010541453312,\"id\":330340423237635,\"uuid\":\"3e3e74b1947b400aa34d7c6964f79168\"}]}")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
        //任意一个模型数据不能提交，则全部不能提交，保证数据一致性。
        boolean allowCommit = true;
        // 先给所有没有id的ciEntity分配新的id
        for (int ciindex = 0; ciindex < ciEntityObjList.size(); ciindex++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(ciindex);
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            if (StringUtils.isNotBlank(uuid)) {
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                if (id != null) {
                    ciEntityTransactionVo.setCiEntityId(id);
                }
                ciEntityTransactionMap.put(uuid, ciEntityTransactionVo);
            }
        }

        for (int ciindex = 0; ciindex < ciEntityObjList.size(); ciindex++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(ciindex);
            Long ciId = ciEntityObj.getLong("ciId");
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            String description = ciEntityObj.getString("description");
            CiEntityTransactionVo ciEntityTransactionVo;

            if (id != null) {
                ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(id);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            } else if (StringUtils.isNotBlank(uuid)) {
                ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
                ciEntityTransactionVo.setCiEntityUuid(uuid);
                ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
            } else {
                throw new ApiRuntimeException("数据不合法，缺少id或uuid");
            }

            if (Objects.equals(ciEntityObj.getString("editMode"), EditModeType.GLOBAL.getValue()) || Objects.equals(ciEntityObj.getString("editMode"), EditModeType.PARTIAL.getValue())) {
                ciEntityTransactionVo.setEditMode(ciEntityObj.getString("editMode"));
            }
            ciEntityTransactionVo.setCiId(ciId);
            ciEntityTransactionVo.setDescription(description);

            // 解析属性数据
            JSONObject attrObj = ciEntityObj.getJSONObject("attrEntityData");
            //修正新配置项的uuid为id
            if (MapUtils.isNotEmpty(attrObj)) {
                for (String key : attrObj.keySet()) {
                    JSONObject obj = attrObj.getJSONObject(key);
                    JSONArray valueList = obj.getJSONArray("valueList");
                    //删除没用的属性
                    obj.remove("actualValueList");
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        //因为可能需要删除某些成员，所以需要倒着循环
                        for (int i = valueList.size() - 1; i >= 0; i--) {
                            if (valueList.get(i) instanceof JSONObject) {
                                JSONObject valueObj = valueList.getJSONObject(i);
                                String attrCiEntityUuid = valueObj.getString("uuid");
                                Long attrCiEntityId = valueObj.getLong("id");
                                if (attrCiEntityId == null && StringUtils.isNotBlank(attrCiEntityUuid)) {
                                    CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(attrCiEntityUuid);
                                    if (tmpVo != null) {
                                        //替换掉原来的ciEntityUuid为新的ciEntityId
                                        valueList.set(i, tmpVo.getCiEntityId());
                                    } else {
                                        throw new ApiRuntimeException("找不到" + attrCiEntityUuid + "的新配置项");
                                    }
                                } else if (attrCiEntityId != null) {
                                    valueList.set(i, attrCiEntityId);
                                } else {
                                    valueList.remove(i);
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setAttrEntityData(attrObj);

            // 解析关系数据
            JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
            //修正新配置项的uuid为id
            if (MapUtils.isNotEmpty(relObj)) {
                for (String key : relObj.keySet()) {
                    JSONObject obj = relObj.getJSONObject(key);
                    JSONArray relDataList = obj.getJSONArray("valueList");
                    if (CollectionUtils.isNotEmpty(relDataList)) {
                        for (int i = 0; i < relDataList.size(); i++) {
                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                            if (relEntityObj.getLong("ciEntityId") == null && StringUtils.isNotBlank(relEntityObj.getString("ciEntityUuid"))) {
                                CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(relEntityObj.getString("ciEntityUuid"));
                                if (tmpVo != null) {
                                    relEntityObj.put("ciEntityId", tmpVo.getCiEntityId());
                                } else {
                                    throw new ApiRuntimeException("找不到" + relEntityObj.getString("ciEntityUuid") + "的新配置项");
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setRelEntityData(relObj);

            //判断权限
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())) {
                boolean isInGroup = false;
                CiEntityVo newCiEntityVo = new CiEntityVo(ciEntityTransactionVo);
                if (!CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check()) {
                    CiVo ciVo = ciMapper.getCiById(ciId);
                    throw new CiEntityAuthException(ciVo.getLabel(), TransactionActionType.INSERT.getText());
                }
                if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check()) {
                    allowCommit = false;
                }
            } else if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
                if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).checkCiEntityIsInGroup(id, GroupType.MAINTAIN).check()) {
                    CiVo ciVo = ciMapper.getCiById(ciId);
                    throw new CiEntityAuthException(ciVo.getLabel(), TransactionActionType.UPDATE.getText());
                }
                if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).checkCiEntityIsInGroup(id, GroupType.MAINTAIN).check()) {
                    allowCommit = false;
                }
            }

            ciEntityTransactionList.add(ciEntityTransactionVo);
        }
        if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
            for (CiEntityTransactionVo t : ciEntityTransactionList) {
                if (allowCommit) {
                    t.setAllowCommit(needCommit);
                } else {
                    t.setAllowCommit(false);
                }
            }
            Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
            JSONObject returnObj = new JSONObject();
            returnObj.put("transactionGroupId", transactionGroupId);
            returnObj.put("committed", allowCommit);
            return returnObj;
        }
        return null;
    }

}
