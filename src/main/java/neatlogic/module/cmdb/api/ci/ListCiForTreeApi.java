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

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiForTreeApi extends PrivateApiComponentBase {
    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "获取模型树型列表";
    }

    @Override
    public String getToken() {
        return "cmdb/ci/listtree";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id")})
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "返回模型树型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        List<CiVo> ciList = ciMapper.getAllCi(null);
        Map<Long, CiVo> ciMap = new HashMap<>();
        for (CiVo ciVo : ciList) {
            ciMap.put(ciVo.getId(), ciVo);
        }
        //将模型挂到父模型上
        for (CiVo ciVo : ciList) {
            if (ciVo.getParentCiId() != null) {
                CiVo parentCiVo = ciMap.get(ciVo.getParentCiId());
                if (parentCiVo != null) {
                    parentCiVo.addChild(ciVo);
                }
            }
        }
        //清除所有非父节点模型
        ciList.removeIf(ciVo -> ciVo.getParentCiId() != null || (ciId != null && (ciVo.getId().equals(ciId) || checkCiIdIsParent(ciId, ciVo))));
        return ciList;
    }

    private boolean checkCiIdIsParent(Long ciId, CiVo ciVo) {
        while (ciVo.getParentCi() != null) {
            if (ciVo.getParentCi().getId().equals(ciId)) {
                return true;
            }
            ciVo = ciVo.getParentCi();
        }
        return false;
    }


}
