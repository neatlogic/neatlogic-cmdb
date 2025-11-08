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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSON;
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
        ObjectVo objectVo = JSON.toJavaObject(paramObj, ObjectVo.class);
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
