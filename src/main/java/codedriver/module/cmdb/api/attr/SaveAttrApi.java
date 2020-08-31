package codedriver.module.cmdb.api.attr;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.prop.PropMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.prop.PropVo;
import codedriver.module.cmdb.exception.attr.AttrExpressionIsValidedException;
import codedriver.module.cmdb.exception.attr.AttrNotFoundException;
import codedriver.module.cmdb.exception.attr.AttrPropIdIsValidedException;
import codedriver.module.cmdb.exception.prop.PropNotFoundException;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private PropMapper propMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/save";
    }

    @Override
    public String getName() {
        return "保存模型属性";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表添加"),
        @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "type", type = ApiParamType.ENUM, rule = "custom,property,expression", isRequired = true,
            desc = "属性类型，自定义｜属性值｜表达式"),
        @Param(name = "propId", type = ApiParamType.LONG, desc = "属性id"),
        @Param(name = "expression", type = ApiParamType.STRING, desc = "表达式"),
        @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, maxLength = 50, desc = "英文名称"),
        @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100,
            isRequired = true),
        @Param(name = "description", type = ApiParamType.STRING, desc = "备注", maxLength = 500, xss = true),
        @Param(name = "validator", type = ApiParamType.STRING, desc = "校验组件"),
        @Param(name = "validConfig", type = ApiParamType.JSONOBJECT, desc = "校验设置"),
        @Param(name = "isRequired", type = ApiParamType.INTEGER, desc = "是否必填"),
        @Param(name = "isUnique", type = ApiParamType.INTEGER, desc = "是否唯一"),
        @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", desc = "输入类型，人工录入|自动发现"),
        @Param(name = "groupName", type = ApiParamType.STRING, desc = "分组")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "属性id"),})
    @Description(desc = "保存模型属性接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AttrVo attrVo = JSONObject.toJavaObject(jsonObj, AttrVo.class);
        Long attrId = jsonObj.getLong("id");
        if (attrVo.getType().equals(AttrType.EXPRESSION.getValue())) {
            if (StringUtils.isBlank(attrVo.getExpression())) {
                throw new AttrExpressionIsValidedException();
            }
        } else if (attrVo.getType().equals(AttrType.PROPERTY.getValue())) {
            if (attrVo.getPropId() == null) {
                throw new AttrPropIdIsValidedException();
            }
            PropVo propVo = propMapper.getPropById(attrVo.getPropId());
            if (propVo == null) {
                throw new PropNotFoundException(attrVo.getPropId());
            }
        } else {
            attrVo.setExpression(null);
            attrVo.setPropId(null);
        }
        if (attrId == null) {
            attrMapper.insertAttr(attrVo);
        } else {
            if (attrMapper.getAttrById(attrId) == null) {
                throw new AttrNotFoundException(attrId);
            }
            attrMapper.updateAttr(attrVo);
        }
        return attrVo.getId();
    }

}
