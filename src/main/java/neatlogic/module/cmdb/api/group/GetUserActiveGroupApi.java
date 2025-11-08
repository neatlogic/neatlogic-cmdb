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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetUserActiveGroupApi extends PrivateApiComponentBase {

    @Resource
    private GroupMapper groupMapper;

    @Override
    public String getToken() {
        return "/cmdb/group/getuseractivegroup";
    }

    @Override
    public String getName() {
        return "nmcag.getuseractivegroupapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid")})
    @Output({@Param(explode = GroupVo[].class)})
    @Description(desc = "nmcag.getuseractivegroupapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String userUuid = UserContext.get().getUserUuid();
        List<String> teamUuidList = UserContext.get().getTeamUuidList();
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        List<Long> groupIdList = groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
        List<GroupVo> groupList = groupMapper.getActiveGroupByCiId(ciId);
        groupList.removeIf(group -> !groupIdList.contains(group.getId()));
        return groupList;
    }
}
