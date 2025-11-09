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

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.resourcecenter.condition.IResourcecenterCondition;
import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.condition.ConditionGroupVo;
import neatlogic.framework.dto.condition.ConditionVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetResourceCustomConditionValueConvertTextApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取资产高级过滤条件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "conditionGroupList", type = ApiParamType.JSONARRAY, desc = "条件组"),
            @Param(name = "conditionGroupRelList", type = ApiParamType.JSONARRAY, desc = "条件组之间的关系"),
    })
    @Output({
            @Param(name = "conditionGroupList", type = ApiParamType.JSONARRAY, desc = "条件组"),
            @Param(name = "conditionGroupRelList", type = ApiParamType.JSONARRAY, desc = "条件组之间的关系")
    })
    @Description(desc = "获取资产高级过滤条件,根据值补充中文")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        ResourceSearchVo resourceSearch = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        List<ConditionGroupVo> conditionGroupList = resourceSearch.getConditionGroupList();
        if (CollectionUtils.isNotEmpty(conditionGroupList)) {
            for (ConditionGroupVo conditionGroup : conditionGroupList) {
                List<ConditionVo> conditionList = conditionGroup.getConditionList();
                if (CollectionUtils.isNotEmpty(conditionList)) {
                    for (ConditionVo condition : conditionList) {
                        IResourcecenterCondition conditionHandler = ResourcecenterConditionFactory.getHandler(condition.getName());
                        if (conditionHandler != null) {
                            Object textList = conditionHandler.valueConversionText(condition.getValueList(), null);
                            condition.setText(textList);
                            condition.setLabel(conditionHandler.getDisplayName());
                        }
                    }
                }
            }
        }
        result.put("conditionGroupList", conditionGroupList);
        result.put("conditionGroupRelList", resourceSearch.getConditionGroupRelList());
        return result;
    }

    @Override
    public String getToken() {
        return "resourcecenter/custom/condition/valueconverttext/get";
    }
}
