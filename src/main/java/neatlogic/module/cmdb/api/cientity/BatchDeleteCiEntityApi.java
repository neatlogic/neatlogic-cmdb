/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
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
public class BatchDeleteCiEntityApi extends PrivateApiComponentBase implements IBatchDeleteCiEntityApiCrossoverService {

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
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "是否需要提交"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注", xss = true)})
    @Output({@Param(name = "transactionGroupId", type = ApiParamType.LONG, desc = "事务组id")})
    @Description(desc = "批量删除配置项接口")
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
