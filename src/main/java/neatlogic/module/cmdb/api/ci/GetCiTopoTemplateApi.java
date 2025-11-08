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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.utils.RelPathBuilder;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiTopoTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/topotemplate/get";
    }

    @Override
    public String getName() {
        return "nmcac.getcitopotemplateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true),
            @Param(name = "needRelPath", desc = "term.cmdb.needcirel", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = CiTopoTemplateVo.class)})
    @Description(desc = "nmcac.getcitopotemplateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTopoTemplateVo ciTopoTemplateVo = ciTopoTemplateMapper.getCiTopoTemplateById(jsonObj.getLong("id"));
        Integer needRelPath = jsonObj.getInteger("needRelPath");
        if (needRelPath != null && needRelPath.equals(1)) {
            //以下逻辑是为了可以正确回显已经选择的关系路径
            JSONArray ciRelList = ciTopoTemplateVo.getConfig().getJSONArray("ciRelList");
            Long ciId = ciTopoTemplateVo.getCiId();
            if (ciId != null) {
                JSONObject root = new JSONObject();
                RelPathBuilder.build(root, 0, ciRelList, ciId);
                if (MapUtils.isNotEmpty(root)) {
                    ciTopoTemplateVo.setRelPath(root.getJSONArray("children"));
                }
            }
        }
        return ciTopoTemplateVo;
    }
}
