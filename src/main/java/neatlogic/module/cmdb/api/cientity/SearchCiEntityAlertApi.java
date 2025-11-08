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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityAlertApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/alert/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchcientityalertapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityIdList", type = ApiParamType.JSONARRAY, desc = "nmcac.getcientitybaseinfolistapi.input.param.desc.idlist", help = "nmcac.searchcientityalertapi.input.param.help.idlist"),
            @Param(name = "groupIdList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityalertapi.input.param.desc.groupidlist", help = "nmcac.searchcientityalertapi.input.param.help.groupidlist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage")
    })
    @Output({@Param(explode = BasePageVo[].class)})
    @Description(desc = "nmcac.searchcientityalertapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityAlertVo ciEntityAlertVo = JSON.toJavaObject(jsonObj, CiEntityAlertVo.class);
        int rowNum = ciEntityAlertMapper.searchCiEntityAlertCount(ciEntityAlertVo);
        List<CiEntityAlertVo> ciEntityAlertVoList = null;
        if (rowNum > 0) {
            ciEntityAlertVoList = ciEntityAlertMapper.searchCiEntityAlert(ciEntityAlertVo);
            ciEntityAlertVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(ciEntityAlertVoList, ciEntityAlertVo);
    }


}
