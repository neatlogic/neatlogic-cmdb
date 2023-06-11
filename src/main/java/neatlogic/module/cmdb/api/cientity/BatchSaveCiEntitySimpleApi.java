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
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.RelActionType;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.SaveModeType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchSaveCiEntitySimpleApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "nmcac.batchsavecientitysimpleapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchsave/simple";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        Map<String, CiVo> ciMap = new HashMap<>();
        JSONArray returnCiEntityObjList = new JSONArray();
        for (int index = 0; index < ciEntityObjList.size(); index++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(index);
            JSONObject returnCiEntityObj = new JSONObject();
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            String ciName = ciEntityObj.getString("ciName");
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
                                        }});
                                    }

                                }
                                relObj.put("valueList", returnValueList);
                                relEntityData.put("rel" + relVo.getDirection() + "_" + relVo.getId() + relVo.getId(), relObj);
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


    /**
     * @param ciVo            ci对象
     * @param entityDataParam entity属性或关系入参
     * @param attrMap         ci属性map
     * @param relMap          ci关系map
     * @param ciEntityId      entity Id
     * @param ciMap           ci 缓存
     * @param editMode        global：全局，partial：局部
     * @return 转换后的配置项入参
     */
    private JSONObject getCiEntityResultDate(CiVo ciVo, JSONObject entityDataParam, Map<String, AttrVo> attrMap, Map<String, RelVo> relMap, Long ciEntityId, Map<Long, CiVo> ciMap, String editMode) {
        JSONObject ciEntityResult = JSONObject.parseObject(JSONObject.toJSONString(ciVo));
        ciEntityResult.put("ciId", ciVo.getId());
        ciEntityResult.put("id", null);
        JSONObject attrEntityData = new JSONObject();
        if (ciEntityId != null) {
            ciEntityResult.put("id", ciEntityId);
        } else {
            ciEntityResult.put("uuid", UUID.randomUUID().toString().replace("-", ""));
        }
        ciEntityResult.put("editMode", editMode);
        ciEntityResult.put("attrEntityData", attrEntityData);
        JSONObject relEntityData = new JSONObject();
        ciEntityResult.put("relEntityData", relEntityData);
        //遍历入参属性 key value,转换为对应id
        for (Map.Entry<String, Object> attrEntity : entityDataParam.entrySet()) {
            String entityKey = attrEntity.getKey();
            JSONArray entityValueArray = JSONArray.parseArray(JSONArray.toJSONString(attrEntity.getValue()));
            if (attrMap.containsKey(entityKey)) {
                JSONObject ciEntityAttr = new JSONObject();
                AttrVo attrVo = attrMap.get(entityKey);
                ciEntityAttr.put("type", attrVo.getType());
                attrEntityData.put("attr_" + attrVo.getId(), ciEntityAttr);
                JSONArray valueList = new JSONArray();
                JSONArray actualValueList = new JSONArray();
                if (attrVo.getTargetCiId() != null) {
                    for (int j = 0; j < entityValueArray.size(); j++) {
                        String value = entityValueArray.getString(j);
                        if (StringUtils.isNotBlank(value)) {
                            value = value.trim();
                            List<CiEntityVo> targetCiEntityList = getCiEntityBaseInfoByName(attrVo.getTargetCiId(), value, ciMap);
                            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(targetCiEntityList)) {
                                valueList.addAll(targetCiEntityList.stream().map(CiEntityVo::getId).distinct().collect(Collectors.toList()));
                                actualValueList.addAll(targetCiEntityList.stream().map(CiEntityVo::getName).distinct().collect(Collectors.toList()));

                            } else {
                                throw new CiEntityNotFoundException(value);
                            }
                        }
                    }
                } else {
                    valueList.add(entityValueArray.getString(0));
                }
                ciEntityAttr.put("valueList", valueList);
                ciEntityAttr.put("actualValueList", actualValueList);
            }
            //遍历入参关系 key value,转换为对应id

            if (relMap.containsKey(entityKey)) {
                JSONObject ciEntityRel = new JSONObject();
                RelVo relVo = relMap.get(entityKey);
                relEntityData.put("rel" + relVo.getDirection() + "_" + relVo.getId(), ciEntityRel);
                JSONArray valueList = new JSONArray();
                for (int j = 0; j < entityValueArray.size(); j++) {
                    String value = entityValueArray.getString(j);
                    if (StringUtils.isNotBlank(value)) {
                        value = value.trim();
                        List<CiEntityVo> targetCiEntityList = getCiEntityBaseInfoByName(relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId(), value, ciMap);
                        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(targetCiEntityList)) {
                            for (CiEntityVo entity : targetCiEntityList) {
                                JSONObject valueRel = new JSONObject();
                                valueList.add(valueRel);
                                valueRel.put("ciId", entity.getCiId());
                                valueRel.put("ciEntityId", entity.getId());
                                valueRel.put("ciEntityName", entity.getName());
                                valueRel.put("action", RelActionType.REPLACE.getValue());
                            }
                        } else {
                            throw new CiEntityNotFoundException(value);
                        }
                    }
                }
                ciEntityRel.put("valueList", valueList);
            }
        }
        return ciEntityResult;
    }

    /**
     * 根据配置项值获取配置项
     *
     * @param ciId  配置模型id
     * @param name  配置项值
     * @param ciMap 缓存
     * @return 配置项列表
     */
    private List<CiEntityVo> getCiEntityBaseInfoByName(Long ciId, String name, Map<Long, CiVo> ciMap) {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (!ciMap.containsKey(ciId)) {
            ciMap.put(ciId, ciCrossoverMapper.getCiById(ciId));
        }
        CiVo ciVo = ciMap.get(ciId);
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(ciId);
        ciEntityVo.setName(name);
        if (ciVo.getIsVirtual().equals(0)) {
            return ciEntityCrossoverMapper.getCiEntityBaseInfoByName(ciEntityVo);
        } else {
            return ciEntityCrossoverMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
        }
    }

    @Override
    public JSONObject example() {
        String json = "{\n" +
                "  \"needCommit\": true,\n" +
                "  \"ciEntityList\": [\n" +
                "    {\n" +
                "      \"editMode\": \"partial\",\n" +
                "      \"name\": \"名称20220719\",\n" +
                "      \"ciName\": \"APP\",\n" +
                "      \"entityData\": {\n" +
                "        \"owner\": [\n" +
                "          \"admin\"\n" +
                "        ],\n" +
                "        \"bg\": [\n" +
                "          \"运维管理中心\"\n" +
                "        ],\n" +
                "        \"maintenance_window\": [\n" +
                "          \"2022-07-20\"\n" +
                "        ],\n" +
                "        \"abbrName\": [\n" +
                "          \"system20220719\"\n" +
                "        ],\n" +
                "        \"name\": [\n" +
                "          \"名称20220719\"\n" +
                "        ],\n" +
                "        \"description\": [\n" +
                "          \"描述s\"\n" +
                "        ],\n" +
                "        \"data_center\": [\n" +
                "          \"MAIN\"\n" +
                "        ],\n" +
                "        \"APPIns\": [\n" +
                "          \"a\"\n" +
                "        ],\n" +
                "        \"state\": [\n" +
                "          \"下线中\"\n" +
                "        ],\n" +
                "        \"APPComponent\": [\n" +
                "          \"20220719\"\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        return JSONObject.parseObject(json);
    }

}
