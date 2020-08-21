package codedriver.module.cmdb.api.rel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.exception.ci.RelIsExistsException;

@Service
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class InsertRelApi extends ApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/insert";
    }

    @Override
    public String getName() {
        return "添加模型关系";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "关系名称"),
        @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", isRequired = true, desc = "录入方式"),
        @Param(name = "fromCiId", type = ApiParamType.LONG, isRequired = true, desc = "来源模型id"),
        @Param(name = "fromName", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "来源名称",
            maxLength = 100),
        @Param(name = "fromLabel", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "来源标签",
            maxLength = 200),
        @Param(name = "fromTypeId", type = ApiParamType.LONG, desc = "来源类型id"),
        @Param(name = "fromGroupId", type = ApiParamType.LONG, desc = "来源分组id"),
        @Param(name = "fromRule", type = ApiParamType.ENUM, rule = "1:n,1:1,0:1,0:n", isRequired = true, desc = "来源规则"),
        @Param(name = "toCiId", type = ApiParamType.LONG, isRequired = true, desc = "来源模型id"),
        @Param(name = "toName", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "目标名称",
            maxLength = 100),
        @Param(name = "toLabel", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "目标标签",
            maxLength = 200),
        @Param(name = "toTypeId", type = ApiParamType.LONG, desc = "目标类型id"),
        @Param(name = "toGroupId", type = ApiParamType.LONG, desc = "目标分组id"),
        @Param(name = "toRule", type = ApiParamType.ENUM, rule = "1:n,1:1,0:1,0:n", isRequired = true, desc = "目标规则")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "关系id")})
    @Description(desc = "添加模型关系接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelVo relVo = JSONObject.toJavaObject(jsonObj, RelVo.class);
        if (relMapper.checkRelByFromToName(relVo.getFromName(), relVo.getToName()) > 0) {
            throw new RelIsExistsException(relVo.getFromName(), relVo.getToName());
        }
        relMapper.insertRel(relVo);
        return relVo.getId();
    }

}
