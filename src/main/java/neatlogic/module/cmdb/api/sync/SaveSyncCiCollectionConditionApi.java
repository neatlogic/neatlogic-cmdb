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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncConditionVo;
import neatlogic.framework.cmdb.enums.sync.ExpressionType;
import neatlogic.framework.cmdb.enums.sync.FieldType;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 保存独立于本次执行和定时策略的同步过滤预设。 */
@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveSyncCiCollectionConditionApi extends PrivateApiComponentBase {
    @Resource
    private SyncMapper syncMapper;

    /** 注册预设保存接口。 */
    @Override
    public String getToken() {
        return "/cmdb/sync/cicollection/condition/save";
    }

    /** 返回接口名称。 */
    @Override
    public String getName() {
        return "nmcas.savesynccicollectionconditionapi.getname";
    }

    /** 本接口无附加配置。 */
    @Override
    public String getConfig() {
        return null;
    }

    /** 校验嵌套条件后只保存预设，不触发同步也不重置同步进度。 */
    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.syncmappingid"),
            @Param(name = "conditionList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "筛选条件，空数组清空预设")})
    @Description(desc = "nmcas.savesynccicollectionconditionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        JSONArray conditions = jsonObj.getJSONArray("conditionList");
        // 框架允许必填数组显式传 null，此处必须区分 null 与主动清空的空数组。
        if (conditions == null) {
            throw new ParamNotExistsException("conditionList");
        }
        SyncCiCollectionVo collection = syncMapper.getSyncCiCollectionById(id);
        if (collection == null) {
            throw new SyncCiCollectionNotFoundException(id);
        }
        List<SyncConditionVo> conditionList = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            String paramName = "conditionList[" + i + "]";
            if (!(conditions.get(i) instanceof JSONObject)) {
                throw new ParamIrregularException(paramName);
            }
            JSONObject condition = conditions.getJSONObject(i);
            for (String key : Arrays.asList("field", "type", "expression", "value")) {
                if (!(condition.get(key) instanceof String) || StringUtils.isBlank(condition.getString(key))) {
                    throw new ParamIrregularException(paramName + "." + key);
                }
            }
            SyncConditionVo vo = condition.toJavaObject(SyncConditionVo.class);
            validateCondition(vo, paramName);
            conditionList.add(vo);
        }
        // 复用执行查询构造验证正则等值约束，防止不可执行的条件成为共享预设。
        SyncCiCollectionVo candidate = new SyncCiCollectionVo();
        candidate.setConditionList(conditionList);
        try {
            candidate.getCriteria().getCriteriaObject();
        } catch (RuntimeException ex) {
            throw new ParamIrregularException("conditionList");
        }
        collection.setDefaultConditionList(conditionList);
        collection.setLcu(UserContext.get().getUserUuid());
        syncMapper.updateSyncCiCollectionCondition(collection);
        return null;
    }

    /** 嵌套字段不会由顶层注解校验，沿用编辑器允许的数据类型和表达式组合。 */
    private void validateCondition(SyncConditionVo condition, String paramName) {
        boolean validType = Arrays.stream(FieldType.values()).anyMatch(type -> type.getValue().equalsIgnoreCase(condition.getType()));
        if (!validType) {
            throw new ParamIrregularException(paramName + ".type");
        }
        boolean validExpression = ExpressionType.getExpressionBySupportType(condition.getType()).stream()
                .anyMatch(expression -> expression.getValue().equalsIgnoreCase(condition.getExpression()));
        if (!validExpression) {
            throw new ParamIrregularException(paramName + ".expression");
        }
        Object value = condition.getFormatValue();
        if (value == null || value instanceof Float && !Float.isFinite((Float) value)) {
            throw new ParamIrregularException(paramName + ".value");
        }
    }
}
