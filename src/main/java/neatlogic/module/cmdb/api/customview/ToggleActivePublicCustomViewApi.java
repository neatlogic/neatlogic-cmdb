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

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.cmdb.exception.customview.CustomViewIsPrivateException;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CUSTOMVIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ToggleActivePublicCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getName() {
        return "激活/禁用公共自定义视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
            @Param(name = "isActive",
                    type = ApiParamType.INTEGER,
                    desc = "是否激活", isRequired = true)
    })
    @Description(desc = "激活/禁用公共自定义视图接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        CustomViewVo customViewVo = customViewService.getCustomViewById(id);
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(id);
        }
        if (!customViewVo.getType().equals(CustomViewType.PUBLIC.getValue())) {
            throw new CustomViewIsPrivateException(customViewVo);
        }
        customViewVo.setIsActive(paramObj.getInteger("isActive"));
        customViewService.updateCustomViewActive(customViewVo);
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/public/toggleactive";
    }
}
