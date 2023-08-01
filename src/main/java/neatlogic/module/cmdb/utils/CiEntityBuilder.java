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

package neatlogic.module.cmdb.utils;

import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityMultipleException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CiEntityBuilder {
    private final CiEntityVo paramCiEntityVo;
    private final List<HashMap<String, Object>> resultList;
    private Map<Long, AttrVo> attrMap;
    private Map<Long, RelVo> relMap;
    private final CiVo ciVo;
    private boolean flattenAttr = false;

    private CiEntityBuilder(Builder builder) {
        resultList = builder.resultList;
        List<AttrVo> attrList = builder.attrList;
        List<RelVo> relList = builder.relList;
        flattenAttr = builder.flattenAttr;
        ciVo = builder.ciVo;
        paramCiEntityVo = builder.ciEntityVo;
        if (CollectionUtils.isNotEmpty(attrList)) {
            attrMap = new HashMap<>();
            for (AttrVo attrVo : attrList) {
                attrMap.put(attrVo.getId(), attrVo);
            }
        }
        if (CollectionUtils.isNotEmpty(relList)) {
            relMap = new HashMap<>();
            for (RelVo relVo : relList) {
                relMap.put(relVo.getId(), relVo);
            }
        }
    }

    public List<CiEntityVo> getCiEntityList() {
        Map<Long, CiEntityVo> ciEntityMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(resultList)) {
            for (Map<String, Object> result : resultList) {
                //由于mybatis去掉了值为null的属性字段，为了避免比对时数据不一致，所以要补充不存在的属性
                if (flattenAttr) {
                    for (Long attrId : attrMap.keySet()) {
                        if (!result.containsKey("attr_" + attrId)) {
                            result.put("attr_" + attrId, null);
                        }
                    }
                }
                CiEntityVo ciEntityVo;

                Long id = result.get("id") != null ? Long.valueOf(String.valueOf(result.get("id"))) : null;
                String uuid = result.get("uuid") != null ? String.valueOf(result.get("uuid")) : null;
                String name = result.get("name") != null ? String.valueOf(result.get("name")) : null;
                String status = result.get("status") != null ? String.valueOf(result.get("status")) : null;
                String fcu = result.get("fcu") != null ? String.valueOf(result.get("fcu")) : null;
                Date fcd = result.get("fcd") != null ? (Date) (result.get("fcd")) : null;
                String lcu = result.get("lcu") != null ? String.valueOf(result.get("lcu")) : null;
                Date lcd = result.get("lcd") != null ? (Date) (result.get("lcd")) : null;
                Long typeId = result.get("typeId") != null ? Long.valueOf(String.valueOf(result.get("typeId"))) : null;
                String typeName = result.get("typeName") != null ? String.valueOf(result.get("typeName")) : null;
                String ciName = result.get("ciName") != null ? String.valueOf(result.get("ciName")) : null;
                String ciIcon = result.get("ciIcon") != null ? String.valueOf(result.get("ciIcon")) : null;
                String ciLabel = result.get("ciLabel") != null ? String.valueOf(result.get("ciLabel")) : null;
                Long ciId = result.get("ciId") != null ? Long.valueOf(String.valueOf(result.get("ciId"))) : null;

                Date inspectTime = result.get("inspectTime") != null ? new Date(Long.parseLong(String.valueOf(result.get("inspectTime")))) : null;
                Date monitorTime = result.get("monitorTime") != null ? new Date(Long.parseLong(String.valueOf(result.get("monitorTime")))) : null;
                Date renewTime = result.get("renewTime") != null ? new Date(Long.parseLong(String.valueOf(result.get("renewTime")))) : null;
                String inspectStatus = result.get("inspectStatus") != null ? String.valueOf(result.get("inspectStatus")) : null;
                String monitorStatus = result.get("monitorStatus") != null ? String.valueOf(result.get("monitorStatus")) : null;
                String account = result.get("account") != null ? String.valueOf(result.get("account")) : null;

                if (!ciEntityMap.containsKey(id)) {
                    ciEntityVo = new CiEntityVo();
                    ciEntityVo.setCiId(ciId);
                    ciEntityVo.setId(id);
                    ciEntityVo.setUuid(uuid);
                    ciEntityVo.setName(name);
                    ciEntityVo.setStatus(status);
                    ciEntityVo.setTypeId(typeId);
                    ciEntityVo.setTypeName(typeName);
                    ciEntityVo.setCiName(ciName);
                    ciEntityVo.setCiLabel(ciLabel);
                    ciEntityVo.setCiIcon(ciIcon);
                    ciEntityVo.setFcu(fcu);
                    ciEntityVo.setFcd(fcd);
                    ciEntityVo.setLcu(lcu);
                    ciEntityVo.setLcd(lcd);
                    ciEntityVo.setInspectStatus(inspectStatus);
                    ciEntityVo.setInspectTime(inspectTime);
                    ciEntityVo.setMonitorStatus(monitorStatus);
                    ciEntityVo.setMonitorTime(monitorTime);
                    ciEntityVo.setRenewTime(renewTime);
                    ciEntityVo.setAccount(account);
                    ciEntityMap.put(id, ciEntityVo);
                } else {
                    ciEntityVo = ciEntityMap.get(id);
                }

                for (String key : result.keySet()) {
                    if (key.startsWith("attr_")) {
                        Long attrId = Long.parseLong(key.substring(5));
                        Object value = result.get(key);
                        //处理属性
                        AttrVo attrVo = attrMap.get(attrId);
                        if (!ciEntityVo.hasAttrEntityData(attrId)) {
                            if (attrVo != null) {
                                ciEntityVo.addAttrEntityData(attrId, buildAttrObj(ciEntityVo.getId(), attrVo, value));
                            }
                        } else {
                            JSONArray valueList = new JSONArray();
                            //例如附件型参数有可能是个数组，所以先尝试做转换，不行再当字符串处理
                            try {
                                valueList = JSONArray.parseArray(value.toString());
                            } catch (Exception ignored) {
                                valueList.add(value);
                            }
                            //JSONArray actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                            //ciEntityVo.addAttrEntityDataValue(attrId, valueList, actualValueList);
                            ciEntityVo.addAttrEntityDataValue(attrId, valueList);
                        }
                    } else if (key.startsWith("rel")) {//字段名范例：relfrom_12313131323或relto_131231231313
                        if (!key.contains("#")) {
                            String[] ks = key.split("_");
                            String direction = ks[0].replace("rel", "");
                            Long relId = Long.parseLong(ks[1]);
                            RelVo relVo = relMap.get(relId);
                            if (!ciEntityVo.hasRelEntityData(relId, direction)) {
                                if (relVo != null) {
                                    ciEntityVo.addRelEntityData(relId, direction, buildRelObj(ciEntityVo.getId(), relVo, direction, result, key));
                                }
                            } else {
                                //限制最大返回关系，数据库已经控制，无需再处理
                                //if (paramCiEntityVo.getMaxRelEntityCount() == null || ciEntityVo.getRelEntityByRelIdAndDirection(relId, direction).size() <= paramCiEntityVo.getMaxRelEntityCount()) {
                                JSONObject valueDataObj = new JSONObject();
                                valueDataObj.put("ciId", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                valueDataObj.put("ciEntityId", result.get(key));
                                valueDataObj.put("ciEntityName", result.get(key + "#name"));
                                ciEntityVo.addRelEntityDataValue(relId, direction, valueDataObj);
                                //}
                            }
                        }
                    }
                }
            }
        }
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        for (Long key : ciEntityMap.keySet()) {
            CiEntityVo ciEntityVo = ciEntityMap.get(key);
            JSONObject attrObj = ciEntityVo.getAttrEntityData();
            if (MapUtils.isNotEmpty(attrObj)) {
                for (String attrKey : attrObj.keySet()) {
                    Long attrId = Long.parseLong(attrKey.replace("attr_", ""));
                    AttrVo attrVo = attrMap.get(attrId);
                    JSONArray valueList = attrObj.getJSONObject(attrKey).getJSONArray("valueList");
                    JSONArray actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                    attrObj.getJSONObject(attrKey).put("actualValueList", actualValueList);
                }
            }
            ciEntityList.add(ciEntityVo);
        }
        return ciEntityList;
    }

    public CiEntityVo getCiEntity() {
        CiEntityVo ciEntityVo = null;
        if (CollectionUtils.isNotEmpty(resultList)) {
            for (Map<String, Object> result : resultList) {
                //由于mybatis去掉了值为null的属性字段，为了避免比对时数据不一致，所以要补充不存在的属性
                if (flattenAttr) {
                    if (MapUtils.isNotEmpty(attrMap)) {
                        for (Long attrId : attrMap.keySet()) {
                            if (!result.containsKey("attr_" + attrId)) {
                                result.put("attr_" + attrId, null);
                            }
                        }
                    }
                }

                Long id = result.get("id") != null ? Long.valueOf(String.valueOf(result.get("id"))) : null;
                String name = result.get("name") != null ? String.valueOf(result.get("name")) : null;
                String uuid = result.get("uuid") != null ? String.valueOf(result.get("uuid")) : null;
                String status = result.get("status") != null ? String.valueOf(result.get("status")) : null;
                String fcu = result.get("fcu") != null ? String.valueOf(result.get("fcu")) : null;
                Date fcd = result.get("fcd") != null ? (Date) result.get("fcd") : null;
                String lcu = result.get("lcu") != null ? String.valueOf(result.get("lcu")) : null;
                Date lcd = result.get("lcd") != null ? (Date) result.get("lcd") : null;
                Long typeId = result.get("typeId") != null ? Long.valueOf(String.valueOf(result.get("typeId"))) : null;
                String typeName = result.get("typeName") != null ? String.valueOf(result.get("typeName")) : null;
                String ciName = result.get("ciName") != null ? String.valueOf(result.get("ciName")) : null;
                String ciIcon = result.get("ciIcon") != null ? String.valueOf(result.get("ciIcon")) : null;
                String ciLabel = result.get("ciLabel") != null ? String.valueOf(result.get("ciLabel")) : null;
                Long ciId = result.get("ciId") != null ? Long.valueOf(String.valueOf(result.get("ciId"))) : null;
                Date inspectTime = result.get("inspectTime") != null ? new Date(Long.parseLong(String.valueOf(result.get("inspectTime")))) : null;
                Date monitorTime = result.get("monitorTime") != null ? new Date(Long.parseLong(String.valueOf(result.get("monitorTime")))) : null;
                Date renewTime = result.get("renewTime") != null ? new Date(Long.parseLong(String.valueOf(result.get("renewTime")))) : null;
                String inspectStatus = result.get("inspectStatus") != null ? String.valueOf(result.get("inspectStatus")) : null;
                String monitorStatus = result.get("monitorStatus") != null ? String.valueOf(result.get("monitorStatus")) : null;

                if (ciEntityVo == null) {
                    ciEntityVo = new CiEntityVo();
                    ciEntityVo.setCiId(ciId);
                    ciEntityVo.setId(id);
                    ciEntityVo.setUuid(uuid);
                    ciEntityVo.setName(name);
                    ciEntityVo.setStatus(status);
                    ciEntityVo.setTypeId(typeId);
                    ciEntityVo.setTypeName(typeName);
                    ciEntityVo.setCiName(ciName);
                    ciEntityVo.setCiLabel(ciLabel);
                    ciEntityVo.setCiIcon(ciIcon);
                    ciEntityVo.setFcu(fcu);
                    ciEntityVo.setFcd(fcd);
                    ciEntityVo.setLcu(lcu);
                    ciEntityVo.setLcd(lcd);
                    ciEntityVo.setInspectStatus(inspectStatus);
                    ciEntityVo.setInspectTime(inspectTime);
                    ciEntityVo.setMonitorStatus(monitorStatus);
                    ciEntityVo.setMonitorTime(monitorTime);
                    ciEntityVo.setRenewTime(renewTime);
                } else {
                    if (!ciEntityVo.getId().equals(id)) {
                        throw new CiEntityMultipleException(id);
                    }
                }

                for (String key : result.keySet()) {
                    if (key.startsWith("attr_")) {
                        Long attrId = Long.parseLong(key.substring(5));
                        Object value = result.get(key);
                        //处理属性
                        if (key.startsWith("attr_")) {
                            AttrVo attrVo = attrMap.get(attrId);
                            if (!ciEntityVo.hasAttrEntityData(attrId)) {
                                if (attrVo != null) {
                                    ciEntityVo.addAttrEntityData(attrId, buildAttrObj(ciEntityVo.getId(), attrVo, value));
                                }
                            } else {
                                JSONArray valueList = new JSONArray();
                                //例如附件型参数有可能是个数组，所以先尝试做转换，不行再当字符串处理
                                if (value.toString().startsWith("[")) {
                                    try {
                                        valueList = JSONArray.parseArray(value.toString());
                                    } catch (Exception ignored) {
                                        valueList.add(value);
                                    }
                                } else {
                                    valueList.add(value);
                                }
                                //JSONArray actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                                //ciEntityVo.addAttrEntityDataValue(attrId, valueList, actualValueList);
                                ciEntityVo.addAttrEntityDataValue(attrId, valueList);
                            }
                        }
                    } else if (key.startsWith("rel")) {//字段名范例：relfrom_12313131323或relto_131231231313
                        if (!key.contains("#")) {
                            String[] ks = key.split("_");
                            Long relId = Long.parseLong(ks[1]);
                            String direction = ks[0].replace("rel", "");
                            RelVo relVo = relMap.get(relId);
                            if (!ciEntityVo.hasRelEntityData(relId, direction)) {
                                if (relVo != null) {
                                    ciEntityVo.addRelEntityData(relId, direction, buildRelObj(ciEntityVo.getId(), relVo, direction, result, key));
                                }
                            } else {
                                JSONObject valueDataObj = new JSONObject();
                                valueDataObj.put("ciId", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                valueDataObj.put("ciEntityId", result.get(key));
                                valueDataObj.put("id", result.get(key + "#id"));
                                valueDataObj.put("ciEntityName", result.get(key + "#name"));
                                valueDataObj.put("validDay", result.get(key + "#validDay"));
                                if (StringUtils.isBlank(valueDataObj.getString("ciEntityName"))) {
                                    valueDataObj.put("ciEntityName", "无名配置项");
                                }
                                ciEntityVo.addRelEntityDataValue(relId, direction, valueDataObj);
                            }
                        }
                    }
                }
            }
        }
        //转换引用属性的真实值
        if (ciEntityVo != null) {
            JSONObject attrObj = ciEntityVo.getAttrEntityData();
            if (MapUtils.isNotEmpty(attrObj)) {
                for (String attrKey : attrObj.keySet()) {
                    Long attrId = Long.parseLong(attrKey.replace("attr_", ""));
                    AttrVo attrVo = attrMap.get(attrId);
                    JSONArray valueList = attrObj.getJSONObject(attrKey).getJSONArray("valueList");
                    JSONArray actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                    attrObj.getJSONObject(attrKey).put("actualValueList", actualValueList);
                }
            }
        }
        return ciEntityVo;
    }


    public static class Builder {
        private final CiEntityVo ciEntityVo;
        private final List<HashMap<String, Object>> resultList;
        private final List<AttrVo> attrList;
        private final List<RelVo> relList;
        private final CiVo ciVo;
        private boolean flattenAttr = false;

        public Builder(CiEntityVo _ciEntityVo, List<HashMap<String, Object>> _resultList, CiVo _ciVo, List<AttrVo> _attrList, List<RelVo> _relList) {
            ciEntityVo = _ciEntityVo;
            resultList = _resultList;
            attrList = _attrList;
            relList = _relList;
            ciVo = _ciVo;
        }

        public Builder isFlattenAttr(boolean flattenAttr) {
            this.flattenAttr = flattenAttr;
            return this;
        }

        public CiEntityBuilder build() {
            return new CiEntityBuilder(this);
        }
    }

    public static JSONObject buildAttrObj(Long ciEntityId, AttrVo attrVo, JSONArray valueList, JSONArray actualValueList) {
        JSONObject attrObj = new JSONObject();
        attrObj.put("ciEntityId", ciEntityId);
        attrObj.put("attrId", attrVo.getId());
        attrObj.put("type", attrVo.getType());
        attrObj.put("name", attrVo.getName());
        attrObj.put("label", attrVo.getLabel());
        attrObj.put("config", attrVo.getConfig(true));//克隆一个config对象，避免json序列化出错
        attrObj.put("targetCiId", attrVo.getTargetCiId());
        attrObj.put("ciId", attrVo.getCiId());
        attrObj.put("valueList", valueList);
        attrObj.put("actualValueList", actualValueList);
        return attrObj;
    }

    private JSONObject buildAttrObj(Long ciEntityId, AttrVo attrVo, Object value) {
        JSONObject attrObj = new JSONObject();
        attrObj.put("ciEntityId", ciEntityId);
        attrObj.put("attrId", attrVo.getId());
        attrObj.put("type", attrVo.getType());
        attrObj.put("name", attrVo.getName());
        attrObj.put("label", attrVo.getLabel());
        attrObj.put("config", attrVo.getConfig(true));//克隆一个config对象，避免json序列化出错
        attrObj.put("targetCiId", attrVo.getTargetCiId());
        attrObj.put("ciId", attrVo.getCiId());
        JSONArray valueList = new JSONArray();
        if (value != null) {
            if (value instanceof JSONArray) {
                valueList.addAll((JSONArray) value);
            } else if (value.toString().startsWith("[")) {
                if (attrVo.getType().equals("file")) {
                    try {
                        //例如附件型参数有可能是个数组，所以先尝试做转换，不行再当字符串处理
                        valueList = JSONArray.parseArray(value.toString());
                    } catch (Exception ignored) {
                        valueList.add(value);
                    }
                } else {
                    valueList.add(value);
                }
            } else {
                valueList.add(value);
            }
        }
        attrObj.put("valueList", valueList);
        //不在这里处理的原因是为了等所有值都设完以后一起处理，避免单个值重复查
        //attrObj.put("actualValueList", AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList));
        return attrObj;
    }

    public static JSONObject buildRelObj(Long ciEntityId, RelVo relVo, List<RelEntityVo> relEntityList) {
        JSONObject relObj = new JSONObject();
        relObj.put("ciEntityId", ciEntityId);
        relObj.put("name", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToName() : relVo.getFromName());
        relObj.put("label", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToLabel() : relVo.getFromLabel());
        relObj.put("relId", relVo.getId());
        relObj.put("direction", relVo.getDirection());
        relObj.put("ciId", relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
        JSONArray valueList = new JSONArray();
        for (RelEntityVo relEntityVo : relEntityList) {
            JSONObject valueDataObj = new JSONObject();
            valueDataObj.put("id", relEntityVo.getRelId());
            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                valueDataObj.put("ciId", relEntityVo.getToCiId());
                valueDataObj.put("ciEntityId", relEntityVo.getToCiEntityId());
                valueDataObj.put("validDay", relEntityVo.getValidDay());
                if (StringUtils.isNotBlank(relEntityVo.getToCiEntityName())) {
                    valueDataObj.put("ciEntityName", relEntityVo.getToCiEntityName());
                } else {
                    valueDataObj.put("ciEntityName", "无名配置项");
                }
            } else {
                valueDataObj.put("ciId", relEntityVo.getFromCiId());
                valueDataObj.put("ciEntityId", relEntityVo.getFromCiEntityId());
                valueDataObj.put("validDay", relEntityVo.getValidDay());
                if (StringUtils.isNotBlank(relEntityVo.getFromCiEntityName())) {
                    valueDataObj.put("ciEntityName", relEntityVo.getFromCiEntityName());
                } else {
                    valueDataObj.put("ciEntityName", "无名配置项");
                }
            }
            valueList.add(valueDataObj);
        }

        relObj.put("valueList", valueList);
        return relObj;
    }

    private static JSONObject buildRelObj(Long ciEntityId, RelVo relVo, String direction, Map<String, Object> result, String key) {
        JSONObject relObj = new JSONObject();
        relObj.put("ciEntityId", ciEntityId);
        relObj.put("name", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToName() : relVo.getFromName());
        relObj.put("label", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToLabel() : relVo.getFromLabel());
        relObj.put("relId", relVo.getId());
        relObj.put("direction", direction);
        relObj.put("ciId", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
        JSONObject valueDataObj = new JSONObject();
        valueDataObj.put("id", result.get(key + "#id"));
        valueDataObj.put("ciId", result.get(key + "#ciId"));
        valueDataObj.put("ciEntityId", result.get(key));
        valueDataObj.put("validDay", result.get(key + "#validDay"));
        valueDataObj.put("ciEntityName", result.get(key + "#name"));
        if (StringUtils.isBlank(valueDataObj.getString("ciEntityName"))) {
            valueDataObj.put("ciEntityName", "无名配置项");
        }
        relObj.put("valueList", new ArrayList<JSONObject>() {{
            this.add(valueDataObj);
        }});
        return relObj;
    }
}
