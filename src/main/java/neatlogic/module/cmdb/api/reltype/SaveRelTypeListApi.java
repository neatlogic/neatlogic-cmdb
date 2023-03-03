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

package neatlogic.module.cmdb.api.reltype;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RELTYPE_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.RelTypeMapper;
import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.cmdb.exception.reltype.RelTypeNameIsExistsException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = RELTYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveRelTypeListApi extends PrivateApiComponentBase {

    @Autowired
    private RelTypeMapper relTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/reltype/save";
    }

    @Override
    public String getName() {
        return "保存模型关系类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "关系类型id，不提供代表添加"),
            @Param(name = "name", type = ApiParamType.STRING, maxLength = 10, isRequired = true, xss = true, desc = "关系类型名称"),
            @Param(name = "isShowInTopo", type = ApiParamType.INTEGER, rule = "1,0", desc = "是否在拓扑图中展示"),
            @Param(name = "description", type = ApiParamType.STRING, maxLength = 500, xss = true, desc = "关系类型说明")
    })
    @Description(desc = "保存模型关系类型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelTypeVo relTypeVo = JSONObject.toJavaObject(jsonObj, RelTypeVo.class);
        if (relTypeMapper.checkRelTypeNameIsExists(relTypeVo) > 0) {
            throw new RelTypeNameIsExistsException(relTypeVo.getName());
        }
        if (jsonObj.getLong("id") == null) {
            relTypeMapper.insertRelType(relTypeVo);
        } else {
            relTypeMapper.updateRelType(relTypeVo);
        }
        return relTypeVo.getId();
    }
}
