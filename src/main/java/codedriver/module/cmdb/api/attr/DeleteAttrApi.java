package codedriver.module.cmdb.api.attr;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.exception.attr.AttrDeleteDeniedException;
import codedriver.module.cmdb.exception.attr.AttrIsInvokedByExpressionException;
import codedriver.module.cmdb.exception.attr.AttrNotFoundException;
import codedriver.module.cmdb.service.attr.AttrService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

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
        if (attrVo.getIsPrivate() != null && attrVo.getIsPrivate().equals(1)) {
            throw new AttrDeleteDeniedException();
        }
        //获取当前属性所属模型的所有属性，检查当前属性是否被其他属性引用
        List<AttrVo> attrList = attrMapper.getAttrByCiId(attrVo.getCiId());
        boolean isUsed = false;
        for (AttrVo otherAttr : attrList) {
            if (!otherAttr.getId().equals(attrVo.getId()) && otherAttr.getType().equals(AttrType.EXPRESSION.getValue())) {
                if (otherAttr.getExpression().contains("{" + attrVo.getName() + "}")) {
                    throw new AttrIsInvokedByExpressionException();
                }
            }
        }
        attrService.deleteAttrById(attrVo);
        return null;
    }

}
