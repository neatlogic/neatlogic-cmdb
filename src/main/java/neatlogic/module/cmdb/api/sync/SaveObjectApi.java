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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.sync.ObjectVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.sync.ObjectIsExistsException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.sync.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveObjectApi extends PrivateApiComponentBase {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "nmcas.saveobjectapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "objCategory", type = ApiParamType.STRING, desc = "term.cmdb.objcategory", isRequired = true),
            @Param(name = "objType", type = ApiParamType.STRING, desc = "term.cmdb.objtype", isRequired = true),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid")})
    @Description(desc = "nmcas.saveobjectapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        ObjectVo objectVo = JSONObject.toJavaObject(paramObj, ObjectVo.class);
        if (objectVo.getCiId() != null) {
            CiVo ciVo = ciMapper.getCiById(objectVo.getCiId());
            if (ciVo == null) {
                throw new CiNotFoundException(objectVo.getCiId());
            }
        }
        if (objectMapper.checkObjectIsExists(objectVo) > 0) {
            throw new ObjectIsExistsException(objectVo);
        }
        if (id == null) {
            objectMapper.insertObject(objectVo);
        } else {
            objectMapper.updateObject(objectVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/object/save";
    }
}
