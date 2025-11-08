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

package neatlogic.module.cmdb.api.validator;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.validator.ValidatorVo;
import neatlogic.framework.cmdb.validator.core.IValidator;
import neatlogic.framework.cmdb.validator.core.ValidatorFactory;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListValidatorHandlerApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "/cmdb/validator/handler/list";
    }

    @Override
    public String getName() {
        return "获取校验组件列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = ValidatorVo.class)})
    @Description(desc = "获取校验组件列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<IValidator> handlerList = ValidatorFactory.getValidatorHandlerList();
        List<ValueTextVo> returnList = new ArrayList<>();
        for (IValidator validator : handlerList) {
            returnList.add(new ValueTextVo(validator.getClassName(), validator.getName()));
        }
        return returnList;
    }

}
