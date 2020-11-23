package codedriver.module.cmdb.api.attr;

import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetShowCiAttrListApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/showattr/list";
    }

    @Override
    public String getName() {
        return "获取配置项详情页显示的字段";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = AttrVo[].class)})
    @Description(desc = "获取配置项详情页显示的字段")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        List<String> showTypes = new ArrayList<>();
        showTypes.add(ShowType.DETAIL.getValue());
        showTypes.add(ShowType.ALL.getValue());
        List<AttrVo> attrList= attrMapper.getAttrByCiIdAndShowType(ciId,showTypes);
        return attrList;
    }
}
