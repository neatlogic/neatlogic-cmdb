package codedriver.module.cmdb.api.validator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.dao.mapper.validator.ValidatorMapper;
import codedriver.framework.cmdb.dto.validator.ValidatorVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchValidatorApi extends PrivateApiComponentBase {

    @Autowired
    private ValidatorMapper validatorMapper;

    @Override
    public String getToken() {
        return "/cmdb/validator/search";
    }

    @Override
    public String getName() {
        return "查询属性校验器";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
        @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页"),
        @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数量"),
        @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = ValidatorVo[].class)})
    @Description(desc = "查询属性校验器接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        ValidatorVo validatorVo = JSONObject.toJavaObject(jsonObj, ValidatorVo.class);
        List<ValidatorVo> validatorList = validatorMapper.searchValidator(validatorVo);
        returnObj.put("tbodyList", validatorList);
        if (validatorList.size() > 0 && validatorVo.getNeedPage()) {
            returnObj.put("pageSize", validatorVo.getPageSize());
            returnObj.put("pageCount", validatorVo.getPageCount());
            returnObj.put("rowNum", validatorVo.getRowNum());
            returnObj.put("currentPage", validatorVo.getCurrentPage());
        }
        return returnObj;
    }

}
