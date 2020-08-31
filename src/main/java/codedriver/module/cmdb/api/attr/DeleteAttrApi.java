package codedriver.module.cmdb.api.attr;

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
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.exception.attr.AttrDeleteDeniedException;
import codedriver.module.cmdb.exception.attr.AttrNotFoundException;

@Service
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/delete";
    }

    @Override
    public String getName() {
        return "删除模型属性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)})
    @Description(desc = "删除模型属性接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long attrId = jsonObj.getLong("id");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }
        if (attrVo.getIsPrivate().equals(1)) {
            throw new AttrDeleteDeniedException();
        }
        attrMapper.deleteAttrById(attrId);
        // FIXME 补充删除attrentity 数据
        return null;
    }

}
