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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.graph.GraphVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.graph.GraphMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchGraphApi extends PrivateApiComponentBase {

    @Resource
    private GraphMapper graphMapper;

    @Override
    public String getToken() {
        return "/cmdb/graph/search";
    }

    @Override
    public String getName() {
        return "nmcag.searchgraphapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "excludeId", type = ApiParamType.LONG, desc = "common.excludeid"),
            @Param(name = "hasParent", type = ApiParamType.BOOLEAN, desc = "common.hasparent"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "term.cmdb.cientityid"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "common.isneedpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")})
    @Output({@Param(name = "tbodyList", explode = GraphVo[].class, desc = "common.tbodylist"),
            @Param(explode = BasePageVo.class)})
    @Description(desc = "nmcag.searchgraphapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        GraphVo graphVo = JSONObject.toJavaObject(jsonObj, GraphVo.class);
        String userUuid = UserContext.get().getUserUuid(true);
        graphVo.setFcu(userUuid);
        if (AuthActionChecker.check(CUSTOMVIEW_MODIFY.class.getSimpleName())) {
            graphVo.setAdmin(true);
        } else {
            AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
            graphVo.setUserUuid(authenticationInfoVo.getUserUuid());
            graphVo.setTeamUuidList(authenticationInfoVo.getTeamUuidList());
            graphVo.setRoleUuidList(authenticationInfoVo.getRoleUuidList());
        }
        List<GraphVo> graphList = graphMapper.searchGraph(graphVo);
        if (CollectionUtils.isNotEmpty(graphList)) {
            int rowNum = graphMapper.searchGraphCount(graphVo);
            graphVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(graphList, graphVo);
    }
}
