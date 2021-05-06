/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.GroupType;
import codedriver.framework.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/get";
    }

    @Override
    public String getName() {
        return "获取配置项详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要检查操作权限，会根据结果返回action列")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(id);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(id);
        }
        if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciEntityVo.getCiId()).checkIsInGroup(ciEntityVo.getId(), GroupType.READONLY).check()) {
            throw new CiEntityAuthException(ciEntityVo.getCiLabel(), "查看");
        }

        if (needAction) {
            if (CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciEntityVo.getId()).checkIsInGroup(ciEntityVo.getId(), GroupType.MAINTAIN)
                    .check()) {
                ciEntityVo.setAuthData(new HashMap<String, Boolean>() {
                    {
                        this.put(CiAuthType.CIENTITYUPDATE.getValue(), true);
                    }
                });
            }
        }
        return ciEntityVo;
    }

}
