/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.group;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.GROUP_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.group.GroupAuthVo;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.group.GroupService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = GROUP_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveGroupApi extends PrivateApiComponentBase {

    @Resource
    private GroupService groupService;


    @Override
    public String getToken() {
        return "/cmdb/group/save";
    }

    @Override
    public String getName() {
        return "保存团体信息";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，提供代表修改，不提供代表添加"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称", maxLength = 50, xss = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活，1：激活，0：禁用"),
            @Param(name = "description", type = ApiParamType.STRING, maxLength = 300, xss = true, desc = "描述"),
            @Param(name = "ciGroupList", type = ApiParamType.JSONARRAY, desc = "模型规则列表"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "授权列表")})
    @Output({@Param(explode = CiVo.class)})
    @Example(example = "{\n" +
            "  \"ciGroupList\": [\n" +
            "    {\n" +
            "      \"ciId\": 350611677437952,\n" +
            "      \"rule\": {\n" +
            "        \"conditionGroupRelList\": [],\n" +
            "        \"conditionGroupList\": [\n" +
            "          {\n" +
            "            \"uuid\": \"5bf36d76e40241498c954c50e3115bf6\",\n" +
            "            \"conditionList\": [\n" +
            "              {\n" +
            "                \"uuid\": \"66a00081bcee46dfbac50c92c00df8a1\",\n" +
            "                \"id\": \"attr_350460481167361\",\n" +
            "                \"label\": \"\",\n" +
            "                \"name\": \"\",\n" +
            "                \"type\": \"attr\",\n" +
            "                \"expression\": \"equal\",\n" +
            "                \"valueList\": [\n" +
            "                  \"123\"\n" +
            "                ]\n" +
            "              }\n" +
            "            ],\n" +
            "            \"conditionRelList\": []\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"ciLabel\": \"应用系统\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"name\": \"测试团体\",\n" +
            "  \"isActive\": 1,\n" +
            "  \"type\": \"maintain\"\n" +
            "}")
    @Description(desc = "保存团体信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        GroupVo groupVo = JSONObject.toJavaObject(jsonObj, GroupVo.class);
        //转换权限格式
        List<GroupAuthVo> groupAuthList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(groupVo.getAuthList())) {
            for (String auth : groupVo.getAuthList()) {
                GroupAuthVo authVo = new GroupAuthVo();
                authVo.setAuthType(auth.split("#")[0]);
                authVo.setAuthUuid(auth.split("#")[1]);
                groupAuthList.add(authVo);
            }
        }
        groupVo.setGroupAuthList(groupAuthList);
        if (id == null) {
            groupVo.setFcu(UserContext.get().getUserUuid(true));
            groupService.insertGroup(groupVo);
        } else {
            groupVo.setLcu(UserContext.get().getUserUuid(true));
            groupService.updateGroup(groupVo);
        }
        return null;
    }
}
