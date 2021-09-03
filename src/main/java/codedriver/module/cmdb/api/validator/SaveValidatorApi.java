/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.validator;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.validator.ValidatorMapper;
import codedriver.framework.cmdb.dto.validator.ValidatorVo;
import codedriver.framework.cmdb.exception.validator.ValidatorHandlerNotFoundException;
import codedriver.framework.cmdb.validator.core.IValidator;
import codedriver.framework.cmdb.validator.core.ValidatorFactory;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveValidatorApi extends PrivateApiComponentBase {

    @Autowired
    private ValidatorMapper validatorMapper;

    @Override
    public String getToken() {
        return "/cmdb/validator/save";
    }

    @Override
    public String getName() {
        return "保存校验规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id,提供代表更新，不提供代表新增"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, maxLength = 30, xss = true, desc = "名称"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"),
            @Param(name = "handler", type = ApiParamType.STRING, isRequired = true, desc = "组件"),
            @Param(name = "description", type = ApiParamType.STRING, xss = true, maxLength = 500, desc = "描述"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "配置"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")})
    @Description(desc = "保存校验规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        ValidatorVo validatorVo = JSONObject.toJavaObject(jsonObj, ValidatorVo.class);
        Long id = jsonObj.getLong("id");
        IValidator validator = ValidatorFactory.getValidator(validatorVo.getHandler());
        if (validator == null) {
            throw new ValidatorHandlerNotFoundException(validatorVo.getHandler());
        }
        if (id == null) {
            validatorMapper.insertValidator(validatorVo);
        } else {
            validatorMapper.updateValidator(validatorVo);
        }
        return null;
    }

}
