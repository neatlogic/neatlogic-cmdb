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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.service.group.GroupService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityListApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private GroupService groupService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/list";
    }

    @Override
    public String getName() {
        return "nmcac.getcientitylistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "term.cmdb.cientityid")})
    @Output({@Param(name = "Return", explode = CiEntityVo[].class)})
    @Description(desc = "nmcac.getcientitylistapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ids = jsonObj.getJSONArray("idList");
        Long ciId = jsonObj.getLong("ciId");
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            idList.add(ids.getLong(i));
        }
        boolean needGroup = !CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciId).check();
        if (needGroup) {
            List<Long> groupIdList = groupService.getCurrentUserGroupIdList();
            if (CollectionUtils.isNotEmpty(groupIdList)) {
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(ciId);
                ciEntityVo.setIdList(idList);
                ciEntityVo.setGroupIdList(groupIdList);
                ciEntityVo.setLimitRelEntity(true);
                ciEntityVo.setLimitAttrEntity(true);
                return ciEntityService.getCiEntityByIdList(ciEntityVo);
            } else {
                throw new CiEntityAuthException(TransactionActionType.VIEW.getText());
            }
        } else {
            return ciEntityService.getCiEntityByIdList(ciId, idList);
        }

    }

}
