/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.TransactionVo;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.cmdb.enums.TransactionActionType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/delete";
    }

    @Override
    public String getName() {
        return "删除配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "是否需要提交"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注", xss = true)})
    @Output({@Param(name = "transactionId", type = ApiParamType.LONG, desc = "事务id")})
    @Description(desc = "删除配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        String description = jsonObj.getString("description");
        CiEntityVo oldCiEntityVo = ciEntityService.getCiEntityBaseInfoById(id);
        if (oldCiEntityVo == null) {
            throw new CiEntityNotFoundException(id);
        }
        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        if (!CiAuthChecker.chain().checkCiEntityDeletePrivilege(oldCiEntityVo.getCiId()).checkIsInGroup(id, GroupType.MAINTAIN).check()) {
            throw new CiEntityAuthException(TransactionActionType.DELETE.getText());
        }
        if (needCommit) {
            needCommit = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(oldCiEntityVo.getCiId()).checkIsInGroup(id, GroupType.MAINTAIN).check();
        }
        TransactionVo t = new TransactionVo();
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(id);
        ciEntityVo.setDescription(description);
        ciEntityService.deleteCiEntity(ciEntityVo, needCommit);
        return null;
    }

}
