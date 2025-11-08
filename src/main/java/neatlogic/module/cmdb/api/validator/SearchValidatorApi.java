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
import neatlogic.framework.cmdb.dao.mapper.validator.ValidatorMapper;
import neatlogic.framework.cmdb.dto.validator.ValidatorVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchValidatorApi extends PrivateApiComponentBase {

    @Autowired
    private ValidatorMapper validatorMapper;

    @Override
    public String getToken() {
        return "/cmdb/validator/search";
    }

    @Override
    public String getName() {
        return "查询校验规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数量"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = ValidatorVo[].class)})
    @Description(desc = "查询校验规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        ValidatorVo validatorVo = JSONObject.toJavaObject(jsonObj, ValidatorVo.class);
        List<ValidatorVo> validatorList = validatorMapper.searchValidator(validatorVo);
        returnObj.put("tbodyList", validatorList);
        if (validatorList.size() > 0 && validatorVo.getNeedPage()) {
            returnObj.put("pageSize", validatorVo.getPageSize());
            returnObj.put("pageCount", validatorVo.getPageCount());
            returnObj.put("rowNum", validatorVo.getRowNum());
            returnObj.put("currentPage", validatorVo.getCurrentPage());
        }
        return returnObj;
    }

}
