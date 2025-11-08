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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchRelApi extends PrivateApiComponentBase {

    @Resource
    private RelMapper relMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/rel/search";
    }

    @Override
    public String getName() {
        return "nmcar.searchrelapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")})
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "nmcar.searchrelapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelVo relVo = JSON.toJavaObject(jsonObj, RelVo.class);
        return relMapper.searchRel(relVo);
    }


    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}
