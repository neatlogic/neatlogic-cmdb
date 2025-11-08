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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class BatchDeleteCiEntityApi extends PrivateApiComponentBase implements IBatchDeleteCiEntityApiCrossoverService {

    @Resource
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
