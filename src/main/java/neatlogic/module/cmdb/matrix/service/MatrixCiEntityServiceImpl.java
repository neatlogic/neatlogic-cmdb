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
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.matrix.dao.mapper.MatrixCiEntityMapper;
import neatlogic.module.cmdb.matrix.dto.MatrixCiEntitySearchVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatrixCiEntityServiceImpl implements MatrixCiEntityService {

    private static final Logger logger = LoggerFactory.getLogger(MatrixCiEntityServiceImpl.class);

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

    @Override
    public List<Map<String, Object>> searchCiEntityList(MatrixCiEntitySearchVo matrixCiEntitySearchVo) {
        long time = 0L;
        if (logger.isInfoEnabled()) {
            time = System.currentTimeMillis();
        }

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
            if (logger.isInfoEnabled()) {
                logger.info("查询配置项行数，行数{}，耗时{}ms", rowNum, System.currentTimeMillis() - time);
            }
            matrixCiEntitySearchVo.setRowNum(rowNum);
        }
        List<Map<String, Object>> list = matrixCiEntityMapper.searchCiEntityList(matrixCiEntitySearchVo);
        if (CollectionUtils.isNotEmpty(list)) {
            return handleIdToName(list, attrList);
        }

        return new ArrayList<>();
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
                            if (Objects.equals(key, "const_id")) {

                            } else if (Objects.equals(key, "const_typeName")) {
                                long typeId = Long.parseLong(valueStr);
                                typeIdSet.add(typeId);
                            } else if (Objects.equals(key, "const_ciLabel")) {
                                long ciId = Long.parseLong(valueStr);
                                ciIdSet.add(ciId);
                            } else if (Objects.equals(key, "const_inspectTime")) {

                            } else if (Objects.equals(key, "const_inspectStatus")) {

                            } else if (Objects.equals(key, "const_monitorTime")) {

                            } else if (Objects.equals(key, "const_monitorStatus")) {

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
