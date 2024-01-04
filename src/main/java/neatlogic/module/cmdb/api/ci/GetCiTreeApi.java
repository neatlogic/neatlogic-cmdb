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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiTreeApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/tree";
    }

    @Override
    public String getName() {
        return "nmcac.getciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "nmcac.getciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<CiVo> ciList = ciMapper.getCiTree();
        JSONArray returnList = new JSONArray();
        for (CiVo ciVo : ciList) {
            JSONObject ciObj = new JSONObject();
            ciObj.put("id", ciVo.getId());
            ciObj.put("parentCiId", ciVo.getParentCiId());
            ciObj.put("icon", ciVo.getIcon());
            ciObj.put("name", ciVo.getName());
            ciObj.put("label", ciVo.getLabel());
            ciObj.put("sort", ciVo.getSort());
            returnList.add(ciObj);
        }
        return returnList;
    }


}
