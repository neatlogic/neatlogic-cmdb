/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.cmdb.matrix.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.*;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrFilterVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.module.cmdb.dao.mapper.ci.*;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.matrix.dao.mapper.MatrixCiEntityMapper;
import neatlogic.module.cmdb.matrix.dto.MatrixCiEntitySearchVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

//@Service
public class MatrixCiEntityServiceImpl implements MatrixCiEntityService {

    @Resource
    private MatrixCiEntityMapper matrixCiEntityMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiTypeMapper ciTypeMapper;
    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Override
    public List<Map<String, Object>> searchCiEntityList(MatrixCiEntitySearchVo matrixCiEntitySearchVo) {
        CiVo ciVo = ciMapper.getCiById(matrixCiEntitySearchVo.getCiId());
        if (ciVo == null) {
            throw new CiNotFoundException(matrixCiEntitySearchVo.getCiId());
        }
        matrixCiEntitySearchVo.setIsVirtual(ciVo.getIsVirtual());
        matrixCiEntitySearchVo.setFromCi(ciVo);
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(new GlobalAttrVo());


        Map<Long, CiVo> joinCiMap = new LinkedHashMap<>();
        Map<Long, AttrVo> joinAttrMap = new LinkedHashMap<>();
        List<AttrVo> showAttrList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(matrixCiEntitySearchVo.getAttrIdList())) {
            for (Long attrId : matrixCiEntitySearchVo.getAttrIdList()) {
                for (AttrVo attrVo : attrList) {
                    if (Objects.equals(attrVo.getId(), attrId)) {
                        showAttrList.add(attrVo);
                        if (Objects.equals(attrVo.isNeedTargetCi(), true)) {
                            if (!joinAttrMap.containsKey(attrVo.getId())) {
                                joinAttrMap.put(attrVo.getId(), attrVo);
                            }
                        } else {
                            if (Objects.equals(ciVo.getIsVirtual(), 0)) {
                                if (!joinCiMap.containsKey(attrVo.getCiId())) {
                                    for (CiVo ci : ciList) {
                                        if (Objects.equals(ci.getId(), attrVo.getCiId())) {
                                            joinCiMap.put(ci.getId(), ci);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        matrixCiEntitySearchVo.setShowAttrList(showAttrList);
        matrixCiEntitySearchVo.setJoinAttrList(new ArrayList<>(joinAttrMap.values()));
        /*
        如果有属性过滤，则根据属性补充关键信息
         */
        if (CollectionUtils.isNotEmpty(matrixCiEntitySearchVo.getAttrFilterList())) {
            Iterator<AttrFilterVo> itAttrFilter = matrixCiEntitySearchVo.getAttrFilterList().iterator();
            while (itAttrFilter.hasNext()) {
                AttrFilterVo attrFilterVo = itAttrFilter.next();
                boolean isExists = false;
                for (AttrVo attrVo : attrList) {
                    if (attrVo.getId().equals(attrFilterVo.getAttrId())) {
                        attrFilterVo.setCiId(attrVo.getCiId());
                        attrFilterVo.setType(attrVo.getType());
                        attrFilterVo.setNeedTargetCi(attrVo.isNeedTargetCi());
                        isExists = true;
                        if (Objects.equals(attrVo.isNeedTargetCi(), false)) {
                            if (Objects.equals(ciVo.getIsVirtual(), 0)) {
                                if (!joinCiMap.containsKey(attrVo.getCiId())) {
                                    for (CiVo ci : ciList) {
                                        if (Objects.equals(ci.getId(), attrVo.getCiId())) {
                                            joinCiMap.put(ci.getId(), ci);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
                if (!isExists) {
                    itAttrFilter.remove();
                }
            }
        }
        matrixCiEntitySearchVo.setJoinCiList(new ArrayList<>(joinCiMap.values()));
        Map<Long, RelVo> joinRelMap = new LinkedHashMap<>();
        List<RelVo> showRelList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(matrixCiEntitySearchVo.getRelIdList())) {
            for (Long relId : matrixCiEntitySearchVo.getRelIdList()) {
                for (RelVo relVo : relList) {
                    if (relVo.getId().equals(relId)) {
                        showRelList.add(relVo);
                        if (!joinRelMap.containsKey(relVo.getId())) {
                            joinRelMap.put(relVo.getId(), relVo);
                        }
                        break;
                    }
                }
            }
        }
        matrixCiEntitySearchVo.setShowRelList(showRelList);
        matrixCiEntitySearchVo.setJoinRelList(new ArrayList<>(joinRelMap.values()));
        if (CollectionUtils.isNotEmpty(matrixCiEntitySearchVo.getRelFilterList())) {
            Iterator<RelFilterVo> itRelFilter = matrixCiEntitySearchVo.getRelFilterList().iterator();
            while (itRelFilter.hasNext()) {
                RelFilterVo relFilterVo = itRelFilter.next();
                boolean isExists = false;
                for (RelVo relVo : relList) {
                    if (relVo.getId().equals(relFilterVo.getRelId()) && relVo.getDirection().equals(relFilterVo.getDirection())) {
                        isExists = true;
                        break;
                    }
                }
                if (!isExists) {
                    itRelFilter.remove();
                }
            }
        }

        Map<Long, GlobalAttrVo> joinGlobalAttrMap = new LinkedHashMap<>();
        List<GlobalAttrVo> showGlobalAttrList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(matrixCiEntitySearchVo.getGlobalAttrIdList())) {
            for (Long globalAttrId : matrixCiEntitySearchVo.getGlobalAttrIdList()) {
                for (GlobalAttrVo globalAttrVo : globalAttrList) {
                    if (globalAttrVo.getId().equals(globalAttrId)) {
                        showGlobalAttrList.add(globalAttrVo);
                        if (!joinGlobalAttrMap.containsKey(globalAttrVo.getId())) {
                            joinGlobalAttrMap.put(globalAttrVo.getId(), globalAttrVo);
                        }
                        break;
                    }
                }
            }
        }
        matrixCiEntitySearchVo.setShowGlobalAttrList(showGlobalAttrList);
        matrixCiEntitySearchVo.setJoinGlobalAttrList(new ArrayList<>(joinGlobalAttrMap.values()));
        if (CollectionUtils.isNotEmpty(matrixCiEntitySearchVo.getGlobalAttrFilterList())) {
            Iterator<GlobalAttrFilterVo> itGlobalAttrFilter = matrixCiEntitySearchVo.getGlobalAttrFilterList().iterator();
            while (itGlobalAttrFilter.hasNext()) {
                GlobalAttrFilterVo globalAttrFilterVo = itGlobalAttrFilter.next();
                boolean isExists = false;
                for (GlobalAttrVo globalAttrVo : globalAttrList) {
                    if (globalAttrVo.getId().equals(globalAttrFilterVo.getAttrId())) {
                        isExists = true;
                        break;
                    }
                }
                if (!isExists) {
                    itGlobalAttrFilter.remove();
                }
            }
        }
        if (matrixCiEntitySearchVo.getNeedRowNum()) {
            int rowNum = matrixCiEntityMapper.searchCiEntityCount(matrixCiEntitySearchVo);
            matrixCiEntitySearchVo.setRowNum(rowNum);
        }
        List<Map<String, Object>> list = matrixCiEntityMapper.searchCiEntityList(matrixCiEntitySearchVo);
        if (CollectionUtils.isNotEmpty(list)) {
            return handleIdToName(list, attrList);
        }

        return new ArrayList<>();
    }

    @Override
    public AttrFilterVo convertAttrFilter(AttrVo attrVo, String expression, List<String> valueList) {
        AttrFilterVo attrFilterVo = new AttrFilterVo();
        attrFilterVo.setAttrId(attrVo.getId());
        if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NULL.getExpression())
                || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NOTNULL.getExpression())) {
            attrFilterVo.setExpression(expression);
            return attrFilterVo;
        }
        if (StringUtils.isBlank(expression)) {
            expression = neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression();
        }
        if (Objects.equals(attrVo.isNeedTargetCi(), true)) {
            CiVo targetCiVo = ciMapper.getCiById(attrVo.getTargetCiId());
            if (targetCiVo == null) {
                return null;
            }
            List<CiVo> downwardCiList = ciMapper.getDownwardCiListByLR(targetCiVo.getLft(), targetCiVo.getRht());
            Map<Long, CiVo> downwardCiMap = downwardCiList.stream().collect(Collectors.toMap(CiVo::getId, e -> e));

            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setCiId(targetCiVo.getId());
            ciEntityVo.setIdList(new ArrayList<>(downwardCiMap.keySet()));
            Set<Long> ciEntityIdSet = new HashSet<>();
            for (String value : valueList) {
                List<CiEntityVo> ciEntityList = new ArrayList<>();
                ciEntityVo.setName(value);
                if (Objects.equals(targetCiVo.getIsVirtual(), 1)) {
                    if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                            || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NL.getExpression())) {
                        ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByLikeName(ciEntityVo);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            if (StringUtils.isNotBlank(value) && value.contains(",")) {
                                String[] split = value.split(",");
                                for (String str : split) {
                                    str = str.trim();
                                    if (StringUtils.isNotBlank(str)) {
                                        ciEntityVo.setName(str);
                                        List<CiEntityVo> list = ciEntityMapper.getVirtualCiEntityBaseInfoByLikeName(ciEntityVo);
                                        ciEntityList.addAll(list);
                                    }
                                }
                            }
                        }
                    } else if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression())
                            || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                        ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            if (StringUtils.isNotBlank(value) && value.contains(",")) {
                                String[] split = value.split(",");
                                for (String str : split) {
                                    str = str.trim();
                                    if (StringUtils.isNotBlank(str)) {
                                        ciEntityVo.setName(str);
                                        List<CiEntityVo> list = ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
                                        ciEntityList.addAll(list);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                            || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NL.getExpression())) {
                        ciEntityList = ciEntityMapper.getCiEntityListByCiIdListAndLikeName(ciEntityVo);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            if (StringUtils.isNotBlank(value) && value.contains(",")) {
                                String[] split = value.split(",");
                                for (String str : split) {
                                    str = str.trim();
                                    if (StringUtils.isNotBlank(str)) {
                                        ciEntityVo.setName(str);
                                        List<CiEntityVo> list = ciEntityMapper.getCiEntityListByCiIdListAndLikeName(ciEntityVo);
                                        ciEntityList.addAll(list);
                                    }
                                }
                            }
                        }
                    } else if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression())
                            || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                        ciEntityList = ciEntityMapper.getCiEntityListByCiIdListAndName(ciEntityVo);
                        if (CollectionUtils.isEmpty(ciEntityList)) {
                            if (StringUtils.isNotBlank(value) && value.contains(",")) {
                                String[] split = value.split(",");
                                for (String str : split) {
                                    str = str.trim();
                                    if (StringUtils.isNotBlank(str)) {
                                        ciEntityVo.setName(str);
                                        List<CiEntityVo> list = ciEntityMapper.getCiEntityListByCiIdListAndName(ciEntityVo);
                                        ciEntityList.addAll(list);
                                    }
                                }
                            }
                        }
                    }
                }
                for (CiEntityVo ciEntity : ciEntityList) {
                    ciEntityIdSet.add(ciEntity.getId());
                }
            }
            List<String> newValueList = new ArrayList<>();
            if (CollectionUtils.isEmpty(ciEntityIdSet)) {
                if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                        || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression())) {
                    return null;
                }
            } else {
                List<Long> ciEntityIdList = new ArrayList<>(ciEntityIdSet);
                ciEntityIdList.sort(Comparator.naturalOrder());
                for (Long ciEntityId : ciEntityIdList) {
                    newValueList.add(ciEntityId.toString());
                }
            }
            attrFilterVo.setValueList(newValueList);
        } else if (Objects.equals(attrVo.getType(), "set")) {
            Set<String> newValueSet = new HashSet<>();
            if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                    || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NL.getExpression())) {
                List<String> memberList = new ArrayList<>();
                JSONObject config = attrVo.getConfig();
                if (MapUtils.isNotEmpty(config)) {
                    JSONArray members = config.getJSONArray("members");
                    if (CollectionUtils.isNotEmpty(members)) {
                        memberList = members.toJavaList(String.class);
                    }
                }
                for (String value : valueList) {
                    if (memberList.contains(value)) {
                        newValueSet.add(value);
                    } else if (value.contains(",")) {
                        String[] split = value.split(",");
                        for (String str : split) {
                            if (memberList.contains(str)) {
                                newValueSet.add(str);
                            }
                        }
                    }
                }
            } else if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                for (String value : valueList) {
                    newValueSet.add(value);
                    newValueSet.add(value.toLowerCase());
                    newValueSet.add(value.toUpperCase());
                }
            } else {
                newValueSet.addAll(valueList);
            }
            attrFilterVo.setValueList(new ArrayList<>(newValueSet));
        } else if (Objects.equals(attrVo.getType(), "datetimerange")) {
            List<String> newValueList = new ArrayList<>();
            for (String value : valueList) {
                List<String> strList = new ArrayList<>();
                if (value.contains("~")) {
                    String[] split = value.split("~");
                    for (String str : split) {
                        strList.add(str.trim());
                    }
                    newValueList.add(String.join(",", strList));
                } else {
                    newValueList.add(value);
                }
            }
            attrFilterVo.setValueList(newValueList);
        } else {
            Set<String> newValueSet = new HashSet<>();
            if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                for (String value : valueList) {
                    newValueSet.add(value);
                    newValueSet.add(value.toLowerCase());
                    newValueSet.add(value.toUpperCase());
                }
            } else {
                newValueSet.addAll(valueList);
            }
            attrFilterVo.setValueList(new ArrayList<>(newValueSet));
        }
        attrFilterVo.setExpression(expression);
        return attrFilterVo;
    }

    @Override
    public GlobalAttrFilterVo convertGlobalAttrFilter(GlobalAttrVo globalAttrVo, String
            expression, List<String> valueList) {
        GlobalAttrFilterVo globalAttrFilterVo = new GlobalAttrFilterVo();
        globalAttrFilterVo.setAttrId(globalAttrVo.getId());
        if (StringUtils.isBlank(expression)) {
            expression = neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression();
        }
        if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NULL.getExpression())
                || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NOTNULL.getExpression())) {
            globalAttrFilterVo.setExpression(expression);
            return globalAttrFilterVo;
        }
        Set<Long> longValueSet = new HashSet<>();
        List<GlobalAttrItemVo> itemList = globalAttrMapper.getAllGlobalAttrItemByAttrId(globalAttrVo.getId());
        Map<String, GlobalAttrItemVo> globalAttrItemMap = itemList.stream().collect(Collectors.toMap(GlobalAttrItemVo::getValue, e -> e));
        for (String value : valueList) {
            if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression())
                    || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                for (Map.Entry<String, GlobalAttrItemVo> entry : globalAttrItemMap.entrySet()) {
                    String key = entry.getKey();
                    if (Objects.equals(key.toLowerCase(), value.toLowerCase())) {
                        longValueSet.add(entry.getValue().getId());
                    }
                }
                if (CollectionUtils.isEmpty(longValueSet) && value.contains(",")) {
                    String[] split = value.split(",");
                    for (String str : split) {
                        for (Map.Entry<String, GlobalAttrItemVo> entry : globalAttrItemMap.entrySet()) {
                            String key = entry.getKey();
                            if (Objects.equals(key.toLowerCase(), str.toLowerCase())) {
                                longValueSet.add(entry.getValue().getId());
                            }
                        }
                    }
                }
            } else if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                    || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NL.getExpression())) {
                for (Map.Entry<String, GlobalAttrItemVo> entry : globalAttrItemMap.entrySet()) {
                    String key = entry.getKey();
                    if (key.toLowerCase().contains(value.toLowerCase())) {
                        longValueSet.add(entry.getValue().getId());
                    }
                }
                if (CollectionUtils.isEmpty(longValueSet) && value.contains(",")) {
                    String[] split = value.split(",");
                    for (String str : split) {
                        for (Map.Entry<String, GlobalAttrItemVo> entry : globalAttrItemMap.entrySet()) {
                            String key = entry.getKey();
                            if (key.toLowerCase().contains(str.toLowerCase())) {
                                longValueSet.add(entry.getValue().getId());
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(longValueSet)) {
            return null;
        }
        List<Long> longValueList = new ArrayList<>(longValueSet);
        longValueList.sort(Comparator.naturalOrder());
        globalAttrFilterVo.setValueList(longValueList);
        globalAttrFilterVo.setExpression(expression);
        globalAttrFilterVo.setName(globalAttrVo.getName());
        globalAttrFilterVo.setLabel(globalAttrVo.getLabel());
        return globalAttrFilterVo;
    }

    @Override
    public RelFilterVo convertFromRelFilter(RelVo relVo, String expression, List<String> valueList, String
            direction) {
        if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NULL.getExpression())
                || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NOTNULL.getExpression())) {
            RelFilterVo relFilterVo = new RelFilterVo();
            relFilterVo.setRelId(relVo.getId());
            relFilterVo.setExpression(expression);
            relFilterVo.setDirection(direction);
            return relFilterVo;
        }
        Long ciId = null;
        if ("from".equals(direction)) {
            ciId = relVo.getToCiId();
        } else if ("to".equals(direction)) {
            ciId = relVo.getFromCiId();
        } else {
            return null;
        }
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            return null;
        }
        if (StringUtils.isBlank(expression)) {
            expression = neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression();
        }
        Set<Long> ciEntityIdSet = new HashSet<>();
        for (String value : valueList) {
            RelEntityVo relEntityVo = new RelEntityVo();
            relEntityVo.setRelId(relVo.getId());
            relEntityVo.setPageSize(100);
            if ("from".equals(direction)) {
                relEntityVo.setToCiEntityName(value);
                List<RelEntityVo> relEntityList = new ArrayList<>();
                if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                        || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NL.getExpression())) {
                    relEntityList = relEntityMapper.getRelEntityByRelIdAndLikeToCiEntityName(relEntityVo);
                    if (CollectionUtils.isEmpty(relEntityList)) {
                        if (value.contains(",")) {
                            String[] split = value.split(",");
                            for (String str : split) {
                                str = str.trim();
                                if (StringUtils.isNotBlank(str)) {
                                    relEntityVo.setToCiEntityName(str);
                                    List<RelEntityVo> list = relEntityMapper.getRelEntityByRelIdAndLikeToCiEntityName(relEntityVo);
                                    relEntityList.addAll(list);
                                }
                            }
                        }
                    }
                } else if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression())
                        || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                    relEntityList = relEntityMapper.getRelEntityByRelIdAndToCiEntityName(relEntityVo);
                    if (CollectionUtils.isEmpty(relEntityList)) {
                        if (value.contains(",")) {
                            String[] split = value.split(",");
                            for (String str : split) {
                                str = str.trim();
                                if (StringUtils.isNotBlank(str)) {
                                    relEntityVo.setToCiEntityName(str);
                                    List<RelEntityVo> list = relEntityMapper.getRelEntityByRelIdAndToCiEntityName(relEntityVo);
                                    relEntityList.addAll(list);
                                }
                            }
                        }
                    }
                }
                for (RelEntityVo relEntity : relEntityList) {
                    ciEntityIdSet.add(relEntity.getToCiEntityId());
                }
            } else if ("to".equals(direction)) {
                relEntityVo.setFromCiEntityName(value);
                List<RelEntityVo> relEntityList = new ArrayList<>();
                if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())
                        || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NL.getExpression())) {
                    relEntityList = relEntityMapper.getRelEntityByRelIdAndLikeFromCiEntityName(relEntityVo);
                    if (CollectionUtils.isEmpty(relEntityList)) {
                        if (value.contains(",")) {
                            String[] split = value.split(",");
                            for (String str : split) {
                                str = str.trim();
                                if (StringUtils.isNotBlank(str)) {
                                    relEntityVo.setFromCiEntityName(value);
                                    List<RelEntityVo> list = relEntityMapper.getRelEntityByRelIdAndLikeFromCiEntityName(relEntityVo);
                                    relEntityList.addAll(list);
                                }
                            }
                        }
                    }
                } else if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression())
                        || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.NE.getExpression())) {
                    relEntityList = relEntityMapper.getRelEntityByRelIdAndFromCiEntityName(relEntityVo);
                    if (CollectionUtils.isEmpty(relEntityList)) {
                        if (value.contains(",")) {
                            String[] split = value.split(",");
                            for (String str : split) {
                                str = str.trim();
                                if (StringUtils.isNotBlank(str)) {
                                    relEntityVo.setFromCiEntityName(value);
                                    List<RelEntityVo> list = relEntityMapper.getRelEntityByRelIdAndFromCiEntityName(relEntityVo);
                                    relEntityList.addAll(list);
                                }
                            }
                        }
                    }
                }
                for (RelEntityVo relEntity : relEntityList) {
                    ciEntityIdSet.add(relEntity.getFromCiEntityId());
                }
            }
        }
        if (CollectionUtils.isEmpty(ciEntityIdSet)) {
            if (Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.EQ.getExpression()) || Objects.equals(expression, neatlogic.framework.matrix.constvalue.SearchExpression.LI.getExpression())) {
                return null;
            }
        }

        List<Long> ciEntityIdList = new ArrayList<>(ciEntityIdSet);
        ciEntityIdList.sort(Comparator.naturalOrder());
        RelFilterVo relFilterVo = new RelFilterVo();
        relFilterVo.setRelId(relVo.getId());
        relFilterVo.setExpression(expression);
        relFilterVo.setValueList(ciEntityIdList);
        relFilterVo.setDirection(direction);
        return relFilterVo;
    }


    private List<Map<String, Object>> handleIdToName(
            List<Map<String, Object>> list,
            List<AttrVo> attrList) {
        Map<Long, List<Long>> attrId2ValueListMap = new HashMap<>();
        Set<Long> typeIdSet = new HashSet<>();
        Set<Long> ciIdSet = new HashSet<>();
        Set<Long> nonVirtualCiEntityIdSet = new HashSet<>();
        Set<Long> globalAttrItemIdSet = new HashSet<>();
        for (Map<String, Object> map : list) {
            if (MapUtils.isNotEmpty(map)) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        String valueStr = value.toString();
                        if (key.startsWith("const_")) {
                            if (Objects.equals(key, "const_typeName")) {
                                long typeId = Long.parseLong(valueStr);
                                typeIdSet.add(typeId);
                            } else if (Objects.equals(key, "const_ciLabel")) {
                                long ciId = Long.parseLong(valueStr);
                                ciIdSet.add(ciId);
                            }
                        } else if (key.startsWith("attr_")) {
                            long attrId = Long.parseLong(key.substring("attr_".length()));
                            for (AttrVo attrVo : attrList) {
                                if (Objects.equals(attrVo.getId(), attrId)) {
                                    if (Objects.equals(attrVo.isNeedTargetCi(), true)) {
                                        if (valueStr.contains(",")) {
                                            String[] split = valueStr.split(",");
                                            for (String str : split) {
                                                long id = Long.parseLong(str);
                                                attrId2ValueListMap.computeIfAbsent(attrId, k -> new ArrayList<>()).add(id);
                                            }
                                        } else {
                                            long id = Long.parseLong(valueStr);
                                            attrId2ValueListMap.computeIfAbsent(attrId, k -> new ArrayList<>()).add(id);
                                        }
                                    }
                                    break;
                                }
                            }
                        } else if (key.startsWith("relfrom_") || key.startsWith("relto_")) {
                            if (valueStr.contains(",")) {
                                String[] split = valueStr.split(",");
                                for (String str : split) {
                                    long id = Long.parseLong(str);
                                    nonVirtualCiEntityIdSet.add(id);
                                }
                            } else {
                                long id = Long.parseLong(valueStr);
                                nonVirtualCiEntityIdSet.add(id);
                            }
                        } else if (key.startsWith("global_")) {
                            if (valueStr.contains(",")) {
                                String[] split = valueStr.split(",");
                                for (String str : split) {
                                    long id = Long.parseLong(str);
                                    globalAttrItemIdSet.add(id);
                                }
                            } else {
                                long id = Long.parseLong(valueStr);
                                globalAttrItemIdSet.add(id);
                            }
                        }
                    }
                }
            }
        }
        Map<Long, CiVo> ciMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(ciIdSet)) {
            List<CiVo> ciList = ciMapper.getCiByIdList(new ArrayList<>(ciIdSet));
            ciMap = ciList.stream().filter(Objects::nonNull).collect(Collectors.toMap(CiVo::getId, e -> e));
        }
        Map<Long, CiTypeVo> ciTypeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(typeIdSet)) {
            List<CiTypeVo> ciTypeList = ciTypeMapper.getCiTypeListByIdList(new ArrayList<>(typeIdSet));
            ciTypeMap = ciTypeList.stream().filter(Objects::nonNull).collect(Collectors.toMap(CiTypeVo::getId, e -> e));
        }
        Map<Long, List<Long>> virtualCiId2CiEntityIdListMap = new HashMap<>();
        if (MapUtils.isNotEmpty(attrId2ValueListMap)) {
            for (Map.Entry<Long, List<Long>> entry : attrId2ValueListMap.entrySet()) {
                Long key = entry.getKey();
                List<Long> value = entry.getValue();
                for (AttrVo attrVo : attrList) {
                    if (Objects.equals(attrVo.getId(), key)) {
                        if (Objects.equals(attrVo.getTargetIsVirtual(), 1)) {
                            virtualCiId2CiEntityIdListMap.computeIfAbsent(attrVo.getTargetCiId(), k -> new ArrayList<>()).addAll(value);
                        } else {
                            nonVirtualCiEntityIdSet.addAll(value);
                        }
                        break;
                    }
                }
            }
        }
        Map<Long, CiEntityVo> nonVirtualCiEntityMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(nonVirtualCiEntityIdSet)) {
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(new ArrayList<>(nonVirtualCiEntityIdSet));
            nonVirtualCiEntityMap = ciEntityList.stream().filter(Objects::nonNull).collect(Collectors.toMap(CiEntityVo::getId, e -> e));
        }
        Map<Long, List<CiEntityVo>> virtualCiId2CiEntityListMap = new HashMap<>();
        if (MapUtils.isNotEmpty(virtualCiId2CiEntityIdListMap)) {
            for (Map.Entry<Long, List<Long>> entry : virtualCiId2CiEntityIdListMap.entrySet()) {
                Long key = entry.getKey();
                List<Long> value = entry.getValue();
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(key);
                ciEntityVo.setIdList(value);
                List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
                virtualCiId2CiEntityListMap.put(key, ciEntityList);
            }
        }
        Map<Long, GlobalAttrItemVo> globalAttrItemMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(globalAttrItemIdSet)) {
            List<GlobalAttrItemVo> globalAttrItemList = globalAttrMapper.getGlobalAttrItemListByIdList(new ArrayList<>(globalAttrItemIdSet));
            globalAttrItemMap = globalAttrItemList.stream().filter(Objects::nonNull).collect(Collectors.toMap(GlobalAttrItemVo::getId, e -> e));
        }
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            if (MapUtils.isNotEmpty(map)) {
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        String valueStr = value.toString();
                        if (key.startsWith("const_")) {
                            if (Objects.equals(key, "const_id")
                                    || Objects.equals(key, "const_inspectStatus")
                                    || Objects.equals(key, "const_monitorStatus")) {
                                result.put(key, value);
                            } else if (Objects.equals(key, "const_typeName")) {
                                long typeId = Long.parseLong(valueStr);
                                CiTypeVo ciTypeVo = ciTypeMap.get(typeId);
                                if (ciTypeVo != null) {
                                    result.put(key, ciTypeVo.getName());
                                }
                            } else if (Objects.equals(key, "const_ciLabel")) {
                                long ciId = Long.parseLong(valueStr);
                                CiVo ci = ciMap.get(ciId);
                                if (ci != null) {
                                    result.put(key, ci.getLabel());
                                }
                            } else if (Objects.equals(key, "const_inspectTime") || Objects.equals(key, "const_monitorTime")) {
                                if (value instanceof Date date) {
                                    String format = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD_HH_MM_SS));
                                    result.put(key, format);
                                }
                            }
                        } else if (key.startsWith("attr_")) {
                            long attrId = Long.parseLong(key.substring("attr_".length()));
                            for (AttrVo attrVo : attrList) {
                                if (Objects.equals(attrVo.getId(), attrId)) {
                                    if (Objects.equals(attrVo.isNeedTargetCi(), true)) {
                                        List<String> valueNameList = new ArrayList<>();
                                        if (Objects.equals(attrVo.getTargetIsVirtual(), 1)) {
                                            List<CiEntityVo> ciEntityList = virtualCiId2CiEntityListMap.computeIfAbsent(attrVo.getTargetCiId(), k -> new ArrayList<>());
                                            Map<Long, CiEntityVo> ciEntityMap = ciEntityList.stream().filter(Objects::nonNull).collect(Collectors.toMap(CiEntityVo::getId, e -> e));
                                            if (valueStr.contains(",")) {
                                                String[] split = valueStr.split(",");
                                                for (String str : split) {
                                                    long id = Long.parseLong(str);
                                                    CiEntityVo ciEntityVo = ciEntityMap.get(id);
                                                    if (ciEntityVo != null) {
                                                        valueNameList.add(ciEntityVo.getName());
                                                    }
                                                }
                                            } else {
                                                long id = Long.parseLong(valueStr);
                                                CiEntityVo ciEntityVo = ciEntityMap.get(id);
                                                if (ciEntityVo != null) {
                                                    valueNameList.add(ciEntityVo.getName());
                                                }
                                            }
                                        } else {
                                            if (valueStr.contains(",")) {
                                                String[] split = valueStr.split(",");
                                                for (String str : split) {
                                                    long id = Long.parseLong(str);
                                                    CiEntityVo ciEntityVo = nonVirtualCiEntityMap.get(id);
                                                    if (ciEntityVo != null) {
                                                        valueNameList.add(ciEntityVo.getName());
                                                    }
                                                }
                                            } else {
                                                long id = Long.parseLong(valueStr);
                                                CiEntityVo ciEntityVo = nonVirtualCiEntityMap.get(id);
                                                if (ciEntityVo != null) {
                                                    valueNameList.add(ciEntityVo.getName());
                                                }
                                            }
                                        }
                                        result.put(key, String.join(",", valueNameList));
                                    } else {
                                        result.put(key, value);
                                    }
                                    break;
                                }
                            }
                        } else if (key.startsWith("relfrom_") || key.startsWith("relto_")) {
                            List<String> valueNameList = new ArrayList<>();
                            if (valueStr.contains(",")) {
                                String[] split = valueStr.split(",");
                                for (String str : split) {
                                    long id = Long.parseLong(str);
                                    CiEntityVo ciEntityVo = nonVirtualCiEntityMap.get(id);
                                    if (ciEntityVo != null) {
                                        valueNameList.add(ciEntityVo.getName());
                                    }
                                }
                            } else {
                                long id = Long.parseLong(valueStr);
                                CiEntityVo ciEntityVo = nonVirtualCiEntityMap.get(id);
                                if (ciEntityVo != null) {
                                    valueNameList.add(ciEntityVo.getName());
                                }
                            }
                            result.put(key, String.join(",", valueNameList));
                        } else if (key.startsWith("global_")) {
                            List<String> valueNameList = new ArrayList<>();
                            if (valueStr.contains(",")) {
                                String[] split = valueStr.split(",");
                                for (String str : split) {
                                    long id = Long.parseLong(str);
                                    GlobalAttrItemVo globalAttrItemVo = globalAttrItemMap.get(id);
                                    if (globalAttrItemVo != null) {
                                        valueNameList.add(globalAttrItemVo.getValue());
                                    }
                                }
                            } else {
                                long id = Long.parseLong(valueStr);
                                GlobalAttrItemVo globalAttrItemVo = globalAttrItemMap.get(id);
                                if (globalAttrItemVo != null) {
                                    valueNameList.add(globalAttrItemVo.getValue());
                                }
                            }
                            result.put(key, String.join(",", valueNameList));
                        }
                    }
                }
                resultList.add(result);
            }
        }
        return resultList;
    }

}
