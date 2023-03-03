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

package neatlogic.module.cmdb.api.rel;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.framework.cmdb.dto.ci.RelGroupVo;
import neatlogic.framework.cmdb.exception.rel.RelGroupNameIsExistsException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiRelGroupApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/relgroup/save";
    }

    @Override
    public String getName() {
        return "保存模型关系分组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不存在代表新增"),
        @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "name", type = ApiParamType.STRING, xss = true, desc = "名称")})
    @Output({@Param(explode = RelGroupVo[].class)})
    @Description(desc = "保存模型关系分组接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelGroupVo relGroupVo = JSONObject.toJavaObject(jsonObj, RelGroupVo.class);
        Long id = jsonObj.getLong("id");
        if (relMapper.checkRelGroupNameIsExists(relGroupVo) > 0) {
            throw new RelGroupNameIsExistsException(relGroupVo.getName());
        }
        if (id == null) {
            relMapper.insertRelGroup(relGroupVo);
        } else {
            relMapper.updateRelGroup(relGroupVo);
        }
        return relGroupVo.getId();
    }
}
