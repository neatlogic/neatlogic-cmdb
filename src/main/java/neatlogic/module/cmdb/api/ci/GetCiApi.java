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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.getciapi.input.param.desc.needaction"),
            @Param(name = "needChildren", type = ApiParamType.BOOLEAN, desc = "term.cmdb.needchildren")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "nmcac.getciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        boolean needChildren = jsonObj.getBooleanValue("needChildren");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        if (needChildren) {
            List<CiVo> childCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            ciVo.setChildren(childCiList.stream().filter(d -> !d.getId().equals(ciVo.getId())).collect(Collectors.toList()));
        }
        ciVo.setHasData(ciSchemaMapper.checkTableHasData(ciVo.getCiTableName()) > 0);
        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null) {
            ciVo.setFileVo(fileMapper.getFileById(ciVo.getFileId()));
        }
        boolean needAction = jsonObj.getBooleanValue("needAction");
        if (needAction) {
            Map<String, Boolean> authData = new HashMap<>();
            boolean hasCiManageAuth;
            boolean hasCiEntityInsertAuth = false;
            boolean hasCiEntityUpdateAuth = false;
            boolean hasCiEntityTransactionAuth = false;
            hasCiManageAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
            if (hasCiManageAuth) {
                hasCiEntityInsertAuth = true;
                hasCiEntityTransactionAuth = true;
                hasCiEntityUpdateAuth = true;
            } else if (ciVo.getIsVirtual().equals(0) && ciVo.getIsAbstract().equals(0)) {
                hasCiEntityUpdateAuth = CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).check();
                hasCiEntityInsertAuth = CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).check();
                hasCiEntityTransactionAuth = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).check();
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
