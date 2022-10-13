/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.group;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.group.GroupVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetUserActiveGroupApi extends PrivateApiComponentBase {

    @Resource
    private GroupMapper groupMapper;

    @Resource
    private TeamMapper teamMapper;


    @Override
    public String getToken() {
        return "/cmdb/group/getuseractivegroup";
    }

    @Override
    public String getName() {
        return "获取当前用户激活团体列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = GroupVo[].class)})
    @Description(desc = "获取当前用户激活团体列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String userUuid = UserContext.get().getUserUuid(true);
        List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        List<Long> groupIdList = groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
        List<GroupVo> groupList = groupMapper.getActiveGroupByCiId(ciId);
        groupList.removeIf(group -> !groupIdList.contains(group.getId()));
        return groupList;
    }
}
