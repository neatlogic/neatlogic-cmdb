package codedriver.module.cmdb.api.ciview;

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
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dto.ci.CiViewVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiViewApi extends PrivateApiComponentBase {

    @Autowired
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ciview/get";
    }

    @Override
    public String getName() {
        return "获取模型显示设置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = CiViewVo[].class)})
    @Description(desc = "获取模型显示设置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiViewVo ciViewVo = JSONObject.toJavaObject(jsonObj, CiViewVo.class);
        return ciViewMapper.getCiViewByCiId(ciViewVo);
    }

}
