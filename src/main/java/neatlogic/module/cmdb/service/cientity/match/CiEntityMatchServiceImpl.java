/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the Sustainable Use License (SUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.service.cientity.match;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.match.CiEntityMatchCandidateVo;
import neatlogic.framework.cmdb.dto.cientity.match.CiEntityMatchConditionVo;
import neatlogic.framework.cmdb.dto.cientity.match.CiEntityMatchMetadataVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.cientity.CiEntityMatchReadPrivilegeException;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMatchMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/** 严格校验后执行轻量候选查询，失效条件绝不自动移除。 */
@Service
public class CiEntityMatchServiceImpl implements CiEntityMatchService {
    private static final int MAX_CONDITIONS = 16;
    private static final int MAX_VALUES = 128;
    @Resource
    private CiMapper ciMapper;
    @Resource
    private AttrMapper attrMapper;
    @Resource
    private CiEntityMatchMapper ciEntityMatchMapper;

    /** 关联查询覆盖整个模型，不能仅凭部分配置项组的可见权限授权全量搜索。 */
    @Override
    public void requireReadPrivilege(Long targetCiId) {
        if (!AuthActionChecker.check(CMDB_BASE.class)) {
            throw new CiEntityMatchReadPrivilegeException("没有读取CMDB模型的权限");
        }
        requireModel(targetCiId);
        if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(targetCiId).check()) {
            throw new CiEntityMatchReadPrivilegeException("没有读取该模型全部配置项的权限");
        }
    }

    /** 配置保存和实际执行共用元数据校验，允许直接文本、数值及继承属性。 */
    @Override
    public CiEntityMatchMetadataVo validate(Long targetCiId, List<Long> attrIdList) {
        CiVo ci = requireModel(targetCiId);
        return metadata(ci, attrIdList);
    }

    /** 不调用完整配置项搜索，避免条件被移除、额外计数和属性关系装配。 */
    @Override
    public List<CiEntityMatchCandidateVo> match(Long targetCiId, List<CiEntityMatchConditionVo> conditions) {
        if (conditions == null || conditions.isEmpty() || conditions.size() > MAX_CONDITIONS) {
            throw new ApiRuntimeException("配置项匹配必须提供1至16个属性条件");
        }
        List<Long> ids = new ArrayList<>();
        for (CiEntityMatchConditionVo condition : conditions) {
            if (condition == null) {
                throw new ApiRuntimeException("配置项匹配属性条件不能为空");
            }
            ids.add(condition.getAttrId());
        }
        CiVo ci = requireModel(targetCiId);
        CiEntityMatchMetadataVo metadata = metadata(ci, ids);
        Map<Long, AttrVo> attrs = new HashMap<>();
        for (AttrVo attr : metadata.getAttrList()) {
            attrs.put(attr.getId(), attr);
        }
        List<AttrFilterVo> filters = new ArrayList<>();
        for (CiEntityMatchConditionVo condition : conditions) {
            AttrVo attr = attrs.get(condition.getAttrId());
            // 调用方保存的类型快照失效时禁止按新的类型继续匹配。
            if (condition.getExpectedType() != null && !Objects.equals(condition.getExpectedType(), attr.getType())) {
                throw new ApiRuntimeException("匹配属性类型已改变，请重新保存映射：" + attr.getId());
            }
            AttrFilterVo filter = new AttrFilterVo();
            filter.setAttrId(attr.getId());
            filter.setCiId(attr.getCiId());
            filter.setType(attr.getType());
            filter.setNeedTargetCi(false);
            filter.setIsInvokeAttr(false);
            if (Objects.equals(attr.getType(), "number")) {
                filter.setExpression(SearchExpression.BT.getExpression());
            } else {
                filter.setExpression(SearchExpression.EQ.getExpression());
            }
            filter.setValueList(normalizeValues(condition.getValueList(), attr.getType()));
            filters.add(filter);
        }
        return ciEntityMatchMapper.match(ci, filters);
    }

    /** 只接收真实、已存在的物理模型；抽象模型可作为后代实体的查询范围。 */
    private CiVo requireModel(Long id) {
        if (id == null || id <= 0) {
            throw new ApiRuntimeException("配置项匹配目标模型无效");
        }
        CiVo ci = ciMapper.getCiById(id);
        if (ci == null || Objects.equals(ci.getIsVirtual(), 1)) {
            throw new ApiRuntimeException("配置项匹配目标模型不存在或为虚拟模型");
        }
        return ci;
    }

    /** 对照最新属性列表及祖先模型检查每个属性，禁止跨模型或损坏属性扩大查询范围。 */
    private CiEntityMatchMetadataVo metadata(CiVo ci, List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_CONDITIONS) {
            throw new ApiRuntimeException("配置项匹配必须提供1至16个属性条件");
        }
        List<CiVo> ancestry = ciMapper.getUpwardCiListByLR(ci.getLft(), ci.getRht());
        Set<Long> modelIds = new HashSet<>();
        modelIds.add(ci.getId());
        if (ancestry != null) {
            for (CiVo ancestor : ancestry) {
                modelIds.add(ancestor.getId());
            }
        }
        Map<Long, AttrVo> attrs = new HashMap<>();
        List<AttrVo> available = attrMapper.getAttrByCiId(ci.getId());
        if (available != null) {
            for (AttrVo attr : available) {
                attrs.put(attr.getId(), attr);
            }
        }
        Set<Long> distinct = new HashSet<>();
        List<AttrVo> selected = new ArrayList<>();
        for (Long id : ids) {
            AttrVo attr = attrs.get(id);
            if (id == null || id <= 0 || !distinct.add(id) || attr == null
                    || !modelIds.contains(attr.getCiId()) || attr.getTargetCiId() != null
                    || !(Objects.equals(attr.getType(), "text") || Objects.equals(attr.getType(), "number"))) {
                throw new ApiRuntimeException("匹配属性不存在、重复、不属于目标模型或不支持直接比较：" + id);
            }
            selected.add(attr);
        }
        CiEntityMatchMetadataVo result = new CiEntityMatchMetadataVo();
        result.setCiId(ci.getId());
        result.setCiName(ci.getName());
        result.setCiLabel(ci.getLabel());
        result.setIsAbstract(ci.getIsAbstract());
        result.setAttrList(selected);
        return result;
    }

    /** 空值和非法数值必须报错；多值去重后保持任一相等语义，限制单次查询规模。 */
    private JSONArray normalizeValues(JSONArray raw, String type) {
        if (raw == null || raw.isEmpty() || raw.size() > MAX_VALUES) {
            throw new ApiRuntimeException("匹配来源值必须为1至128个非空标量");
        }
        Set<Object> values = new LinkedHashSet<>();
        for (Object value : raw) {
            if (!(value instanceof String) && !(value instanceof Number)) {
                throw new ApiRuntimeException("匹配来源值只能为文本或数字");
            }
            String text = value.toString();
            if (text.trim().isEmpty() || text.length() > 4096) {
                throw new ApiRuntimeException("匹配来源值为空或超出长度限制");
            }
            if (Objects.equals(type, "number")) {
                try {
                    BigDecimal number = new BigDecimal(text.trim()).stripTrailingZeros();
                    if (number.precision() > 65 || number.scale() > 30 || number.scale() < -65) {
                        throw new NumberFormatException();
                    }
                    values.add(number);
                } catch (NumberFormatException ex) {
                    throw new ApiRuntimeException("匹配来源值无法转换为有效数字");
                }
            } else {
                values.add(text);
            }
        }
        JSONArray result = new JSONArray();
        result.addAll(values);
        return result;
    }
}
