/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.group;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.cmdb.exception.group.GroupNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.GROUP_MODIFY;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import neatlogic.module.cmdb.group.CiEntityGroupManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = GROUP_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ExecGroupApi extends PrivateApiComponentBase {

    @Resource
    private GroupMapper groupMapper;


    @Override
    public String getToken() {
        return "/cmdb/group/exec";
    }

    @Override
    public String getName() {
        return "应用团体规则";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "团体id", isRequired = true),
            @Param(name = "isSync", type = ApiParamType.INTEGER, desc = "是否同步，是则会删除不符合规则的配置项关联，默认：0")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "应用团体规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer isSync = jsonObj.getIntValue("isSync");
        GroupVo groupVo = groupMapper.getGroupById(id);
        if (groupVo == null) {
            throw new GroupNotFoundException(id);
        }
        groupVo.setIsSync(isSync);
        CiEntityGroupManager.group(groupVo);
        return null;
    }
}
