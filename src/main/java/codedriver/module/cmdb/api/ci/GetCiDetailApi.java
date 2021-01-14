package codedriver.module.cmdb.api.ci;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetCiDetailApi extends PrivateApiComponentBase {

    @Autowired
    private CiService ciService;

    @Autowired
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/detail/get";
    }

    @Override
    public String getName() {
        return "获取模型属性与关系信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "获取模型属性与关系信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        if(ciMapper.getCiById(ciId) == null){
            throw new CiNotFoundException(ciId);
        }
        CiVo ci = ciService.getCiById(ciId);
        return ci;
    }
}
