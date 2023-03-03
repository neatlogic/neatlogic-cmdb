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

package neatlogic.module.cmdb.api.group;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.group.GroupAuthVo;
import neatlogic.framework.cmdb.dto.group.GroupVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.GROUP_MODIFY;
import neatlogic.module.cmdb.service.group.GroupService;
import com.alibaba.fastjson.JSONObject;
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
