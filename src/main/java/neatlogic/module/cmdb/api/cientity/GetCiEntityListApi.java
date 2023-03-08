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

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.service.group.GroupService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
        return "获取多个配置项详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项id")})
    @Output({@Param(name = "Return", explode = CiEntityVo[].class)})
    @Description(desc = "根据id列表获取多个配置项详细信息接口")
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
