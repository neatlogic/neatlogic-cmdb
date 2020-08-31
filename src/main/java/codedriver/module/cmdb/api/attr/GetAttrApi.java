package codedriver.module.cmdb.api.attr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/get";
    }

    @Override
    public String getName() {
        return "获取属性详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "属性id")})
    @Output({@Param(explode = AttrVo[].class)})
    @Description(desc = "获取属性详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return attrMapper.getAttrById(jsonObj.getLong("id"));
    }
}
