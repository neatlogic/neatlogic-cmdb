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
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiTreeApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/tree/save";
    }

    @Override
    public String getName() {
        return "保存模型树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nodeList", type = ApiParamType.JSONARRAY, desc = "节点列表", isRequired = true)})
    @Description(desc = "保存模型树")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray nodeList = jsonObj.getJSONArray("nodeList");
        for (int i = 0; i < nodeList.size(); i++) {
            CiVo ciVo = JSONObject.toJavaObject(nodeList.getJSONObject(i), CiVo.class);
            ciMapper.saveCiTreeItem(ciVo);
        }
        return null;
    }

}
