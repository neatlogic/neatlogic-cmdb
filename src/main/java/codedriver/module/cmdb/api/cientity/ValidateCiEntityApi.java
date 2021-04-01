package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ValidateCiEntityApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(ValidateCiEntityApi.class);

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/validate";
    }

    @Override
    public String getName() {
        return "校验配置项完整性";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "id", type = ApiParamType.LONG, desc = "配置项id，不存在代表添加"),
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "配置项uuid"),
            @Param(name = "attrEntityData", type = ApiParamType.JSONOBJECT, desc = "属性数据"),
            @Param(name = "relEntityData", type = ApiParamType.JSONOBJECT, desc = "关系数据")})
    @Output({@Param(name = "hasChange", type = ApiParamType.BOOLEAN, desc = "是否有变化")})
    @Description(desc = "校验配置项完整性接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long id = jsonObj.getLong("id");
        String uuid = jsonObj.getString("uuid");
        TransactionActionType mode = TransactionActionType.INSERT;
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        ciEntityTransactionVo.setCiEntityId(id);
        ciEntityTransactionVo.setCiEntityUuid(uuid);
        ciEntityTransactionVo.setCiId(ciId);
        // 解析属性数据
        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
        ciEntityTransactionVo.setAttrEntityData(attrObj);
        // 解析关系数据
        JSONObject relObj = jsonObj.getJSONObject("relEntityData");
        ciEntityTransactionVo.setRelEntityData(relObj);
        ciEntityTransactionVo.setTransactionMode(mode);
        boolean hasChange = ciEntityService.validateCiEntity(ciEntityTransactionVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("hasChange", hasChange);
        return returnObj;
    }

}
