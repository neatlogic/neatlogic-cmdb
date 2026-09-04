/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.SaveModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.framework.util.Md5Util;
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.utils.CiEntityUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchSaveCiEntityApi extends PrivateApiComponentBase implements IBatchSaveCiEntityApiCrossoverService {

    @Resource
    private CiEntityService ciEntityService;


    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchsave";
    }

    @Override
    public String getName() {
        return "nmcac.batchsavecientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public JSONObject example() {
        JSONObject defaultJson = new JSONObject(true);
        defaultJson.put("needCommit", true);
        defaultJson.put("isSimple", false);
        defaultJson.put("ciEntityList", new JSONArray() {
            {
                this.add(new JSONObject(true) {{

                    this.put("id", 330340423237635L);
                    this.put("ciId", 323010541453312L);
                    this.put("uuid", "2d327f1213d542bd8a26ace1efb5ab41");
                    this.put("description", "变更说明");
                    //this.put("editMode", "global|partial");
                    this.put("attrEntityData", new JSONObject(true) {{
                        this.put("attr_323010784722944", new JSONObject(true) {{
                            this.put("valueList", new JSONArray() {{
                                this.add($.t("common.testenv"));
                            }});
                            this.put("name", "attrname");
                            this.put("label", "attrlabel");
                            this.put("type", "text");
                        }});
                        this.put("attr_323010784722945", new JSONObject(true) {{
                            this.put("valueList", new JSONArray() {{
                                this.add(new JSONObject(true) {{
                                    this.put("uuid", "12313139343434");
                                }});
                            }});
                            this.put("name", "attrname2");
                            this.put("label", "attrlabel2");
                            this.put("type", "select");
                        }});
                    }});
                    this.put("relEntityData", new JSONObject(true) {{
                        this.put("relfrom_123131313123", new JSONObject(true) {{
                            this.put("valueList", new JSONObject(true) {{
                                this.put("ciEntityUuid", "78a78bc87878abc787d8e7878712");
                                this.put("ciId", "123123123123123");
                            }});
                        }});
                        this.put("relto_1231231313123", new JSONObject(true) {{
                            this.put("valueList", new JSONObject(true) {{
                                this.put("ciEntityUuid", "78a78bc87878abc787d8e7878712");
                                this.put("ciId", "123123123123123");
                            }});
                        }});
                    }});
                    this.put("globalAttrEntityData", new JSONObject(true) {{
                        this.put("global_123131313123", new JSONObject(true) {{
                            this.put("valueList", new JSONObject(true) {{
                                this.put("value", "属性值");
                            }});
                            this.put("name", "globalattrname");
                            this.put("label", "globalattrlabel");
                        }});
                    }});
                }});
            }
        });

        JSONObject simpleJson = new JSONObject(true);
        simpleJson.put("needCommit", true);
        simpleJson.put("isSimple", true);
        simpleJson.put("ciEntityList", new JSONArray() {
            {
                this.add(new JSONObject(true) {{
                    this.put("id", "配置项id，优先级高于uuid");
                    this.put("uuid", "配置项uuid");
                    this.put("ciId", "模型id");
                    this.put("description", "变更说明");
                    //this.put("editMode", "global|partial");
                    this.put("entityData", new JSONObject(true) {{
                        this.put("attrname1（引用型属性更新）", new JSONArray() {{
                            this.add(new JSONObject(true) {{
                                this.put("id", "配置项id，优先级高于uuid");
                                this.put("uuid", "配置项uuid");
                            }});
                        }});
                        this.put("attrname3（引用型属性删除）", new JSONArray());
                        this.put("attrname2（普通属性更新）", new JSONArray() {{
                            this.add(new JSONObject(true) {{
                                this.put("value", "文本、数字、日期等属性值");
                            }});
                        }});
                        this.put("relname2（关系更新）", new JSONArray() {{
                            this.add(new JSONObject(true) {{
                                this.put("id", "目标配置项id");
                                this.put("uuid", "目标配置项的uuid或能作为唯一标识的属性值");
                                this.put("action", "insert（新增关系）|delete（删除关系）|replace（用新关系替换旧关系，如果需要清空关系，无需提供id或uuid属性）。注意：只要任意关系成员的action是replace，关系更新都使用replace模式");
                            }});
                        }});
                        this.put("globalattrname1（全局属性删除）", new JSONArray() {{
                            this.add(new JSONObject(true) {{
                                this.put("value", "");
                            }});
                        }});
                        this.put("globalattrname2（全局属性更新）", new JSONArray() {{
                            this.add(new JSONObject(true) {{
                                this.put("value", "属性值");
                            }});
                        }});
                    }});
                }});
            }
        });

        return new JSONObject(true) {{
            this.put($.t("common.example") + 1, defaultJson);
            this.put($.t("common.example") + 2, simpleJson);
        }};
    }

    private JSONArray convertSimpleData(JSONObject jsonObj) {
        JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        JSONArray returnCiEntityObjList = new JSONArray();
        for (int index = 0; index < ciEntityObjList.size(); index++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(index);
            JSONObject returnCiEntityObj = new JSONObject();
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            Long ciId = ciEntityObj.getLong("ciId");
            String description = ciEntityObj.getString("description");
            returnCiEntityObj.put("editMode", EditModeType.PARTIAL.getValue());
            returnCiEntityObj.put("description", description);
            JSONObject entityData = ciEntityObj.getJSONObject("entityData");
            CiVo ciVo = null;
            if (id != null) {
                ciVo = ciMapper.getCiByCiEntityId(id);
                returnCiEntityObj.put("id", ciEntityObj.getLong("id"));
            } else if (StringUtils.isNotBlank(uuid)) {
                ciVo = ciMapper.getCiByCiEntityUuid(uuid);
                returnCiEntityObj.put("uuid", Md5Util.isMd5(ciEntityObj.getString("uuid")) ? ciEntityObj.getString("uuid") : Md5Util.encryptMD5(ciEntityObj.getString("uuid")));
            } else {
                //如果前端不提供uuid，则生成一个
                returnCiEntityObj.put("uuid", UuidUtil.randomUuid());
            }
            if (ciId != null && ciVo == null) {
                ciVo = ciMapper.getCiById(ciId);
            }
            if (ciVo == null) {
                throw new CiNotFoundException();
            }
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
            List<RelVo> relList = relMapper.getRelByCiId(ciVo.getId());
            List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(new GlobalAttrVo() {{
                this.setIsActive(1);
            }});
            ciVo.setAttrList(attrList);
            ciVo.setRelList(relList);
            ciVo.setGlobalAttrList(globalAttrList);
            returnCiEntityObj.put("ciId", ciVo.getId());
            JSONObject attrEntityData = new JSONObject();
            JSONObject relEntityData = new JSONObject();
            JSONObject globalAttrEntityData = new JSONObject();
            if (MapUtils.isNotEmpty(entityData)) {
                for (String key : entityData.keySet()) {
                    JSONArray valueList = entityData.getJSONArray(key);
                    if (valueList != null) {
                        boolean hasFoundAttr = false;
                        if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                            Optional<AttrVo> attrOp = ciVo.getAttrList().stream().filter(d -> d.getName().equalsIgnoreCase(key)).findFirst();
                            if (attrOp.isPresent()) {
                                AttrVo attrVo = attrOp.get();
                                JSONObject attrObj = new JSONObject();
                                attrObj.put("saveMode", SaveModeType.REPLACE.getValue());
                                attrObj.put("name", attrVo.getName());
                                attrObj.put("label", attrVo.getLabel());
                                attrObj.put("type", attrVo.getType());
                                JSONArray returnValueList = new JSONArray();
                                for (int vindex = 0; vindex < valueList.size(); vindex++) {
                                    JSONObject valueObj = valueList.getJSONObject(vindex);
                                    if (valueObj.getLong("id") != null) {
                                        returnValueList.add(new JSONObject() {{
                                            this.put("id", valueObj.getLong("id"));
                                        }});
                                    } else if (valueObj.containsKey("uuid")) {
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
                                for (int vIndex = 0; vIndex < valueList.size(); vIndex++) {
                                    JSONObject valueObj = valueList.getJSONObject(vIndex);
                                    if (valueObj.getLong("id") != null) {
                                        returnValueList.add(new JSONObject() {{
                                            this.put("ciEntityId", valueObj.getLong("id"));
                                            this.put("ciId", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                            this.put("ciName", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiName() : relVo.getFromCiName());
                                            this.put("action", valueObj.getString("action"));
                                        }});
                                    } else if (StringUtils.isNoneBlank(valueObj.getString("uuid"))) {
                                        returnValueList.add(new JSONObject() {{
                                            this.put("ciEntityUuid", Md5Util.isMd5(valueObj.getString("uuid")) ? valueObj.getString("uuid") : Md5Util.encryptMD5(valueObj.getString("uuid")));
                                            this.put("ciId", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                            this.put("ciName", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiName() : relVo.getFromCiName());
                                            this.put("action", valueObj.getString("action"));
                                        }});
                                    } else if (valueObj.getString("action") != null && valueObj.getString("action").equalsIgnoreCase("replace")) {
                                        /*
                                        一旦有一个replace成员，且不提供具体的cientityId，则意味需要清空关系
                                         */
                                        returnValueList = new JSONArray();
                                        returnValueList.add(new JSONObject() {{
                                            this.put("ciId", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                            this.put("ciName", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiName() : relVo.getFromCiName());
                                            this.put("action", valueList.getJSONObject(0).getString("action"));
                                        }});
                                        break;
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(returnValueList)) {
                                    relObj.put("valueList", returnValueList);
                                    relEntityData.put("rel" + relVo.getDirection() + "_" + relVo.getId(), relObj);
                                    hasFoundAttr = true;
                                }
                            }
                        }

                        if (!hasFoundAttr && CollectionUtils.isNotEmpty(globalAttrList)) {
                            Optional<GlobalAttrVo> attrOp = ciVo.getGlobalAttrList().stream().filter(d -> d.getName().equalsIgnoreCase(key)).findFirst();
                            if (attrOp.isPresent()) {
                                GlobalAttrVo attrVo = attrOp.get();
                                JSONObject attrObj = new JSONObject();
                                attrObj.put("name", attrVo.getName());
                                attrObj.put("label", attrVo.getLabel());
                                attrObj.put("attrId", attrVo.getId());
                                JSONArray returnValueList = new JSONArray();
                                for (int vIndex = 0; vIndex < valueList.size(); vIndex++) {
                                    JSONObject valueObj = valueList.getJSONObject(vIndex);
                                    if (valueObj.containsKey("value")) {
                                        String v = valueObj.getString("value");
                                        GlobalAttrItemVo item = globalAttrMapper.getGlobalAttrItemByAttrIdAndValue(attrVo.getId(), v);
                                        if (item != null) {
                                            valueObj.put("attrId", attrVo.getId());
                                            returnValueList.add(new JSONObject() {{
                                                this.put("attrId", attrVo.getId());
                                                this.put("id", item.getId());
                                                this.put("value", item.getValue());
                                            }});
                                        }

                                    }
                                }
                                attrObj.put("valueList", returnValueList);
                                globalAttrEntityData.put("global_" + attrVo.getId(), attrObj);
                            }
                        }
                    }

                }
            }
            returnCiEntityObj.put("relEntityData", relEntityData);
            returnCiEntityObj.put("attrEntityData", attrEntityData);
            returnCiEntityObj.put("globalAttrEntityData", globalAttrEntityData);
            returnCiEntityObjList.add(returnCiEntityObj);
        }
        return returnCiEntityObjList;
    }

    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "term.cmdb.cientitydata"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "nmcac.batchdeletecientityapi.input.param.desc.needcommit"),
            @Param(name = "isSimple", type = ApiParamType.BOOLEAN, desc = "nmcac.batchsavecientityapi.input.param.desc.issimple", help = "nmcac.batchsavecientityapi.input.param.help.issimple")})
    @Output({@Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "term.cmdb.transactiongroupid"),
            @Param(name = "commited", type = ApiParamType.BOOLEAN, desc = "term.cmdb.iscommit")})
    @ResubmitInterval
    @Description(desc = "nmcac.batchsavecientityapi.getname")
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
        //任意一个模型数据不能提交，则全部不能提交，保证数据一致性。
        boolean allowCommit = true;

        List<CiEntityTransactionVo> ciEntityTransactionList = CiEntityUtils.generateCiEntityTransaction(ciEntityObjList);
        for (CiEntityTransactionVo ciEntityTransactionVo : ciEntityTransactionList) {
            Long id = ciEntityTransactionVo.getCiEntityId();
            Long ciId = ciEntityTransactionVo.getCiId();
            //判断权限
            if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())) {

                if (!CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).check()) {
                    CiVo ciVo = ciMapper.getCiById(ciId);
                    throw new CiEntityAuthException(ciVo.getLabel(), TransactionActionType.INSERT.getText());
                }
                if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).check()) {
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
