/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.GroupType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.cientity.CiEntityAuthException;
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
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/save";
    }

    @Override
    public String getName() {
        return "保存配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "配置项id，不存在代表添加"),
            @Param(name = "attrEntityData", type = ApiParamType.JSONOBJECT, desc = "属性数据"),
            @Param(name = "relEntityData", type = ApiParamType.JSONOBJECT, desc = "关系数据")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "配置项id")})
    @Description(desc = "保存配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long id = jsonObj.getLong("id");
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        if (id != null) {
            if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).checkIsInGroup(id, GroupType.MAINTAIN).check()) {
                throw new CiEntityAuthException(TransactionActionType.UPDATE.getText());
            }
            ciEntityTransactionVo.setCiEntityId(id);
            ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
        } else {
            if (!CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).check()) {
                throw new CiEntityAuthException(TransactionActionType.INSERT.getText());
            }
            ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
        }


        ciEntityTransactionVo.setCiId(ciId);

        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
        ciEntityTransactionVo.setAttrEntityData(attrObj);

        JSONObject relObj = jsonObj.getJSONObject("relEntityData");
        ciEntityTransactionVo.setRelEntityData(relObj);


        Long transactionId = ciEntityService.saveCiEntity(ciEntityTransactionVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("transactionId", transactionId);
        if (transactionId > 0) {
            returnObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
        }
        return returnObj;
    }

}
