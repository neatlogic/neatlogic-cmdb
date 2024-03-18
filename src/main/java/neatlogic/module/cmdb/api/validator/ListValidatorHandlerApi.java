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
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.validator.ValidatorVo;
import neatlogic.framework.cmdb.validator.core.IValidator;
import neatlogic.framework.cmdb.validator.core.ValidatorFactory;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListValidatorHandlerApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "/cmdb/validator/handler/list";
    }

    @Override
    public String getName() {
        return "获取校验组件列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = ValidatorVo.class)})
    @Description(desc = "获取校验组件列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<IValidator> handlerList = ValidatorFactory.getValidatorHandlerList();
        List<ValueTextVo> returnList = new ArrayList<>();
        for (IValidator validator : handlerList) {
            returnList.add(new ValueTextVo(validator.getClassName(), validator.getName()));
        }
        return returnList;
    }

}
