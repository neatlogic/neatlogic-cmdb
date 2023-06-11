/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
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

package neatlogic.module.cmdb.api.cientity;

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
        CiEntityAlertVo ciEntityAlertVo = JSONObject.toJavaObject(jsonObj, CiEntityAlertVo.class);
        int rowNum = ciEntityAlertMapper.searchCiEntityAlertCount(ciEntityAlertVo);
        List<CiEntityAlertVo> ciEntityAlertVoList = null;
        if (rowNum > 0) {
            ciEntityAlertVoList = ciEntityAlertMapper.searchCiEntityAlert(ciEntityAlertVo);
            ciEntityAlertVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(ciEntityAlertVoList, ciEntityAlertVo);
    }


}
