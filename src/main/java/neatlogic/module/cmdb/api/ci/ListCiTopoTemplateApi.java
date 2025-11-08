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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiTopoTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/topotemplate/list";
    }

    @Override
    public String getName() {
        return "nmcac.listcitopotemplateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive")
    })
    @Output({@Param(explode = CiTopoTemplateVo[].class)})
    @Description(desc = "nmcac.listcitopotemplateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTopoTemplateVo ciTopoTemplateVo = JSON.toJavaObject(jsonObj,CiTopoTemplateVo.class);
        return ciTopoTemplateMapper.getCiTopoTemplateByCiId(ciTopoTemplateVo);
    }
}
