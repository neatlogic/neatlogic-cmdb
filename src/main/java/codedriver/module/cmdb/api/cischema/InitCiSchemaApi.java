package codedriver.module.cmdb.api.cischema;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.cischema.CiSchemaHandler;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class InitCiSchemaApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cischema/init";
    }

    @Override
    public String getName() {
        return "重建模型视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id")
    })
    @Description(desc = "重建模型视图表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        CiVo ciVo = ciMapper.getCiById(jsonObj.getLong("id"));
        if (ciVo != null) {
            CiSchemaHandler.initCiSchema(ciVo);
        } else {
            throw new CiNotFoundException(ciId);
        }
        return null;
    }

}
