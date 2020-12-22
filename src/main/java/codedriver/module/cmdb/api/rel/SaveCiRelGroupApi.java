package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.RelGroupVo;
import codedriver.module.cmdb.exception.rel.RelGroupNameIsExistsException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiRelGroupApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/relgroup/save";
    }

    @Override
    public String getName() {
        return "保存模型关系分组";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不存在代表新增"),
        @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "name", type = ApiParamType.STRING, xss = true, desc = "名称")})
    @Output({@Param(explode = RelGroupVo[].class)})
    @Description(desc = "保存模型关系分组接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelGroupVo relGroupVo = JSONObject.toJavaObject(jsonObj, RelGroupVo.class);
        Long id = jsonObj.getLong("id");
        if (relMapper.checkRelGroupNameIsExists(relGroupVo) > 0) {
            throw new RelGroupNameIsExistsException(relGroupVo.getName());
        }
        if (id == null) {
            relMapper.insertRelGroup(relGroupVo);
        } else {
            relMapper.updateRelGroup(relGroupVo);
        }
        return relGroupVo.getId();
    }
}
