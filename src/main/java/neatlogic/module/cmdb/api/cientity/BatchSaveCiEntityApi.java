/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.crossover.IBatchSaveCiEntityApiCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.SaveModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchSaveCiEntityApi extends PrivateApiComponentBase implements IBatchSaveCiEntityApiCrossoverService {
    //static Logger logger = LoggerFactory.getLogger(BatchSaveCiEntityApi.class);

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

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
        JSONObject defaultJson = new JSONObject();
        defaultJson.put("needCommit", true);
        defaultJson.put("isSimple", false);
        defaultJson.put("ciEntityList", new JSONArray() {
            {
                this.add(new JSONObject() {{

                    this.put("id", 330340423237635L);
                    this.put("ciId", 323010541453312L);
                    this.put("uuid", "2d327f1213d542bd8a26ace1efb5ab41");
                    this.put("editMode", "global|partial");
                    this.put("attrEntityData", new JSONObject() {{
                        this.put("attr_323010784722944", new JSONObject() {{
                            this.put("valueList", new JSONArray() {{
                                this.add("测试环境");
                            }});
                            this.put("name", "attrname");
                            this.put("label", "attrlabel");
                            this.put("type", "text");
                        }});
                        this.put("attr_323010784722945", new JSONObject() {{
                            this.put("valueList", new JSONArray() {{
                                this.add(new JSONObject() {{
                                    this.put("uuid", "12313139343434");
                                }});
                            }});
                            this.put("name", "attrname2");
                            this.put("label", "attrlabel2");
                            this.put("type", "select");
                        }});
                    }});
                    this.put("relEntityData", new JSONObject() {{
                        this.put("relfrom_123131313123", new JSONObject() {{
                            this.put("valueList", new JSONObject() {{
                                this.put("ciEntityUuid", "78a78bc87878abc787d8e7878712");
                                this.put("ciId", "123123123123123");
                                this.put("action", "replace(editMode=global才生效，replace模式代表要替换整个关系的值，否则只是补充)");
                            }});
                        }});
                        this.put("relto_1231231313123", new JSONObject() {{
                            this.put("valueList", new JSONObject() {{
                                this.put("ciEntityUuid", "78a78bc87878abc787d8e7878712");
                                this.put("ciId", "123123123123123");
                                this.put("action", "replace(editMode=global才生效，replace模式代表要替换整个关系的值，否则只是补充)");
                            }});
                        }});
                    }});

                }});
            }
        });

        JSONObject simpleJson = new JSONObject();
        simpleJson.put("needCommit", true);
        simpleJson.put("isSimple", true);
        simpleJson.put("ciEntityList", new JSONArray() {
            {
                this.add(new JSONObject() {{
                    this.put("ciId", 323010541453312L);
                    this.put("uuid", "2d327f1213d542bd8a26ace1efb5ab41");
                    this.put("editMode", "global|partial");
                    this.put("entityData", new JSONObject() {{
                        this.put("attrname1", new JSONArray() {{
                            this.add(new JSONObject() {{
                                this.put("uuid", "已存在的uuid或能作为唯一标识的属性值");
                            }});
                        }});
                        this.put("attrname2", new JSONArray() {{
                            this.add(new JSONObject() {{
                                this.put("value", "普通文本属性值");
                            }});
                        }});
                        this.put("relname2", new JSONArray() {{
                            this.add(new JSONObject() {{
                                this.put("uuid", "已存在的uuid或能作为唯一标识的属性值");
                                this.put("action", "replace(editMode=global才生效，replace模式代表要替换整个关系的值，否则只是补充)");
                            }});
                        }});
                    }});
                }});
            }
        });
        return new JSONObject() {{
            this.put("范例1", defaultJson);
            this.put("范例2", simpleJson);
        }};
    }

    private JSONArray convertSimpleData(JSONObject jsonObj) {
        JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        Map<String, CiVo> ciMap = new HashMap<>();
        JSONArray returnCiEntityObjList = new JSONArray();
        for (int index = 0; index < ciEntityObjList.size(); index++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(index);
            JSONObject returnCiEntityObj = new JSONObject();
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            String ciName = ciEntityObj.getString("ciName");
            returnCiEntityObj.put("editMode", ciEntityObj.getString("editMode"));
            if (StringUtils.isBlank(ciName)) {
                throw new ParamNotExistsException("ciEntityList.ciName");
            }
            JSONObject entityData = ciEntityObj.getJSONObject("entityData");
            if (id != null) {
                returnCiEntityObj.put("id", ciEntityObj.getLong("id"));
            }
            if (StringUtils.isNotBlank(uuid)) {
                returnCiEntityObj.put("uuid", Md5Util.isMd5(ciEntityObj.getString("uuid")) ? ciEntityObj.getString("uuid") : Md5Util.encryptMD5(ciEntityObj.getString("uuid")));
            }
            ciName = ciName.toLowerCase();
            CiVo ciVo = ciMap.get(ciName);
            if (ciVo == null) {
                ciVo = ciMapper.getCiByName(ciName);
                if (ciVo != null) {
                    List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                    List<RelVo> relList = relMapper.getRelByCiId(ciVo.getId());
                    ciVo.setAttrList(attrList);
                    ciVo.setRelList(relList);
                    ciMap.put(ciName, ciVo);
                } else {
                    throw new CiNotFoundException(ciName);
                }
            }
            returnCiEntityObj.put("ciId", ciVo.getId());
            JSONObject attrEntityData = new JSONObject();
            JSONObject relEntityData = new JSONObject();
            if (MapUtils.isNotEmpty(entityData)) {
                for (String key : entityData.keySet()) {
                    JSONArray valueList = entityData.getJSONArray(key);
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        boolean hasFoundAttr = false;
                        if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                            Optional<AttrVo> attrOp = ciVo.getAttrList().stream().filter(d -> d.getName().equalsIgnoreCase(key)).findFirst();
                            if (attrOp.isPresent()) {
                                AttrVo attrVo = attrOp.get();
                                JSONObject attrObj = new JSONObject();
                                attrObj.put("saveMode", SaveModeType.MERGE.getValue());
                                attrObj.put("name", attrVo.getName());
                                attrObj.put("label", attrVo.getLabel());
                                attrObj.put("type", attrVo.getType());
                                JSONArray returnValueList = new JSONArray();
                                for (int vindex = 0; vindex < valueList.size(); vindex++) {
                                    JSONObject valueObj = valueList.getJSONObject(vindex);
                                    if (valueObj.containsKey("uuid")) {
                                        returnValueList.add(new JSONObject() {{
                                            this.put("uuid", Md5Util.isMd5(valueObj.getString("uuid")) ? valueObj.getString("uuid") : Md5Util.encryptMD5(valueObj.getString("uuid")));
                                        }});
                                    } else if (valueObj.containsKey("value")) {
                                        returnValueList.add(valueObj.getString("value"));
                                    }
                                }
                                attrObj.put("valueList", returnValueList);
                                attrEntityData.put("attr_" + attrVo.getId(), attrObj);
                                hasFoundAttr = true;
                            }
                        }
                        if (!hasFoundAttr && CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                            Optional<RelVo> relOp = ciVo.getRelList().stream().filter(d -> (d.getDirection().equals(RelDirectionType.FROM.getValue()) && d.getToName().equalsIgnoreCase(key)) || (d.getDirection().equals(RelDirectionType.TO.getValue()) && d.getFromName().equalsIgnoreCase(key))).findFirst();
                            if (relOp.isPresent()) {
                                RelVo relVo = relOp.get();
                                JSONObject relObj = new JSONObject();
                                JSONArray returnValueList = new JSONArray();
                                for (int vindex = 0; vindex < valueList.size(); vindex++) {
                                    JSONObject valueObj = valueList.getJSONObject(vindex);
                                    if (valueObj.containsKey("uuid")) {
                                        returnValueList.add(new JSONObject() {{
                                            this.put("ciEntityUuid", Md5Util.isMd5(valueObj.getString("uuid")) ? valueObj.getString("uuid") : Md5Util.encryptMD5(valueObj.getString("uuid")));
                                            this.put("ciId", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                            this.put("ciName", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiName() : relVo.getFromCiName());
                                            this.put("action", valueObj.getString("action"));
                                        }});
                                    }

                                }
                                relObj.put("valueList", returnValueList);
                                relEntityData.put("rel" + relVo.getDirection() + "_" + relVo.getId(), relObj);
                            }
                        }
                    }

                }
            }
            returnCiEntityObj.put("relEntityData", relEntityData);
            returnCiEntityObj.put("attrEntityData", attrEntityData);
            returnCiEntityObjList.add(returnCiEntityObj);
        }
        return returnCiEntityObjList;
    }

    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项数据"), @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "是否需要提交"), @Param(name = "isSimple", type = ApiParamType.BOOLEAN, desc = "数据是否简易模式", help = "简易模式主要给第三方系统使用，true：简易模式，false：正常模式")})
    @Description(desc = "保存配置项接口")
    @Example(example = "{\"ciEntityList\":[{\"editMode\":\"global|partial\",\"attrEntityData\":{\"attr_323010784722944\":{\"valueList\":[\"测试环境\"],\"name\":\"label\",\"label\":\"显示名\",\"type\":\"text\",\"saveMode\":\"merge\"},\"attr_323010700836864\":{\"valueList\":[\"stg33\"],\"name\":\"name\",\"label\":\"唯一标识\",\"type\":\"text\"}},\"ciId\":323010541453312,\"id\":330340423237635,\"uuid\":\"3e3e74b1947b400aa34d7c6964f79168\"}]}")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        boolean isSimple = jsonObj.getBooleanValue("isSimple");
        JSONArray ciEntityObjList;
        if (isSimple) {
            ciEntityObjList = convertSimpleData(jsonObj);
        } else {
            ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        }
        if (CollectionUtils.isEmpty(ciEntityObjList)) {
            throw new ParamNotExistsException("ciEntityList");
        }
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
                } else {
                    CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(uuid);
                    if (uuidCiEntityVo != null) {
                        ciEntityTransactionVo.setCiEntityId(uuidCiEntityVo.getId());
                        ciEntityObj.put("id", uuidCiEntityVo.getId());
                    }
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
                                        //使用uuid寻找配置项
                                        CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(attrCiEntityUuid);
                                        if (uuidCiEntityVo == null) {
                                            throw new ApiRuntimeException("找不到" + attrCiEntityUuid + "的新配置项");
                                        } else {
                                            valueList.set(i, uuidCiEntityVo.getId());
                                        }
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
                            Long ciEntityId = relEntityObj.getLong("ciEntityId");
                            String ciEntityUuid = relEntityObj.getString("ciEntityUuid");
                            if (ciEntityId == null && StringUtils.isNotBlank(ciEntityUuid)) {
                                CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(ciEntityUuid);
                                if (tmpVo != null) {
                                    relEntityObj.put("ciEntityId", tmpVo.getCiEntityId());
                                } else {
                                    CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(ciEntityUuid);
                                    if (uuidCiEntityVo == null) {
                                        throw new ApiRuntimeException("找不到" + relEntityObj.getString("ciEntityUuid") + "的新配置项");
                                    } else {
                                        relEntityObj.put("ciEntityId", uuidCiEntityVo.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setRelEntityData(relObj);

            //判断权限
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())) {
                //boolean isInGroup = false;
                //CiEntityVo newCiEntityVo = new CiEntityVo(ciEntityTransactionVo);
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
            if (!ciEntityTransactionList.contains(ciEntityTransactionVo)) {
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
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
