/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x - 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.service.cientity;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.crossover.IAttrInvokeCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrInvokeVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.utils.CiEntityBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttrInvokeManager implements IAttrInvokeCrossoverService {

    @Resource
    private CiEntityMapper ciEntityMapper;

    public void replaceAttrInvoke(AttrVo attrVo, Long ciEntityId, JSONArray valueList) {
        ciEntityMapper.deleteAttrInvokeByCiEntityIdAndAttrId(ciEntityId, attrVo.getId());
        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        List<AttrInvokeVo> attrInvokeList = handler.convertValueListToAttrInvokeList(attrVo, ciEntityId, valueList);
        if (CollectionUtils.isNotEmpty(attrInvokeList)) {
            ciEntityMapper.insertAttrInvokeList(attrInvokeList);
        }
    }

    public JSONArray getValueList(Long ciEntityId, AttrVo attrVo) {
        List<AttrInvokeVo> attrInvokeList = ciEntityMapper.getAttrInvokeListByCiEntityIdAndAttrId(ciEntityId, attrVo.getId());
        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
        JSONArray valueList = handler.convertAttrInvokeListToValueList(attrVo, attrInvokeList);
        return valueList == null ? new JSONArray() : valueList;
    }

    public void hydrateCiEntity(CiEntityVo ciEntityVo, List<AttrVo> attrList) {
        if (ciEntityVo != null) {
            hydrateCiEntityList(Collections.singletonList(ciEntityVo), attrList);
        }
    }

    public void hydrateCiEntityList(List<CiEntityVo> ciEntityList, List<AttrVo> attrList) {
        if (CollectionUtils.isEmpty(ciEntityList) || CollectionUtils.isEmpty(attrList)) {
            return;
        }
        // 仅批量还原使用cmdb_attr_invoke表存储的属性。
        List<AttrVo> invokeAttrList = attrList.stream()
                .filter(AttrVo::isInvokeAttr)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(invokeAttrList)) {
            return;
        }
        List<Long> ciEntityIdList = ciEntityList.stream().map(CiEntityVo::getId).filter(id -> id != null).distinct().collect(Collectors.toList());
        List<Long> attrIdList = invokeAttrList.stream().map(AttrVo::getId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ciEntityIdList) || CollectionUtils.isEmpty(attrIdList)) {
            return;
        }

        List<AttrInvokeVo> attrInvokeList = ciEntityMapper.getAttrInvokeListByCiEntityIdListAndAttrIdList(ciEntityIdList, attrIdList);
        Map<Long, Map<Long, List<AttrInvokeVo>>> invokeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(attrInvokeList)) {
            for (AttrInvokeVo attrInvokeVo : attrInvokeList) {
                invokeMap.computeIfAbsent(attrInvokeVo.getCiEntityId(), key -> new HashMap<>())
                        .computeIfAbsent(attrInvokeVo.getAttrId(), key -> new ArrayList<>())
                        .add(attrInvokeVo);
            }
        }

        for (CiEntityVo ciEntityVo : ciEntityList) {
            Map<Long, List<AttrInvokeVo>> entityInvokeMap = invokeMap.getOrDefault(ciEntityVo.getId(), Collections.emptyMap());
            for (AttrVo attrVo : invokeAttrList) {
                IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
                JSONArray valueList = handler.convertAttrInvokeListToValueList(attrVo, entityInvokeMap.getOrDefault(attrVo.getId(), Collections.emptyList()));
                if (valueList == null) {
                    valueList = new JSONArray();
                }
                JSONArray actualValueList = handler.getActualValueList(attrVo, valueList);
                ciEntityVo.addAttrEntityData(attrVo.getId(), CiEntityBuilder.buildAttrObj(ciEntityVo.getId(), attrVo, valueList, actualValueList));
            }
        }
    }

    public void deleteByCiEntityId(Long ciEntityId) {
        ciEntityMapper.deleteAttrInvokeByCiEntityId(ciEntityId);
    }

    public void deleteByAttrId(Long attrId) {
        ciEntityMapper.deleteAttrInvokeByAttrId(attrId);
    }

    @Override
    public List<AttrInvokeVo> getAttrInvokeListByAttrId(Long attrId) {
        return ciEntityMapper.getAttrInvokeListByAttrId(attrId);
    }

    @Override
    public List<AttrInvokeVo> getAttrInvokeListByAttrTypeAndTypeAndInvokeIdList(String attrType, String type, List<Long> invokeIdList) {
        if (CollectionUtils.isEmpty(invokeIdList)) {
            return Collections.emptyList();
        }
        return ciEntityMapper.getAttrInvokeListByAttrTypeAndTypeAndInvokeIdList(attrType, type, invokeIdList);
    }

    @Override
    public List<AttrInvokeVo> getAttrInvokeListByCiEntityIdListAndAttrIdList(List<Long> ciEntityIdList, List<Long> attrIdList) {
        if (CollectionUtils.isEmpty(ciEntityIdList) || CollectionUtils.isEmpty(attrIdList)) {
            return Collections.emptyList();
        }
        return ciEntityMapper.getAttrInvokeListByCiEntityIdListAndAttrIdList(ciEntityIdList, attrIdList);
    }

    @Override
    public void updateAttrInvokeList(List<AttrInvokeVo> attrInvokeList) {
        if (CollectionUtils.isNotEmpty(attrInvokeList)) {
            ciEntityMapper.updateAttrInvokeList(attrInvokeList);
        }
    }
}
