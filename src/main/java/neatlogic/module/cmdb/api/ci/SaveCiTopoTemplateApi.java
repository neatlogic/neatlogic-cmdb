/*
 * Copyright(c) 2024 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class SaveCiTopoTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/topotemplate/save";
    }

    @Override
    public String getName() {
        return "保存拓扑场景";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid", isRequired = true),
            @Param(name = "name", type = ApiParamType.STRING, desc = "common.name", isRequired = true),
            @Param(name = "isDefault", type = ApiParamType.INTEGER, desc = "common.isdefault", rule = "0,1", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive", rule = "0,1", isRequired = true),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "common.config")
    })
    @Output({@Param(explode = CiTopoTemplateVo.class)})
    @Description(desc = "保存拓扑场景")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTopoTemplateVo ciTopoTemplateVo = JSONObject.toJavaObject(jsonObj, CiTopoTemplateVo.class);
        Long id = jsonObj.getLong("id");
        if (ciTopoTemplateVo.getIsDefault().equals(1)) {
            ciTopoTemplateMapper.resetCiTopoTemplateIsDefault(ciTopoTemplateVo);
        }
        if (id != null) {
            ciTopoTemplateMapper.updateCiTopoTemplate(ciTopoTemplateVo);
        } else {
            ciTopoTemplateMapper.insertCiTopoTemplate(ciTopoTemplateVo);

        }
        return null;
    }
}
