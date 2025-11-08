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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.graph.GraphVo;
import neatlogic.framework.cmdb.exception.graph.GraphIsInvokedException;
import neatlogic.framework.cmdb.exception.graph.GraphPrivilegeDeleteException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.graph.GraphMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteGraphApi extends PrivateApiComponentBase {


    @Resource
    private GraphMapper graphMapper;


    @Override
    public String getToken() {
        return "/cmdb/graph/delete";
    }

    @Override
    public String getName() {
        return "nmcag.deletegraphapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")})
    @Description(desc = "nmcag.deletegraphapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        GraphVo graphVo = graphMapper.getGraphById(id);
        if (graphVo != null) {
            if (!graphVo.hasPrivilege()) {
                throw new GraphPrivilegeDeleteException();
            }
            if (graphMapper.checkGraphIsInvoked(id) > 0) {
                throw new GraphIsInvokedException();
            }
            graphMapper.deleteGraphRelByFromGraphId(id);
            graphMapper.deleteGraphAuthByGraphId(id);
            graphMapper.deleteGraphById(id);
        }
        return null;
    }
}
