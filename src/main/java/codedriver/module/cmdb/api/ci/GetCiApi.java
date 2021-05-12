/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/get";
    }

    @Override
    public String getName() {
        return "获取模型信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作，需要的话会进行权限校验")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "获取模型信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        boolean needAction = jsonObj.getBooleanValue("needAction");
        if (needAction) {
            Map<String, Boolean> authData = new HashMap<>();
            boolean hasCiManageAuth, hasCiEntityInsertAuth = false;
            hasCiManageAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
            if (ciVo.getIsVirtual().equals(0)) {
                hasCiEntityInsertAuth = CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).check();
            }
            authData.put(CiAuthType.CIMANAGE.getValue(), hasCiManageAuth);
            authData.put(CiAuthType.CIENTITYINSERT.getValue(), hasCiEntityInsertAuth);
            ciVo.setAuthData(authData);
        }
        return ciVo;
    }
}
