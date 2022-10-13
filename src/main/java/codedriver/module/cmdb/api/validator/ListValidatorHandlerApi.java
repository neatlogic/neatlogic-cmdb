/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.validator;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.validator.ValidatorVo;
import codedriver.framework.cmdb.validator.core.IValidator;
import codedriver.framework.cmdb.validator.core.ValidatorFactory;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
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
