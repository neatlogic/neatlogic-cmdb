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

package neatlogic.module.cmdb.api.ciview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiViewApi extends PrivateApiComponentBase {

    @Resource
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ciview/get";
    }

    @Override
    public String getName() {
        return "nmcac.getciviewapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid")})
    @Output({@Param(explode = CiViewVo[].class)})
    @Description(desc = "nmcac.getciviewapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiViewVo ciViewVo = JSON.toJavaObject(jsonObj, CiViewVo.class);
        return RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo), ciViewVo.getCiId());
    }

}
