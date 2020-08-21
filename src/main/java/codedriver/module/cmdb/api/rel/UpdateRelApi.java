package codedriver.module.cmdb.api.rel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.RelVo;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateRelApi extends ApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/update";
    }

    @Override
    public String getName() {
        return "修改模型关系";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "关系id"),
        @Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "关系名称"),
        @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", isRequired = true, desc = "录入方式"),
        @Param(name = "fromLabel", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "来源标签",
            maxLength = 200),
        @Param(name = "fromTypeId", type = ApiParamType.LONG, desc = "来源类型id"),
        @Param(name = "fromGroupId", type = ApiParamType.LONG, desc = "来源分组id"),
        @Param(name = "fromRule", type = ApiParamType.ENUM, rule = "1:n,1:1,0:1,0:n", isRequired = true, desc = "来源规则"),
        @Param(name = "toLabel", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "目标标签",
            maxLength = 200),
        @Param(name = "toTypeId", type = ApiParamType.LONG, desc = "目标类型id"),
        @Param(name = "toGroupId", type = ApiParamType.LONG, desc = "目标分组id"),
        @Param(name = "toRule", type = ApiParamType.ENUM, rule = "1:n,1:1,0:1,0:n", isRequired = true, desc = "目标规则")})
    @Description(desc = "修改模型关系接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelVo relVo = JSONObject.toJavaObject(jsonObj, RelVo.class);
        relMapper.updateRel(relVo);
        return relVo.getId();
    }

}
