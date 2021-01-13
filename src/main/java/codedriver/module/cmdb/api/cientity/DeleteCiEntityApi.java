package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;
    @Autowired
    private CiEntityMapper ciEntityMapper;

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

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "配置项id")})
    @Output({@Param(name = "transactionId", type = ApiParamType.LONG, desc = "事务id")})
    @Description(desc = "删除配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("id");
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityById(ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        Long transactionId = ciEntityService.deleteCiEntity(ciEntityId);
        JSONObject returnObj = new JSONObject();
        returnObj.put("transactionId", transactionId);
        return returnObj;
    }

}
