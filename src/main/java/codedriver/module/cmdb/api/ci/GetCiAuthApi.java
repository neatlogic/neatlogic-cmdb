package codedriver.module.cmdb.api.ci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiAuthMapper;
import codedriver.module.cmdb.dto.ci.CiVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiAuthApi extends PrivateApiComponentBase {

    @Autowired
    private CiAuthMapper ciAuthMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/auth/get";
    }

    @Override
    public String getName() {
        return "获取模型授权信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "获取模型授权信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        return ciAuthMapper.getCiAuthByCiId(ciId);
    }
}
