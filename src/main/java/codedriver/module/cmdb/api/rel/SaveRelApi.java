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
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.exception.ci.RelIsExistsException;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveRelApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/save";
    }

    @Override
    public String getName() {
        return "保存模型关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不存在代表新增"),
        @Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "关系名称"),
        @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", desc = "录入方式"),
        @Param(name = "fromLabel", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "来源标签",
            maxLength = 200),
        @Param(name = "fromTypeId", type = ApiParamType.LONG, desc = "来源类型id"),
        @Param(name = "fromGroupId", type = ApiParamType.LONG, desc = "来源分组id"),
        @Param(name = "fromRule", type = ApiParamType.ENUM, rule = "1:N,1:1,0:1,0:N", isRequired = true, desc = "来源规则"),
        @Param(name = "toLabel", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "目标标签",
            maxLength = 200),
        @Param(name = "toTypeId", type = ApiParamType.LONG, desc = "目标类型id"),
        @Param(name = "toGroupId", type = ApiParamType.LONG, desc = "目标分组id"),
        @Param(name = "toRule", type = ApiParamType.ENUM, rule = "1:N,1:1,0:1,0:N", isRequired = true, desc = "目标规则")})
    @Description(desc = "保存模型关系接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelVo relVo = JSONObject.toJavaObject(jsonObj, RelVo.class);
        Long id = jsonObj.getLong("id");
        if (relMapper.checkRelByFromToName(relVo) > 0) {
            throw new RelIsExistsException(relVo.getFromName(), relVo.getToName());
        }
        if (relMapper.checkRelByFromToLabel(relVo) > 0) {
            throw new RelIsExistsException(relVo.getFromLabel(), relVo.getToLabel());
        }
        if (id == null) {
            relMapper.insertRel(relVo);
        } else {
            relMapper.updateRel(relVo);
        }
        return relVo.getId();
    }

}
