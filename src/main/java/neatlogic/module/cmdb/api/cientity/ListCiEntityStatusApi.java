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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityStatusVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiEntityStatusApi extends PrivateApiComponentBase {


    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/status/list";
    }

    @Override
    public String getName() {
        return "nmcac.listcientitystatusapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nmcac.getcientitybaseinfolistapi.input.param.desc.idlist"),
    })
    @Output({@Param(explode = CiEntityStatusVo[].class)})
    @Description(desc = "nmcac.listcientitystatusapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciEntityIdObjectList = jsonObj.getJSONArray("ciEntityIdList");
        List<Long> ciEntityIdList = new ArrayList<>();
        for (int i = 0; i < ciEntityIdObjectList.size(); i++) {
            ciEntityIdList.add(ciEntityIdObjectList.getLong(i));
        }
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            return ciEntityAlertMapper.listCiEntityStatus(ciEntityIdList);
        }
        return null;
    }


}
