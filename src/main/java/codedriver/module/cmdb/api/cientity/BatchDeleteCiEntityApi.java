/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
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
public class BatchDeleteCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchdelete";
    }

    @Override
    public String getName() {
        return "批量删除配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "删除列表，包含ciId、ciEntityId和ciEntityName三个属性"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "是否需要提交")})
    @Output({@Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "事务组id")})
    @Description(desc = "批量删除配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciEntityList = jsonObj.getJSONArray("ciEntityList");
        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        List<Long> ciEntityIdList = new ArrayList<>();
        for (int i = 0; i < ciEntityList.size(); i++) {
            JSONObject data = ciEntityList.getJSONObject(i);
            Long ciId = data.getLong("ciId");
            Long ciEntityId = data.getLong("ciEntityId");
            String ciEntityName = data.getString("ciEntityName");
            ciEntityIdList.add(ciEntityId);
            if (!CiAuthChecker.chain().checkCiEntityDeletePrivilege(ciId).checkIsInGroup(ciEntityId, GroupType.MAINTAIN).check()) {
                throw new CiEntityAuthException(ciEntityId, ciEntityName, TransactionActionType.DELETE.getText());
            }
            if (needCommit) {
                needCommit = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).checkIsInGroup(ciEntityId, GroupType.MAINTAIN).check();
                if (!needCommit) {
                    throw new CiEntityAuthException(ciEntityId, ciEntityName, TransactionActionType.DELETE.getText());
                }
            }
        }
        return ciEntityService.deleteCiEntityList(ciEntityIdList, needCommit);
    }

}
