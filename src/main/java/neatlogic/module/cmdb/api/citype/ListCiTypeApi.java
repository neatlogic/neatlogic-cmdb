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

package neatlogic.module.cmdb.api.citype;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiTypeApi extends PrivateApiComponentBase {

    @Resource
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/citype/list";
    }

    @Override
    public String getName() {
        return "nmcac.listcitypeapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    /**
     * 查询层级，仅在调用方明确传入显示条件时过滤。
     */
    @Input({@Param(name = "isShowInCiEntityQuery", type = ApiParamType.INTEGER, rule = "1", desc = "term.cmdb.filterbycientityqueryvisibility")})
    @Output({@Param(explode = CiTypeVo[].class)})
    @Description(desc = "nmcac.listcitypeapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTypeVo ciTypeVo = new CiTypeVo();
        ciTypeVo.setIsShowInCiEntityQuery(jsonObj.getInteger("isShowInCiEntityQuery"));
        return ciTypeMapper.searchCiType(ciTypeVo);
    }
}
