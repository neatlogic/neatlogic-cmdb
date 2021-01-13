package codedriver.module.cmdb.api.rel;

import codedriver.framework.cmdb.constvalue.RelRuleType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Deprecated
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListRelRuleTypeApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/relruletype/list";
    }

    @Override
    public String getName() {
        return "获取关系规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "获取关系规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ValueTextVo> valueList = new ArrayList<>();
        for (RelRuleType type : RelRuleType.values()) {
            valueList.add(new ValueTextVo(type.getValue(), type.getText()));
        }
        return valueList;
    }
}
