/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.validator;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dao.mapper.validator.ValidatorMapper;
import neatlogic.framework.cmdb.dto.validator.ValidatorVo;
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
