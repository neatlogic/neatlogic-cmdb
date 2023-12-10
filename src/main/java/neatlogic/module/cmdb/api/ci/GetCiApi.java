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

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private CiSchemaMapper ciSchemaMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/get";
    }

    @Override
    public String getName() {
        return "nmcac.getciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.getciapi.input.param.desc.needaction")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "nmcac.getciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        ciVo.setHasData(ciSchemaMapper.checkTableHasData(ciVo.getCiTableName()) > 0);
        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null) {
            ciVo.setFileVo(fileMapper.getFileById(ciVo.getFileId()));
        }
        boolean needAction = jsonObj.getBooleanValue("needAction");
        if (needAction) {
            Map<String, Boolean> authData = new HashMap<>();
            boolean hasCiManageAuth, hasCiEntityInsertAuth = false, hasCiEntityUpdateAuth = false, hasCiEntityTransactionAuth = false;
            hasCiManageAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
            if (hasCiManageAuth) {
                hasCiEntityInsertAuth = true;
                hasCiEntityTransactionAuth = true;
                hasCiEntityUpdateAuth = true;
            } else if (ciVo.getIsVirtual().equals(0) && ciVo.getIsAbstract().equals(0)) {
                hasCiEntityUpdateAuth = CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check();
                hasCiEntityInsertAuth = CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check();
                hasCiEntityTransactionAuth = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check();
            }

            authData.put(CiAuthType.CIMANAGE.getValue(), hasCiManageAuth);
            authData.put(CiAuthType.CIENTITYUPDATE.getValue(), hasCiEntityUpdateAuth);
            authData.put(CiAuthType.CIENTITYINSERT.getValue(), hasCiEntityInsertAuth);
            authData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), hasCiEntityTransactionAuth);
            ciVo.setAuthData(authData);
        }
        return ciVo;
    }
}
