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
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.crossover.IBatchDeleteCiEntityApiCrossoverService;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class BatchDeleteCiEntityApi extends PrivateApiComponentBase implements IBatchDeleteCiEntityApiCrossoverService {

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchdelete";
    }

    @Override
    public String getName() {
        return "nmcac.batchdeletecientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nmcac.batchdeletecientityapi.input.param.desc.cientitylist"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "nmcac.batchdeletecientityapi.input.param.desc.needcommit"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.memo", xss = true)})
    @Output({@Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "term.cmdb.transactiongroupid")})
    @Description(desc = "nmcac.batchdeletecientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String description = jsonObj.getString("description");
        JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        for (int i = 0; i < ciEntityObjList.size(); i++) {
            JSONObject data = ciEntityObjList.getJSONObject(i);
            Long ciId = data.getLong("ciId");
            Long ciEntityId = data.getLong("ciEntityId");
            String ciEntityName = data.getString("ciEntityName");
            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setId(ciEntityId);
            ciEntityVo.setDescription(description);
            ciEntityList.add(ciEntityVo);
            if (!CiAuthChecker.chain().checkCiEntityDeletePrivilege(ciId).checkCiEntityIsInGroup(ciEntityId, GroupType.MAINTAIN).check()) {
                throw new CiEntityAuthException(ciEntityId, ciEntityName, TransactionActionType.DELETE.getText());
            }
            if (needCommit) {
                needCommit = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).checkCiEntityIsInGroup(ciEntityId, GroupType.MAINTAIN).check();
                if (!needCommit) {
                    throw new CiEntityAuthException(ciEntityId, ciEntityName, TransactionActionType.DELETE.getText());
                }
            }
        }
        return ciEntityService.deleteCiEntityList(ciEntityList, needCommit);
    }

}
