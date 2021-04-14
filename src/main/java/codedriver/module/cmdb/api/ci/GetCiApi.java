/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.CiAuthType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;

@Service
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
            boolean hasAuth = AuthActionChecker.check("CI_MODIFY");
            Map<String, Boolean> authData = new HashMap<>();
            boolean hasCiManageAuth = hasAuth, hasCiEntityInsertAuth = hasAuth;
            if (!hasCiManageAuth) {
                hasCiEntityInsertAuth = hasCiManageAuth = CiAuthChecker.hasCiManagePrivilege(ciId);
            }
            if (!hasCiEntityInsertAuth) {
                hasCiEntityInsertAuth = CiAuthChecker.hasCiEntityInsertPrivilege(ciId);
            }
            authData.put(CiAuthType.CIMANAGE.getValue(), hasCiManageAuth);
            authData.put(CiAuthType.CIENTITYINSERT.getValue(), hasCiEntityInsertAuth);
            ciVo.setAuthData(authData);
        }
        return ciVo;
    }
}
