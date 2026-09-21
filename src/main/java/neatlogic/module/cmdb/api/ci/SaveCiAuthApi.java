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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.ci.CiAuthInvalidException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiAuthApi extends PrivateApiComponentBase {

    private static final int BATCH_SIZE = 500;
    private static final Set<String> ACTION_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            CiAuthType.CIMANAGE.getValue(),
            CiAuthType.CIENTITYINSERT.getValue(),
            CiAuthType.CIENTITYUPDATE.getValue(),
            CiAuthType.CIENTITYDELETE.getValue(),
            CiAuthType.CIENTITYRECOVER.getValue(),
            CiAuthType.CIENTITYQUERY.getValue(),
            CiAuthType.TRANSACTIONMANAGE.getValue(),
            CiAuthType.PASSWORDVIEW.getValue()
    )));
    private static final Set<String> VIRTUAL_ACTION_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            CiAuthType.CIMANAGE.getValue(),
            CiAuthType.CIENTITYQUERY.getValue()
    )));
    private static final Set<String> AUTH_TYPE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "common", "user", "team", "role"
    )));

    @Resource
    private CiAuthMapper ciAuthMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/auth/save";
    }

    @Override
    public String getName() {
        return "nmcac.saveciauthapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "appendCiIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.ciidlist"),
            @Param(name = "replaceCiIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.ciidlist"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY,
                    desc = "nmcac.saveciauthapi.input.param.desc.authlist")})
    @Description(desc = "nmcac.saveciauthapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        LinkedHashSet<Long> appendCiIdSet = parseCiIdSet(jsonObj.getJSONArray("appendCiIdList"));
        LinkedHashSet<Long> replaceCiIdSet = parseCiIdSet(jsonObj.getJSONArray("replaceCiIdList"));
        if (ciId != null) {
            if (ciId <= 0L || jsonObj.containsKey("appendCiIdList") || jsonObj.containsKey("replaceCiIdList")) {
                throw new CiAuthInvalidException();
            }
            replaceCiIdSet.add(ciId);
        } else if (CollectionUtils.isEmpty(appendCiIdSet) && CollectionUtils.isEmpty(replaceCiIdSet)) {
            throw new CiAuthInvalidException();
        }

        Set<Long> intersectionSet = new HashSet<>(appendCiIdSet);
        intersectionSet.retainAll(replaceCiIdSet);
        if (CollectionUtils.isNotEmpty(intersectionSet)) {
            throw new CiAuthInvalidException();
        }

        List<CiAuthVo> normalizedAuthList = normalizeAuthList(jsonObj.getJSONArray("authList"));
        List<Long> targetCiIdList = new ArrayList<>(appendCiIdSet);
        targetCiIdList.addAll(replaceCiIdSet);
        Map<Long, CiVo> ciMap = getAndValidateCiMap(targetCiIdList);
        validateManagePrivilege(targetCiIdList, ciMap);

        // 所有目标均通过校验后才开始写入，保证批量请求不会产生部分修改。
        deleteCiAuthByCiIdList(new ArrayList<>(replaceCiIdSet));
        List<CiAuthVo> insertAuthList = buildInsertAuthList(targetCiIdList, ciMap, normalizedAuthList);
        for (int fromIndex = 0; fromIndex < insertAuthList.size(); fromIndex += BATCH_SIZE) {
            int toIndex = Math.min(fromIndex + BATCH_SIZE, insertAuthList.size());
            ciAuthMapper.insertCiAuthList(insertAuthList.subList(fromIndex, toIndex));
        }
        return null;
    }

    /** 解析模型ID列表，列表内重复ID自动合并。 */
    private LinkedHashSet<Long> parseCiIdSet(JSONArray ciIdArray) {
        LinkedHashSet<Long> ciIdSet = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(ciIdArray)) {
            return ciIdSet;
        }
        for (Object value : ciIdArray) {
            if (!(value instanceof Number)) {
                throw new CiAuthInvalidException();
            }
            Long ciId;
            try {
                ciId = Long.valueOf(value.toString());
            } catch (NumberFormatException ex) {
                throw new CiAuthInvalidException();
            }
            if (ciId <= 0L) {
                throw new CiAuthInvalidException();
            }
            ciIdSet.add(ciId);
        }
        return ciIdSet;
    }

    /** 校验并去重前端提交的授权项，避免把数据库不支持的动作写入授权表。 */
    private List<CiAuthVo> normalizeAuthList(JSONArray authArray) {
        Map<String, CiAuthVo> authMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(authArray)) {
            return new ArrayList<>();
        }
        for (int i = 0; i < authArray.size(); i++) {
            Object authValue = authArray.get(i);
            if (!(authValue instanceof JSONObject)) {
                throw new CiAuthInvalidException();
            }
            JSONObject authObj = (JSONObject) authValue;
            CiAuthVo ciAuthVo = JSON.toJavaObject(authObj, CiAuthVo.class);
            String action = StringUtils.trim(ciAuthVo.getAction());
            String authType = StringUtils.trim(ciAuthVo.getAuthType());
            String authUuid = StringUtils.trim(ciAuthVo.getAuthUuid());
            if (StringUtils.isBlank(action) || StringUtils.isBlank(authType) || StringUtils.isBlank(authUuid)
                    || !ACTION_SET.contains(action) || !AUTH_TYPE_SET.contains(authType)) {
                throw new CiAuthInvalidException();
            }
            ciAuthVo.setAction(action);
            ciAuthVo.setAuthType(authType);
            ciAuthVo.setAuthUuid(authUuid);
            authMap.put(authType + "\u0000" + action + "\u0000" + authUuid, ciAuthVo);
        }
        return new ArrayList<>(authMap.values());
    }

    /** 分批读取模型并确认所有目标都存在。 */
    private Map<Long, CiVo> getAndValidateCiMap(List<Long> ciIdList) {
        Map<Long, CiVo> ciMap = new HashMap<>();
        for (List<Long> batchCiIdList : partition(ciIdList)) {
            List<CiVo> ciList = ciMapper.getCiByIdList(batchCiIdList);
            if (CollectionUtils.isNotEmpty(ciList)) {
                for (CiVo ciVo : ciList) {
                    ciMap.put(ciVo.getId(), ciVo);
                }
            }
        }
        for (Long ciId : ciIdList) {
            if (!ciMap.containsKey(ciId)) {
                throw new CiNotFoundException(ciId);
            }
        }
        return ciMap;
    }

    /** 批量检查模型管理权限，任一模型无权限时拒绝整个请求。 */
    private void validateManagePrivilege(List<Long> ciIdList, Map<Long, CiVo> ciMap) {
        if (hasGlobalCiManagePrivilege()) {
            return;
        }
        Map<Long, List<CiAuthVo>> ciAuthMap = new HashMap<>();
        for (List<Long> batchCiIdList : partition(ciIdList)) {
            List<CiAuthVo> ciAuthList = ciAuthMapper.getCiAuthByCiIdList(batchCiIdList);
            if (CollectionUtils.isNotEmpty(ciAuthList)) {
                for (CiAuthVo ciAuthVo : ciAuthList) {
                    ciAuthMap.computeIfAbsent(ciAuthVo.getCiId(), key -> new ArrayList<>()).add(ciAuthVo);
                }
            }
        }
        for (Long ciId : ciIdList) {
            List<CiAuthVo> ciAuthList = ciAuthMap.get(ciId);
            if (ciAuthList == null) {
                ciAuthList = Collections.emptyList();
            }
            if (!hasCiManagePrivilege(ciAuthList)) {
                throw new CiAuthException(ciMap.get(ciId).getLabel());
            }
        }
    }

    /** 独立封装全局权限判断，便于无 Spring 环境的单元测试覆盖批量保存逻辑。 */
    protected boolean hasGlobalCiManagePrivilege() {
        return AuthActionChecker.check(CI_MODIFY.class);
    }

    /** 判断当前用户是否拥有模型级管理权限。 */
    protected boolean hasCiManagePrivilege(List<CiAuthVo> ciAuthList) {
        return CiAuthChecker.hasPrivilege(ciAuthList, CiAuthType.CIMANAGE);
    }

    /** 按模型类型生成待写入数据，虚拟模型只接受管理和查询权限。 */
    private List<CiAuthVo> buildInsertAuthList(List<Long> ciIdList, Map<Long, CiVo> ciMap, List<CiAuthVo> authList) {
        List<CiAuthVo> resultList = new ArrayList<>();
        for (Long ciId : ciIdList) {
            CiVo ciVo = ciMap.get(ciId);
            boolean isVirtual = ciVo.getIsVirtual() != null && ciVo.getIsVirtual().equals(1);
            for (CiAuthVo sourceAuthVo : authList) {
                if (!isVirtual || VIRTUAL_ACTION_SET.contains(sourceAuthVo.getAction())) {
                    CiAuthVo targetAuthVo = new CiAuthVo();
                    targetAuthVo.setCiId(ciId);
                    targetAuthVo.setAction(sourceAuthVo.getAction());
                    targetAuthVo.setAuthType(sourceAuthVo.getAuthType());
                    targetAuthVo.setAuthUuid(sourceAuthVo.getAuthUuid());
                    resultList.add(targetAuthVo);
                }
            }
        }
        return resultList;
    }

    /** 分批删除覆盖策略目标的原授权。 */
    private void deleteCiAuthByCiIdList(List<Long> ciIdList) {
        for (List<Long> batchCiIdList : partition(ciIdList)) {
            ciAuthMapper.deleteCiAuthByCiIdList(batchCiIdList);
        }
    }

    /** 将集合按固定大小分段，避免生成过长的 IN 条件或 INSERT 语句。 */
    private <T> List<List<T>> partition(Collection<T> collection) {
        List<T> valueList = new ArrayList<>(collection);
        List<List<T>> resultList = new ArrayList<>();
        for (int fromIndex = 0; fromIndex < valueList.size(); fromIndex += BATCH_SIZE) {
            int toIndex = Math.min(fromIndex + BATCH_SIZE, valueList.size());
            resultList.add(valueList.subList(fromIndex, toIndex));
        }
        return resultList;
    }

}
