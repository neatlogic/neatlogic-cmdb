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

package neatlogic.module.cmdb.api.group;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.GROUP_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = GROUP_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteGroupApi extends PrivateApiComponentBase {

    @Resource
    private GroupMapper groupMapper;


    @Override
    public String getToken() {
        return "/cmdb/group/delete";
    }

    @Override
    public String getName() {
        return "删除团体";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "团体id", isRequired = true)})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "删除团体接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        groupMapper.deleteGroupById(id);
        return null;
    }
}
