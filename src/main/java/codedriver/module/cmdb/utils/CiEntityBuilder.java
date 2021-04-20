/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.utils;

import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityMultipleException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class CiEntityBuilder {
    private final List<Map<String, Object>> resultList;
    private Map<Long, AttrVo> attrMap;
    private Map<Long, RelVo> relMap;
    private final CiVo ciVo;

    private CiEntityBuilder(Builder builder) {
        resultList = builder.resultList;
        List<AttrVo> attrList = builder.attrList;
        List<RelVo> relList = builder.relList;
        ciVo = builder.ciVo;
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
                CiEntityVo ciEntityVo;

                Long id = result.get("id") != null ? Long.valueOf(String.valueOf(result.get("id"))) : null;
                String name = result.get("name") != null ? String.valueOf(result.get("name")) : null;
                String status = result.get("status") != null ? String.valueOf(result.get("status")) : null;
                String fcu = result.get("fcu") != null ? String.valueOf(result.get("fcu")) : null;
                Date fcd = result.get("fcd") != null ? (Date) (result.get("fcd")) : null;
                String lcu = result.get("lcu") != null ? String.valueOf(result.get("lcu")) : null;
                Date lcd = result.get("lcd") != null ? (Date) (result.get("lcd")) : null;

                if (!ciEntityMap.containsKey(id)) {
                    ciEntityVo = new CiEntityVo();
                    ciEntityVo.setCiId(ciVo.getId());
                    ciEntityVo.setCiLabel(ciVo.getLabel());
                    ciEntityVo.setCiName(ciVo.getName());
                    ciEntityVo.setId(id);
                    ciEntityVo.setName(name);
                    ciEntityVo.setStatus(status);
                    ciEntityVo.setFcu(fcu);
                    ciEntityVo.setFcd(fcd);
                    ciEntityVo.setLcu(lcu);
                    ciEntityVo.setLcd(lcd);
                    ciEntityMap.put(id, ciEntityVo);
                } else {
                    ciEntityVo = ciEntityMap.get(id);
                }

                for (String key : result.keySet()) {
                    if (key.startsWith("attr_")) {
                        Long attrId = Long.parseLong(key.substring(5));

                        String value = result.get(key).toString();
                        //处理属性
                        if (key.startsWith("attr_")) {
                            AttrVo attrVo = attrMap.get(attrId);
                            if (!ciEntityVo.hasAttrEntityData(attrId)) {
                                if (attrVo != null) {
                                    ciEntityVo.addAttrEntityData(attrId, buildAttrObj(attrVo, value));
                                }
                            } else {
                                JSONArray valueList = new JSONArray();
                                valueList.add(value);
                                JSONArray actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                                ciEntityVo.addAttrEntityDataValue(attrId, valueList, actualValueList);
                            }
                        }
                    } else if (key.startsWith("rel")) {//字段名范例：relfrom_12313131323或relto_131231231313
                        if (!key.contains("#")) {
                            String[] ks = key.split("_");
                            String direction = ks[0].replace("rel", "");
                            Long relId = Long.parseLong(ks[1]);
                            RelVo relVo = relMap.get(relId);
                            if (!ciEntityVo.hasRelEntityData(relId, direction)) {
                                if (relVo != null) {
                                    ciEntityVo.addRelEntityData(relId, direction, buildRelObj(relVo, direction, result, key));
                                }
                            } else {
                                JSONObject valueDataObj = new JSONObject();
                                valueDataObj.put("ciId", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                valueDataObj.put("ciEntityId", result.get(key));
                                valueDataObj.put("ciEntityName", result.get(key + "#name"));
                                ciEntityVo.addRelEntityDataValue(relId, direction, valueDataObj);
                            }
                        }
                    }
                }
            }
        }
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        for (Long key : ciEntityMap.keySet()) {
            ciEntityList.add(ciEntityMap.get(key));
        }
        return ciEntityList;
    }

    public CiEntityVo getCiEntity() {
        CiEntityVo ciEntityVo = null;
        if (CollectionUtils.isNotEmpty(resultList)) {
            for (Map<String, Object> result : resultList) {
                Long id = result.get("id") != null ? Long.valueOf(String.valueOf(result.get("id"))) : null;
                String name = result.get("name") != null ? String.valueOf(result.get("name")) : null;
                String status = result.get("status") != null ? String.valueOf(result.get("status")) : null;
                String fcu = result.get("fcu") != null ? String.valueOf(result.get("fcu")) : null;
                Date fcd = result.get("fcd") != null ? (Date) result.get("fcd") : null;
                String lcu = result.get("lcu") != null ? String.valueOf(result.get("lcu")) : null;
                Date lcd = result.get("lcd") != null ? (Date) result.get("lcd") : null;


                if (ciEntityVo == null) {
                    ciEntityVo = new CiEntityVo();
                    ciEntityVo.setCiId(ciVo.getId());
                    ciEntityVo.setCiLabel(ciVo.getLabel());
                    ciEntityVo.setCiName(ciVo.getName());
                    ciEntityVo.setId(id);
                    ciEntityVo.setName(name);
                    ciEntityVo.setStatus(status);
                    ciEntityVo.setFcu(fcu);
                    ciEntityVo.setFcd(fcd);
                    ciEntityVo.setLcu(lcu);
                    ciEntityVo.setLcd(lcd);
                } else {
                    if (!ciEntityVo.getId().equals(id)) {
                        throw new CiEntityMultipleException(id);
                    }
                }

                for (String key : result.keySet()) {
                    if (key.startsWith("attr_")) {
                        Long attrId = Long.parseLong(key.substring(5));
                        String value = result.get(key).toString();
                        //处理属性
                        if (key.startsWith("attr_")) {
                            AttrVo attrVo = attrMap.get(attrId);
                            if (!ciEntityVo.hasAttrEntityData(attrId)) {
                                if (attrVo != null) {
                                    ciEntityVo.addAttrEntityData(attrId, buildAttrObj(attrVo, value));
                                }
                            } else {
                                JSONArray valueList = new JSONArray();
                                valueList.add(value);
                                JSONArray actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                                ciEntityVo.addAttrEntityDataValue(attrId, valueList, actualValueList);
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
                                    ciEntityVo.addRelEntityData(relId, direction, buildRelObj(relVo, direction, result, key));
                                }
                            } else {
                                JSONObject valueDataObj = new JSONObject();
                                valueDataObj.put("ciId", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
                                valueDataObj.put("ciEntityId", result.get(key));
                                valueDataObj.put("ciEntityName", result.get(key + "#name"));
                                ciEntityVo.addRelEntityDataValue(relId, direction, valueDataObj);
                            }
                        }
                    }
                }
            }
        }
        return ciEntityVo;
    }


    public static class Builder {
        private final List<Map<String, Object>> resultList;
        private final List<AttrVo> attrList;
        private final List<RelVo> relList;
        private final CiVo ciVo;

        public Builder(List<Map<String, Object>> _resultList, CiVo _ciVo, List<AttrVo> _attrList, List<RelVo> _relList) {
            resultList = _resultList;
            attrList = _attrList;
            relList = _relList;
            ciVo = _ciVo;
        }

        public CiEntityBuilder build() {
            return new CiEntityBuilder(this);
        }
    }

    private JSONObject buildAttrObj(AttrVo attrVo, String value) {
        JSONObject attrObj = new JSONObject();
        attrObj.put("type", attrVo.getType());
        attrObj.put("name", attrVo.getName());
        attrObj.put("label", attrVo.getLabel());
        attrObj.put("config", attrVo.getConfig(true));//克隆一个config对象，避免json序列化出错
        attrObj.put("targetCiId", attrVo.getTargetCiId());
        JSONArray valueList = new JSONArray();
        valueList.add(value);
        attrObj.put("valueList", valueList);
        attrObj.put("actualValueList", AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList));
        return attrObj;
    }

    private JSONObject buildRelObj(RelVo relVo, String direction, Map<String, Object> result, String key) {
        JSONObject relObj = new JSONObject();
        relObj.put("name", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToName() : relVo.getFromName());
        relObj.put("label", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToLabel() : relVo.getFromLabel());
        relObj.put("relId", relVo.getId());
        relObj.put("direction", direction);
        JSONObject valueDataObj = new JSONObject();
        valueDataObj.put("ciId", direction.equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId());
        valueDataObj.put("ciEntityId", result.get(key));
        valueDataObj.put("ciEntityName", result.get(key + "#name"));
        relObj.put("valueList", new ArrayList<JSONObject>() {{
            this.add(valueDataObj);
        }});
        return relObj;
    }
}