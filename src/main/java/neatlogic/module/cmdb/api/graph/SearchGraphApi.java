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

package neatlogic.module.cmdb.api.graph;

import com.alibaba.fastjson.JSON;
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
        GraphVo graphVo = JSON.toJavaObject(jsonObj, GraphVo.class);
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
