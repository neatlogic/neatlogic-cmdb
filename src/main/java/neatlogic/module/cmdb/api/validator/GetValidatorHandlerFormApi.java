/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.validator;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.exception.validator.ValidatorHandlerNotFoundException;
import neatlogic.framework.cmdb.validator.core.IValidator;
import neatlogic.framework.cmdb.validator.core.ValidatorFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetValidatorHandlerFormApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/validator/form/get";
    }

    @Override
    public String getName() {
        return "获取校验组件表单";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "handler", type = ApiParamType.STRING, desc = "组件名称", isRequired = true)})
    @Description(desc = "获取校验组件表单接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String handler = jsonObj.getString("handler");
        IValidator validator = ValidatorFactory.getValidator(handler);
        if (validator == null) {
            throw new ValidatorHandlerNotFoundException(handler);
        }
        return validator.getForm();
    }
}
