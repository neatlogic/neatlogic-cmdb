/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.auth.label.CI_MODIFY;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.exception.ci.CiAuthException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteCiApi extends PrivateApiComponentBase {


    @Resource
    private CiService ciService;


    @Override
    public String getToken() {
        return "/cmdb/ci/delete";
    }

    @Override
    public String getName() {
        return "删除模型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "删除模型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        boolean hasAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
        if (!hasAuth) {
            throw new CiAuthException();
        }

        // 删除配置项信息
        ciService.deleteCi(ciId);
        return null;
    }
}
